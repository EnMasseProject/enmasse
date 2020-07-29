/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"flag"
	"fmt"
	adminv1beta2 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta2"
	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/metric"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/resolvers"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server/query"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"k8s.io/client-go/tools/clientcmd"
	"log"
	"net/http"
	"os"
	"time"
)

/*
console-server - presents a GraphQL API allowing the client to query details of address spaces, addresses,
connections and links.

For development purposes, you can run the console backend outside the container.  See the Makefile target 'run'.
*/

type logWriter struct {
}

func (writer logWriter) Write(bytes []byte) (int, error) {
	return fmt.Print(time.Now().UTC().Format("2006-01-02T15:04:05.999Z") + " " + string(bytes))
}

func main() {
	log.SetFlags(0)
	log.SetOutput(new(logWriter))

	infraNamespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")
	port := util.GetEnvOrDefault("PORT", "8080")
	metricsPort := util.GetEnvOrDefault("METRICS_PORT", "8889")

	var impersonationConfig *query.ImpersonationConfig
	impersonationEnable := util.GetBooleanEnvOrDefault("IMPERSONATION_ENABLE", false)
	if impersonationEnable {
		impersonationConfig = &query.ImpersonationConfig{}
		userHeader, ok := os.LookupEnv("IMPERSONATION_USER_HEADER")
		if ok {
			impersonationConfig.UserHeader = &userHeader
		}
	}

	var developmentMode = flag.Bool("developmentMode", false,
		"set to true to run console-server outside of the OpenShift container.  It will look for"+
			"manually established routes to agents rather than services.")
	flag.Parse()

	log.Printf("console-server starting\n")

	if *developmentMode {
		log.Printf("Running in DEVELOPMENT MODE.  This mode is not intended for use within the container.\n")
		log.Printf(`Expose addressspace agents to the console server by *manually* running:

oc create route passthrough  --service=agent-<infrauuid>

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

	queryTimeMetric, queryErrorCountMetric, sessionCountMetric := server.CreateMetrics()
	prometheus.MustRegister(queryTimeMetric, queryErrorCountMetric, sessionCountMetric)

	sessionManager := server.CreateSessionManager(sessionLifetime, sessionIdleTimeout,
		func() { manager.BeginWatching() },
		func() { manager.EndWatching() },
		sessionCountMetric)

	queryServer := query.CreateQueryServer(resolver, graphqlPlayground, *developmentMode,
		sessionManager,
		queryErrorCountMetric, queryTimeMetric, config, impersonationConfig, query.CreateClientSets)

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

