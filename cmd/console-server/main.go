/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"context"
	"flag"
	"github.com/99designs/gqlgen/cmd"
	"github.com/99designs/gqlgen/graphql"
	"github.com/99designs/gqlgen/handler"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	adminv1beta2 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta2"
	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/agent"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/resolvers"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/enmasseproject/enmasse/pkg/util"
	user "github.com/openshift/client-go/user/clientset/versioned/typed/user/v1"
	"github.com/vektah/gqlparser/gqlerror"
	"k8s.io/client-go/tools/clientcmd"
	"k8s.io/client-go/tools/clientcmd/api"
	"log"
	"net/http"
	"os"
	"reflect"
	"time"
)

/*

console-server - presents a GraphQL API allowing the client to query details of address spaces, addresses,
connections and links.

For development purposes, you can run the console backend outside the container.  See the Makefile target 'run'.

TODO:

1) Restrict query results to what the user may see
2) Mutations
3) Refactor cachedb index specification to avoid spreading of knowledge of the indices throughout the code
4) Pass CA to the Go-AMQP client when connecting to agents.

*/

func authHandler(next http.Handler) http.Handler {
	return http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		accessToken := req.Header.Get("X-Forwarded-Access-Token")

		if accessToken == "" {
			rw.WriteHeader(401)
			return
		}

		kubeconfig := clientcmd.NewNonInteractiveDeferredLoadingClientConfig(
			clientcmd.NewDefaultClientConfigLoadingRules(),
			&clientcmd.ConfigOverrides{
				AuthInfo: api.AuthInfo{
					Token: accessToken,
				},
			},
		)

		config, err :=  kubeconfig.ClientConfig()

		if err != nil {
			log.Printf("Failed to build config : %v", err)
			rw.WriteHeader(500)
			return
		}

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

func developmentHandler(next http.Handler) http.Handler {
	return http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {

		kubeconfig := clientcmd.NewNonInteractiveDeferredLoadingClientConfig(
			clientcmd.NewDefaultClientConfigLoadingRules(),
			&clientcmd.ConfigOverrides{},
		)

		config, err :=  kubeconfig.ClientConfig()

		if err != nil {
			log.Printf("Failed to build config : %v", err)
			rw.WriteHeader(500)
			return
		}

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

	var developmentMode = flag.Bool("developmentMode", false,
		"set to true to run console-server outside of the OpenShift container.  It will look for" +
		"manually established routes to agents rather than services.")
	flag.Parse()

	log.Printf("console-server starting\n")

	if *developmentMode {
		log.Printf("Running in DEVELOPMENT MODE.  This mode is not intended for use within the container.\n")
		log.Printf(`Expose addressspace agents to the console server by *manually* running:

oc create route passthrough  --service=console-4f5fcdf

The GraphQL playground is available:
http://localhost:` + port + `/graphql
			`)
	}

	dumpCachePeriod, _ := os.LookupEnv("DUMP_CACHE_PERIOD")

	log.Printf("Namespace: %s\n", infraNamespace)

	objectCache, err := cache.CreateObjectCache()
	if err != nil {
		panic(err)
	}


	kubeconfig := clientcmd.NewNonInteractiveDeferredLoadingClientConfig(
		clientcmd.NewDefaultClientConfigLoadingRules(),
		&clientcmd.ConfigOverrides{},
	)

	config, err :=  kubeconfig.ClientConfig()
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

	creators := []func() (watchers.ResourceWatcher, error) {
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAddressSpaceWatcher(objectCache, watchers.AddressSpaceWatcherConfig(config),
				watchers.AddressSpaceWatcherFactory(
					func(space *v1beta1.AddressSpace) interface{} {
						return &consolegraphql.AddressSpaceHolder{
							AddressSpace: *space,
						}
					},
					func(value *v1beta1.AddressSpace, existing interface{}) bool {
						ash := existing.(*consolegraphql.AddressSpaceHolder)
						if reflect.DeepEqual(ash.AddressSpace, *value) {
							return false
						} else {
							value.DeepCopyInto(&ash.AddressSpace)
							return true
						}
					},
				))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAddressWatcher(objectCache, watchers.AddressWatcherConfig(config),
				watchers.AddressWatcherFactory(
					func(space *v1beta1.Address) interface{} {
						return &consolegraphql.AddressHolder{
							Address: *space,
						}
					},
					func(value *v1beta1.Address, existing interface{}) bool {
						ash := existing.(*consolegraphql.AddressHolder)
						if reflect.DeepEqual(ash.Address, *value) {
							return false
						} else {
							value.DeepCopyInto(&ash.Address)
							return true
						}
					},
				))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewNamespaceWatcher(objectCache, watchers.NamespaceWatcherConfig(config))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAddressSpacePlanWatcher(objectCache, infraNamespace, watchers.AddressSpacePlanWatcherConfig(config))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAddressPlanWatcher(objectCache, infraNamespace, watchers.AddressPlanWatcherConfig(config))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAuthenticationServiceWatcher(objectCache, infraNamespace, watchers.AuthenticationServiceWatcherConfig(config))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAddressSpaceSchemaWatcher(objectCache, watchers.AddressSpaceSchemaWatcherConfig(config))
		},
		func() (watchers.ResourceWatcher, error) {
			watcherConfigs := make([]watchers.WatcherOption, 0)
			watcherConfigs = append(watcherConfigs, watchers.AgentWatcherServiceConfig(config))
			if *developmentMode {
				watcherConfigs = append(watcherConfigs, watchers.AgentWatcherRouteConfig(config))
			}
			return watchers.NewAgentWatcher(objectCache, infraNamespace, func() agent.AgentCollector {
				return agent.AmqpAgentCollectorCreator(config.BearerToken)
			}, *developmentMode, watcherConfigs...)
		},
	}

	resourcewatchers := make([]watchers.ResourceWatcher,0)

	for _, f := range creators {
		watcher, err := f()
		if err != nil {
			panic(err.Error())
		}
		resourcewatchers = append(resourcewatchers, watcher)

		err = watcher.Watch()
		if err != nil {
			panic(err.Error())
		}
	}

	resolver := resolvers.Resolver{
		AdminConfig: adminclient,
		CoreConfig:  coreclient,
		Cache:       objectCache,
	}

	if dumpCachePeriod != "" {
		duration, err := time.ParseDuration(dumpCachePeriod)
		if err == nil {
			schedule(func() {
				_ = objectCache.Dump()
			}, duration)

		}
	}

	queryEndpoint := "/graphql/query"
	playground := handler.Playground("GraphQL playground", queryEndpoint)
	http.Handle("/graphql/", playground)

	graphql := handler.GraphQL(resolvers.NewExecutableSchema(resolvers.Config{Resolvers: &resolver}),
		handler.ErrorPresenter(
			func(ctx context.Context, e error) *gqlerror.Error {
				rctx := graphql.GetRequestContext(ctx)
				if rctx != nil {
					log.Printf("Query error - op: %s query: %s vars: %+v error: %s\n", rctx.OperationName, rctx.RawQuery, rctx.Variables, e)
				}
				return graphql.DefaultErrorPresenter(ctx, e)
			},
		),
		handler.RequestMiddleware(func(ctx context.Context, next func(ctx context.Context) []byte) []byte {
			rctx := graphql.GetRequestContext(ctx)
			start := time.Now()
			bytes := next(ctx)
			if rctx != nil {
				log.Printf("Query execution - op: %s %s\n", rctx.OperationName, time.Since(start))
			}
			return bytes
		}),
	)

	if *developmentMode {
		http.Handle(queryEndpoint, developmentHandler(graphql))
	} else {
		http.Handle(queryEndpoint, authHandler(graphql))
	}

	err = http.ListenAndServe(":"+port, nil)
	if err != nil {
		panic(err.Error())
	}

	cmd.Execute()
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
