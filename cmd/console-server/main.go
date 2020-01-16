/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"context"
	"fmt"
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
	"net/http/httputil"
	"os"
	"reflect"
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

type Tracer struct {
	http.RoundTripper
	name string
}

// RoundTrip calls the nested RoundTripper while printing each request and
// response/error to os.Stderr on either side of the nested call.  WARNING: this
// may output sensitive information including bearer tokens.
func (t *Tracer) RoundTrip(req *http.Request) (*http.Response, error) {
	// Dump the request to os.Stderr.
	b, err := httputil.DumpRequestOut(req, true)
	if err != nil {
		return nil, err
	}
	log.Printf("%s Tracing rep %+v", t.name, req)
	os.Stderr.Write(b)
	os.Stderr.Write([]byte{'\n'})

	// Call the nested RoundTripper.
	resp, err := t.RoundTripper.RoundTrip(req)

	// If an error was returned, dump it to os.Stderr.
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		return resp, err
	}

	// Dump the response to os.Stderr.
	b, err = httputil.DumpResponse(resp, req.URL.Query().Get("watch") != "true")
	if err != nil {
		return nil, err
	}
	os.Stderr.Write(b)
	os.Stderr.Write([]byte{'\n'})

	return resp, err
}

func authHandler(next http.Handler) http.Handler {
	return http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		accessToken := req.Header.Get("X-Forwarded-Access-Token")
		log.Printf("KWDEBUG Bearer token : " + accessToken)

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
		log.Printf("KWDEBUG Using kubeconfig: %+v", kubeconfig)

		config, err :=  kubeconfig.ClientConfig()

		if err != nil {
			log.Printf("Failed to build config : %v", err)
			rw.WriteHeader(500)
			return
		}

		log.Printf("Wrapping transport")
		config.WrapTransport = func(rt http.RoundTripper) http.RoundTripper {
			return &Tracer{rt, "auth"}
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
	_, disablePropagateBearer := os.LookupEnv("DISABLE_PROPAGATE_BEARER")
	dumpCachePeriod, _ := os.LookupEnv("DUMP_CACHE_PERIOD")

	log.Printf("Namespace: %s, Port: %s, Disable Propagate Bearer: %t\n", infraNamespace, port, disablePropagateBearer)

	objectCache, err := createObjectCache()
	if err != nil {
		panic(err)
	}


	kubeconfig := clientcmd.NewNonInteractiveDeferredLoadingClientConfig(
		clientcmd.NewDefaultClientConfigLoadingRules(),
		&clientcmd.ConfigOverrides{},
	)

	config, err :=  kubeconfig.ClientConfig()
	//config.WrapTransport = func(rt http.RoundTripper) http.RoundTripper {
	//	return &Tracer{rt, "core"}
	//}

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
			return watchers.NewConnectionAndLinkWatcher(objectCache, infraNamespace, func() agent.AgentCollector {
				return agent.AmqpAgentCollectorCreator(config.BearerToken)
			},  watchers.ConnectionAndLinkWatcherConfig(config))
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
				//objectCache.Dump()
				//_ = metricCache.Dump()
			}, duration)

		}
	}

	playground := handler.Playground("GraphQL playground", "/graphql/query")
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

	http.Handle("/graphql/", playground)
	if disablePropagateBearer {
		http.Handle("/graphql/query", graphql)
	} else {
		http.Handle("/graphql/query", authHandler(graphql))
	}

	log.Printf("connect to http://localhost:%s/ for GraphQL playground\n", port)
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
					"Namespace":        		watchers.NamespaceIndexCreator,
					"AddressSpace":     		watchers.AddressSpaceIndexCreator,
					"Address":          		watchers.AddressIndexCreator,
					"AddressPlan":      		watchers.AddressPlanIndexCreator,
					"AddressSpacePlan": 		watchers.AddressSpacePlanIndexCreator,
					"AuthenticationService":	watchers.AuthenticationServiceIndexCreator,
					"AddressSpaceSchema":    	watchers.AddressSpaceSchemaIndexCreator,
					"Connection":       		watchers.ConnectionIndexCreator,
					"Link":             		watchers.ConnectionLinkIndexCreator,
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
