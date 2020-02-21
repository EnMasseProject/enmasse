/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"bytes"
	"context"
	"crypto/sha256"
	"crypto/tls"
	"flag"
	"github.com/99designs/gqlgen/graphql"
	"github.com/99designs/gqlgen/handler"
	"github.com/alexedwards/scs/v2"
	adminv1beta2 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta2"
	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/accesscontroller"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/agent"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/metric"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/resolvers"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/enmasseproject/enmasse/pkg/util"
	user "github.com/openshift/client-go/user/clientset/versioned/typed/user/v1"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/vektah/gqlparser/gqlerror"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/tools/clientcmd"
	"k8s.io/client-go/tools/clientcmd/api"
	"log"
	"net/http"
	"time"
)

/*
console-server - presents a GraphQL API allowing the client to query details of address spaces, addresses,
connections and links.

For development purposes, you can run the console backend outside the container.  See the Makefile target 'run'.
*/

const accessControllerStateCookieName = "accessControllerState"
const sessionOwnerSessionAttribute = "sessionOwnerSessionAttribute"
const loggedOnUserSessionAttribute = "loggedOnUserSessionAttribute"

func authHandler(next http.Handler, sessionManager *scs.SessionManager) http.Handler {

	return http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		var state *server.RequestState

		accessToken := req.Header.Get("X-Forwarded-Access-Token")

		if accessToken == "" {
			http.Error(rw, "No access token", http.StatusUnauthorized)
			rw.WriteHeader(401)
			return
		}

		accessTokenSha := getShaSum(accessToken)
		if sessionManager.Exists(req.Context(), sessionOwnerSessionAttribute) {
			sessionOwnerAccessTokenSha := sessionManager.Get(req.Context(), sessionOwnerSessionAttribute).([]byte)
			if !bytes.Equal(sessionOwnerAccessTokenSha, accessTokenSha) {
				// This session must have belonged to a different accessToken, destroy it.
				// New session created automatically.
				_ = sessionManager.Destroy(req.Context())
				sessionManager.Put(req.Context(), sessionOwnerSessionAttribute, accessTokenSha)
			}
		} else {
			sessionManager.Put(req.Context(), sessionOwnerSessionAttribute, accessTokenSha)
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

		loggedOnUser := "<unknown>"
		if sessionManager.Exists(req.Context(), loggedOnUserSessionAttribute) {
			loggedOnUser = sessionManager.GetString(req.Context(), loggedOnUserSessionAttribute)
		} else {
			if util.HasApi(util.UserGVK) {
				usr, err := userClient.Users().Get("~", metav1.GetOptions{})
				if err == nil {
					loggedOnUser = usr.ObjectMeta.Name
					sessionManager.Put(req.Context(), loggedOnUserSessionAttribute, loggedOnUser)
				}
			}
		}

		accessControllerState := sessionManager.Get(req.Context(), accessControllerStateCookieName)

		controller := accesscontroller.NewKubernetesRBACAccessController(kubeClient, accessControllerState)

		state = &server.RequestState{
			UserInterface:        userClient.Users(),
			EnmasseV1beta1Client: coreClient,
			AccessController:     controller,
			User:                 loggedOnUser,
			UserAccessToken:      accessToken,
		}

		ctx := server.ContextWithRequestState(state, req.Context())

		next.ServeHTTP(rw, req.WithContext(ctx))
	})
}

func developmentHandler(next http.Handler, _ *scs.SessionManager, accessToken string) http.Handler {
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
			AccessController:     accesscontroller.NewAllowAllAccessController(),
			User:                 "<unknown>",
			UserAccessToken:      accessToken,
		}

		ctx := server.ContextWithRequestState(requestState, req.Context())
		next.ServeHTTP(rw, req.WithContext(ctx))
	})
}

