/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"fmt"
	"github.com/99designs/gqlgen/cmd"
	"github.com/99designs/gqlgen/handler"
	adminv1beta2 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta2"
	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/agent"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/resolvers"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/enmasseproject/enmasse/pkg/util"
	user "github.com/openshift/client-go/user/clientset/versioned/typed/user/v1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"log"
	"net/http"
	"os"
	config2 "sigs.k8s.io/controller-runtime/pkg/client/config"
	"time"
)

/*

For development purposes, you can run the console backend outside Kubernetes:

export DISABLE_PROPAGATE_BEARER=1
export ENMASSE_ADDRESS_SPACE_TYPE_FILE=.../console//console-server/src/main/resources/addressSpaceTypes.json
ENMASSE_ADDRESS_TYPE_FILE=.../console//console-server/src/main/resources/addressTypes.json
go run cmd/console-server/main.go

You can then point a browser at :

http://localhost:8080/graphql

You should then be able to query all objects except for connections, links and metrics.

To test the integration with a single Agent locally, use an oc port forward to expose
the AMQP port of the admin pod.

oc port-forward deployment/admin.4f5fcdf 56710:56710

then set:

export AGENT_HOST=localhost:56710

Now restart cmd/console-server/main.go.   Connections for Standard Address Spaces will now be
available through GraphQL.

Major items that are unfinished:

6) Restrict query results to what the user may see
8) Mutations
9) Auth services.
12) Refactor agent -> model conversion - currently ugly code and poor division of responsibilities
13) Refactor cachedb index specification to avoid spreading of knowledge of the indices throughout the code


*/
func authHandler(next http.Handler) http.Handler {
	return http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		accessToken := req.Header.Get("X-Forwarded-Access-Token")
		if accessToken == "" {
			rw.WriteHeader(401)
			return
		}

		config, err := config2.GetConfig()
		if err != nil {
			log.Printf("Failed to build config : %v", err)
			rw.WriteHeader(500)
			return
		}
		config.BearerToken = accessToken

		userclientset, err := user.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			rw.WriteHeader(500)
			return
		}

		requestState := &server.RequestState{
			UserInterface: userclientset.Users(),
		}

		ctx := server.ContextWithRequestState(requestState, req.Context())
		next.ServeHTTP(rw, req.WithContext(ctx))
	})
}

