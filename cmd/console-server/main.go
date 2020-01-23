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
	"github.com/alexedwards/scs/v2"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	adminv1beta2 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta2"
	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/accesscontroller"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/agent"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/metric"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/resolvers"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/enmasseproject/enmasse/pkg/util"
	user "github.com/openshift/client-go/user/clientset/versioned/typed/user/v1"
	"github.com/vektah/gqlparser/gqlerror"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/tools/clientcmd"
	"k8s.io/client-go/tools/clientcmd/api"
	"log"
	"net/http"
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
4) Pass CA to the Go-AMQP client when connecting to agents.

*/

const accessControllerStateCookieName = "accessControllerState"

func authHandler(next http.Handler, sessionManager *scs.SessionManager) http.Handler {

	return http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		var state *server.RequestState

		accessToken := req.Header.Get("X-Forwarded-Access-Token")

		if accessToken == "" {
			rw.WriteHeader(401)
			return
		}

		kubeConfig := clientcmd.NewNonInteractiveDeferredLoadingClientConfig(
			clientcmd.NewDefaultClientConfigLoadingRules(),
			&clientcmd.ConfigOverrides{
				AuthInfo: api.AuthInfo{
					Token: accessToken,
				},
			},
		)

		config, err := kubeConfig.ClientConfig()

		//config.WrapTransport = func(rt http.RoundTripper) http.RoundTripper {
		//	return &server.Tracer{RoundTripper: rt}
		//}

		kubeClient, err := kubernetes.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		if err != nil {
			log.Printf("Failed to build config : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		userClient, err := user.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		coreClient, err := enmassev1beta1.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		accessControllerState := sessionManager.Get(req.Context(), accessControllerStateCookieName)

		controller := accesscontroller.NewKubernetesRBACAccessController(kubeClient, accessControllerState)

		state = &server.RequestState{
			UserInterface:        userClient.Users(),
			EnmasseV1beta1Client: coreClient,
			AccessController:     controller,
		}

		ctx := server.ContextWithRequestState(state, req.Context())

		next.ServeHTTP(rw, req.WithContext(ctx))
	})
}

func developmentHandler(next http.Handler, sessionManager *scs.SessionManager) http.Handler {
	return http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {

		kubeconfig := clientcmd.NewNonInteractiveDeferredLoadingClientConfig(
			clientcmd.NewDefaultClientConfigLoadingRules(),
			&clientcmd.ConfigOverrides{},
		)

		config, err := kubeconfig.ClientConfig()

		if err != nil {
			log.Printf("Failed to build config : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		userclientset, err := user.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		coreClient, err := enmassev1beta1.NewForConfig(config)
		if err != nil {
			log.Printf("Failed to build client set : %v", err)
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}

		requestState := &server.RequestState{
			UserInterface:        userclientset.Users(),
			EnmasseV1beta1Client: coreClient,
			AccessController:     accesscontroller.NewAllowAllAccessController()}

		ctx := server.ContextWithRequestState(requestState, req.Context())
		next.ServeHTTP(rw, req.WithContext(ctx))
	})
}

func main() {
	infraNamespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")
	port := util.GetEnvOrDefault("PORT", "8080")

	var developmentMode = flag.Bool("developmentMode", false,
		"set to true to run console-server outside of the OpenShift container.  It will look for"+
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

	dumpCachePeriod := util.GetEnvOrDefault("DUMP_CACHE_PERIOD", "0s")
	updateMetricsPeriod := util.GetEnvOrDefault("UPDATE_METRICS_PERIOD", "5s")

	log.Printf("Namespace: %s\n", infraNamespace)

	objectCache, err := cache.CreateObjectCache()
	if err != nil {
		panic(err)
	}

	kubeconfig := clientcmd.NewNonInteractiveDeferredLoadingClientConfig(
		clientcmd.NewDefaultClientConfigLoadingRules(),
		&clientcmd.ConfigOverrides{},
	)

	config, err := kubeconfig.ClientConfig()
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

	creators := []func() (watchers.ResourceWatcher, error){
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

	resourcewatchers := make([]watchers.ResourceWatcher, 0)

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

	dumpCache, err := time.ParseDuration(dumpCachePeriod)
	if err == nil && dumpCache.Nanoseconds() > 0 {
		schedule(func() {
			_ = objectCache.Dump()
		}, dumpCache)

	}

	updateMetrics, err := time.ParseDuration(updateMetricsPeriod)
	if err == nil && updateMetrics.Nanoseconds() > 0 {
		schedule(func() {
			err, updated := metric.UpdateAllMetrics(objectCache)
			if err != nil {
				log.Printf("failed to update metrics, %s", err)
			} else if updated > 0 {
				log.Printf("%d object metric(s) updated", updated)

			}
		}, updateMetrics)

	}

	queryEndpoint := "/graphql/query"
	playground := handler.Playground("GraphQL playground", queryEndpoint)
	http.Handle("/graphql/", playground)

	sessionManager := scs.New()
	sessionManager.Lifetime = 30 * time.Minute
	sessionManager.IdleTimeout = 5 * time.Minute
	sessionManager.Cookie.HttpOnly = true
	sessionManager.Cookie.Persist = false
	sessionManager.Cookie.Secure = true

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

			requestState := server.GetRequestStateFromContext(ctx)
			if requestState != nil {
				if updated, accessControllerState := requestState.AccessController.GetState(); updated {
					if accessControllerState == nil {
						sessionManager.Remove(ctx, accessControllerStateCookieName)
					} else {
						sessionManager.Put(ctx, accessControllerStateCookieName, accessControllerState)
					}
				}
			}
			return bytes
		}),
	)

	if *developmentMode {
		http.Handle(queryEndpoint, developmentHandler(graphql, sessionManager))
	} else {
		http.Handle(queryEndpoint, authHandler(graphql, sessionManager))
	}

	err = http.ListenAndServe(":"+port, sessionManager.LoadAndSave(http.DefaultServeMux))
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