func main() {
	infraNamespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")
	port := util.GetEnvOrDefault("PORT", "8080")
	metricsPort := util.GetEnvOrDefault("METRICS_PORT", "8889")

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

	graphqlPlayground := util.GetBooleanEnvOrDefault("GRAPHQL_PLAYGROUND", false)
	promQLRateMetricExpression := util.GetEnvOrDefault("PROMQL_RATE_METRIC_EXPRESSION", "round(rate(unused_label[5m]), 0.01)") // Rate per second, rounded to hundredths
	dumpCachePeriod := util.GetDurationEnvOrDefault("DUMP_CACHE_PERIOD", time.Second*0)
	updateMetricsPeriod := util.GetDurationEnvOrDefault("UPDATE_METRICS_PERIOD", time.Second*5)
	agentCommandDelegateExpiryPeriod := util.GetDurationEnvOrDefault("AGENT_COMMAND_DELEGATE_EXPIRY_PERIOD", time.Minute*5)
	agentAmqpConnectTimeout := util.GetDurationEnvOrDefault("AGENT_AMQP_CONNECT_TIMEOUT", time.Second*10)
	agentAmqpMaxFrameSize := uint32(util.GetUintEnvOrDefault("AGENT_AMQP_MAX_FRAME_SIZE", 0, 32, 4294967295)) // Matches Rhea default
	sessionLifetime := util.GetDurationEnvOrDefault("HTTP_SESSION_LIFETIME", 30*time.Minute)
	sessionIdleTimeout := util.GetDurationEnvOrDefault("HTTP_SESSION_IDLE_TIMEOUT", 5*time.Minute)
	resyncInterval := util.GetDurationEnvOrDefault("RESYNC_INTERVAL", 5*time.Minute)

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

	var getCollector func(string) agent.Delegate

	creators := []func() (watchers.ResourceWatcher, error){
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAddressSpaceWatcher(objectCache, &resyncInterval, watchers.AddressSpaceWatcherConfig(config),
				watchers.AddressSpaceWatcherFactory(watchers.AddressSpaceCreate, watchers.AddressSpaceUpdate))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAddressWatcher(objectCache, &resyncInterval, watchers.AddressWatcherConfig(config),
				watchers.AddressWatcherFactory(watchers.AddressCreate, watchers.AddressUpdate))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewNamespaceWatcher(objectCache, &resyncInterval, watchers.NamespaceWatcherConfig(config))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAddressSpacePlanWatcher(objectCache, &resyncInterval, infraNamespace, watchers.AddressSpacePlanWatcherConfig(config))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAddressPlanWatcher(objectCache, &resyncInterval, infraNamespace, watchers.AddressPlanWatcherConfig(config))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAuthenticationServiceWatcher(objectCache, &resyncInterval, infraNamespace, watchers.AuthenticationServiceWatcherConfig(config))
		},
		func() (watchers.ResourceWatcher, error) {
			return watchers.NewAddressSpaceSchemaWatcher(objectCache, &resyncInterval, watchers.AddressSpaceSchemaWatcherConfig(config))
		},
		func() (watchers.ResourceWatcher, error) {
			watcherConfigs := make([]watchers.WatcherOption, 0)
			watcherConfigs = append(watcherConfigs, watchers.AgentWatcherServiceConfig(config))
			if *developmentMode {
				watcherConfigs = append(watcherConfigs, watchers.AgentWatcherRouteConfig(config))
			}
			watcher, err := watchers.NewAgentWatcher(objectCache, &resyncInterval, infraNamespace,
				func(host string, port int32, infraUuid string, addressSpace string, addressSpaceNamespace string, tlsConfig *tls.Config) agent.Delegate {
					return agent.NewAmqpAgentDelegate(config.BearerToken,
						host, port, tlsConfig,
						addressSpaceNamespace, addressSpace, infraUuid,
						agentCommandDelegateExpiryPeriod, agentAmqpConnectTimeout, agentAmqpMaxFrameSize)
				}, *developmentMode, watcherConfigs...)
			getCollector = watcher.Collector
			return watcher, err
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
		AdminConfig:  adminclient,
		CoreConfig:   coreclient,
		Cache:        objectCache,
		GetCollector: getCollector,
	}

	if dumpCachePeriod > 0 {
		schedule(func() {
			_ = objectCache.Dump()
		}, dumpCachePeriod)

	}

	if updateMetricsPeriod.Nanoseconds() > 0 {
		schedule(func() {
			err, updated := metric.UpdateAllMetrics(objectCache, promQLRateMetricExpression)
			if err != nil {
				log.Printf("failed to update metrics, %s", err)
			} else if updated > 0 {
				log.Printf("%d object metric(s) updated", updated)

			}
		}, updateMetricsPeriod)

	}

	queryTimeMetric := prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "console_query_time_seconds",
		Help:    "The query time in seconds",
		Buckets: prometheus.DefBuckets,
	}, []string{"operationName"})
	queryErrorCountMetric := prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "console_query_error_total",
		Help: "Number of queries that have ended in error",
	}, []string{"operationName"})
	prometheus.MustRegister(queryTimeMetric, queryErrorCountMetric)

	queryServer := http.NewServeMux()

	queryEndpoint := "/graphql/query"
	if graphqlPlayground || *developmentMode {
		playground := handler.Playground("GraphQL playground", queryEndpoint)
		queryServer.Handle("/graphql/", playground)
	}

	sessionManager := scs.New()
	sessionManager.Lifetime = sessionLifetime
	sessionManager.IdleTimeout = sessionIdleTimeout
	sessionManager.Cookie.HttpOnly = true
	sessionManager.Cookie.Persist = false
	sessionManager.Cookie.Secure = true

	gql := handler.GraphQL(resolvers.NewExecutableSchema(resolvers.Config{Resolvers: &resolver}),
		handler.ErrorPresenter(
			func(ctx context.Context, e error) *gqlerror.Error {
				rctx := graphql.GetRequestContext(ctx)
				if rctx != nil {
					log.Printf("Query error - op: %s query: %s vars: %+v error: %s\n", rctx.OperationName, rctx.RawQuery, rctx.Variables, e)
				}
				queryErrorCountMetric.WithLabelValues(rctx.OperationName).Inc()
				return graphql.DefaultErrorPresenter(ctx, e)
			},
		),
		handler.RequestMiddleware(func(ctx context.Context, next func(ctx context.Context) []byte) []byte {

			loggedOnUser := "<unknown>"
			start := time.Now()
			result := next(ctx)

			requestState := server.GetRequestStateFromContext(ctx)
			if requestState != nil {
				loggedOnUser = requestState.User
				if updated, accessControllerState := requestState.AccessController.GetState(); updated {
					if accessControllerState == nil {
						sessionManager.Remove(ctx, accessControllerStateCookieName)
					} else {
						sessionManager.Put(ctx, accessControllerStateCookieName, accessControllerState)
					}
				}
			}

			rctx := graphql.GetRequestContext(ctx)
			if rctx != nil {
				since := time.Since(start)
				log.Printf("[%s] Query execution - op: %s %s\n", loggedOnUser, rctx.OperationName, since)
				queryTimeMetric.WithLabelValues(rctx.OperationName).Observe(since.Seconds())
			}
			return result
		}),
	)

	if *developmentMode {
		queryServer.Handle(queryEndpoint, developmentHandler(gql, sessionManager, config.BearerToken))
	} else {
		queryServer.Handle(queryEndpoint, authHandler(gql, sessionManager))
	}

	go func() {
		err = http.ListenAndServe(":"+port, sessionManager.LoadAndSave(queryServer))
		if err != nil {
			panic(err.Error())
		}
	}()

	metricsServer := http.NewServeMux()

	metricsServer.Handle("/metrics", promhttp.Handler())
	err = http.ListenAndServe(":"+metricsPort, metricsServer)
	if err != nil {
		panic(err.Error())
	}

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

func getShaSum(accessToken string) []byte {
	accessTokenSha := sha256.Sum256([]byte(accessToken))
	return accessTokenSha[:]
}