func main() {

	infraNamespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")
	port := util.GetEnvOrDefault("PORT", "8080")
	_, disablePropagateBearer := os.LookupEnv("DISABLE_PROPAGATE_BEARER")
	dumpCachePeriod, _ := os.LookupEnv("DUMP_CACHE_PERIOD")

	log.Printf("Namespace: %s, Port: %s, Disable Propagate Bearer: %t\n", infraNamespace, port, disablePropagateBearer)

	objectCache, err := createObjectCache()
	if err != nil {
		panic(err)
	}

	metricCache, err := createMetricCache()
	if err != nil {
		panic(err)
	}

	config, err := config2.GetConfig()
	if err != nil {
		panic(err)
	}

	coreclient, err := enmassev1beta1.NewForConfig(config)
	if err != nil {
		panic(err.Error())
	}

	adminclient, err := adminv1beta2.NewForConfig(config)
	if err != nil {
		panic(err.Error())
	}

	resourcewatchers := make([]watchers.ResourceWatcher, 0)
	resourcewatchers = append(resourcewatchers, &watchers.AddressSpaceWatcher{
		Namespace: v1.NamespaceAll,
	})
	resourcewatchers = append(resourcewatchers, &watchers.AddressWatcher{
		Namespace: v1.NamespaceAll,
	})
	resourcewatchers = append(resourcewatchers, &watchers.AddressPlanWatcher{
		Namespace: infraNamespace,
	})
	resourcewatchers = append(resourcewatchers, &watchers.AddressSpacePlanWatcher{
		Namespace: infraNamespace,
	})
	resourcewatchers = append(resourcewatchers, &watchers.NamespaceWatcher{})
	connAndLinkWatcher := &watchers.ConnectionAndLinkWatcher{
		Namespace:   infraNamespace,
		MetricCache: metricCache,
		AgentCollectorCreator: func() agent.AgentCollector {
			return agent.AmqpAgentCollectorCreator(config.BearerToken)
		},
	}
	resourcewatchers = append(resourcewatchers, connAndLinkWatcher)

	for _, resourcewatcher := range resourcewatchers {
		client, err := resourcewatcher.NewClientForConfig(config)
		if err != nil {
			panic(err.Error())
		}
		err = resourcewatcher.Init(objectCache, client)
		if err != nil {
			panic(err.Error())
		}
		err = resourcewatcher.Watch()
		if err != nil {
			panic(err.Error())
		}
	}

	resolver := resolvers.Resolver{
		AdminConfig: adminclient,
		CoreConfig:  coreclient,
		Cache:       objectCache,
		MetricCache: metricCache,
	}

	if dumpCachePeriod != "" {
		duration, err := time.ParseDuration(dumpCachePeriod)
		if err == nil {
			schedule(func() {
				//_ = objectCache.Dump()
				//objectCache.Dump()
				//_ = metricCache.Dump()
			}, duration)

		}
	}

	playground := handler.Playground("GraphQL playground", "/graphql/query")
	graphql := handler.GraphQL(resolvers.NewExecutableSchema(resolvers.Config{Resolvers: &resolver}))

	http.Handle("/graphql/", playground)
	if disablePropagateBearer {
		http.Handle("/graphql/query", graphql)
	} else {
		http.Handle("/graphql/query", authHandler(graphql))
	}

	fmt.Printf("connect to http://localhost:%s/ for GraphQL playground\n", port)
	err = http.ListenAndServe(":"+port, nil)
	if err != nil {
		panic(err.Error())
	}

	cmd.Execute()
}

func createObjectCache() (*cache.MemdbCache, error) {
	c := &cache.MemdbCache{}
	err := c.Init(
		cache.IndexSpecifier{
			Name:    "id",
			Indexer: &cache.UidIndex{},
		},
		cache.IndexSpecifier{
			Name: "hierarchy",
			Indexer: &cache.HierarchyIndex{
				IndexCreators: map[string]cache.HierarchicalIndexCreator{
					"Namespace":        watchers.NamespaceIndexCreator,
					"AddressSpace":     watchers.AddressSpaceIndexCreator,
					"Address":          watchers.AddressIndexCreator,
					"AddressPlan":      watchers.AddressPlanIndexCreator,
					"AddressSpacePlan": watchers.AddressSpacePlanIndexCreator,
					"Connection":       watchers.ConnectionIndexCreator,
					"Link":             watchers.ConnectionLinkIndexCreator,
				},
			},
		},
		cache.IndexSpecifier{
			Name:         "addressLinkHierarchy",
			AllowMissing: true,
			Indexer: &cache.HierarchyIndex{
				IndexCreators: map[string]cache.HierarchicalIndexCreator{
					"Link": watchers.AddressLinkIndexCreator,
				},
			},
		})
	return c, err
}

func createMetricCache() (*cache.MemdbCache, error) {
	c := &cache.MemdbCache{}
	err := c.Init(
		cache.IndexSpecifier{
			Name:    "id",
			Indexer: cache.MetricIndex(),
		},
		cache.IndexSpecifier{
			Name:    "connectionLink",
			Indexer: cache.ConnectionLinkMetricIndex(),
			AllowMissing: true,
		},
	)
	return c, err
}

func schedule(f func(), delay time.Duration) (chan bool, chan bool) {
	stop := make(chan bool)
	bump := make(chan bool)

	go func() {
		for {
			f()
			select {
			case <-time.After(delay):
			case <-bump:
			case <-stop:
				return
			}
		}
	}()

	return stop, bump
}
