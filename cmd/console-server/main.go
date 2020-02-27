/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"context"
	"flag"
	"github.com/99designs/gqlgen/graphql"
	"github.com/99designs/gqlgen/handler"
	adminv1beta2 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta2"
	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/metric"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/resolvers"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/vektah/gqlparser/gqlerror"
	"k8s.io/client-go/tools/clientcmd"
	"log"
	"net/http"
	"time"
)

/*
console-server - presents a GraphQL API allowing the client to query details of address spaces, addresses,
connections and links.

For development purposes, you can run the console backend outside the container.  See the Makefile target 'run'.
*/

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

	manager := watchers.New(objectCache, resyncInterval, config, infraNamespace, developmentMode, agentCommandDelegateExpiryPeriod, agentAmqpConnectTimeout, agentAmqpMaxFrameSize)
	manager.Start()

	resolver := resolvers.Resolver{
		AdminConfig:  adminclient,
		CoreConfig:   coreclient,
		Cache:        objectCache,
		GetCollector: manager.GetCollector,
	}

	if dumpCachePeriod > 0 {
		server.Schedule(func() {
			_ = objectCache.Dump()
		}, dumpCachePeriod)
	}

	if updateMetricsPeriod.Nanoseconds() > 0 {
		server.Schedule(func() {
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
	sessionCountMetric := prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "console_active_session_count",
		Help: "Number of active HTTP sessions",
	})
	prometheus.MustRegister(queryTimeMetric, queryErrorCountMetric, sessionCountMetric)

	queryServer := http.NewServeMux()

	queryEndpoint := "/graphql/query"
	if graphqlPlayground || *developmentMode {
		playground := handler.Playground("GraphQL playground", queryEndpoint)
		queryServer.Handle("/graphql/", playground)
	}

	sessionManager := server.CreateSessionManager(sessionLifetime, sessionIdleTimeout,
		func() { manager.BeginWatching() },
		func() { manager.EndWatching() },
		sessionCountMetric)

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

			loggedOnUser = server.UpdateAccessControllerState(ctx, loggedOnUser, sessionManager)

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
		queryServer.Handle(queryEndpoint, server.DevelopmentHandler(gql, sessionManager, config.BearerToken))
	} else {
		queryServer.Handle(queryEndpoint, server.AuthHandler(gql, sessionManager))
	}

	go func() {
		err = http.ListenAndServe("127.0.0.1:"+port, sessionManager.LoadAndSave(queryServer))
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
