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

1) Authentication to the Agent
2) Make agent generate connection UUIDs for brokered connections
3) Expose brokered agent as AMQP service
4) OAUTH authentication for agent AMQP service (currently uses anonymous)
5) Sorting/Filtering/Pagination
6) Restrict query results to what the user may see
7) Links and Metrics
8) Mutations
9) Auth services.


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

	namespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")
	port := util.GetEnvOrDefault("PORT", "8080")
	_, disablePropagateBearer := os.LookupEnv("DISABLE_PROPAGATE_BEARER")

	log.Printf("Namespace: %s, Port: %s, Disable Propagate Bearer: %t\n", namespace, port, disablePropagateBearer)

	cache := &cache.MemdbCache{}
	err := cache.Init()
	cache.RegisterIndexCreator("Namespace", watchers.NamespaceIndexCreator)
	cache.RegisterIndexCreator("AddressSpace", watchers.AddressSpaceIndexCreator)
	cache.RegisterIndexCreator("Address", watchers.AddressIndexCreator)
	cache.RegisterIndexCreator("AddressPlan", watchers.AddressPlanIndexCreator)
	cache.RegisterIndexCreator("AddressSpacePlan", watchers.AddressSpacePlanIndexCreator)
	cache.RegisterIndexCreator("Connection", watchers.ConnectionIndexCreator)
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
	resourcewatchers = append(resourcewatchers, &watchers.AddressSpaceWatcher{})
	resourcewatchers = append(resourcewatchers, &watchers.AddressWatcher{})
	resourcewatchers = append(resourcewatchers, &watchers.AddressPlanWatcher{})
	resourcewatchers = append(resourcewatchers, &watchers.AddressSpacePlanWatcher{})
	resourcewatchers = append(resourcewatchers, &watchers.NamespaceWatcher{})
	connAndLinkWatcher := &watchers.ConnectionAndLinkWatcher{
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
		err = resourcewatcher.Init(cache, client)
		if err != nil {
			panic(err.Error())
		}
		err = resourcewatcher.Watch(v1.NamespaceAll)
		if err != nil {
			panic(err.Error())
		}
	}

	resolver := resolvers.Resolver{}
	resolver.AdminConfig = adminclient
	resolver.CoreConfig = coreclient
	resolver.Cache = cache
	//resolver.AddrLinkMap = make(map[string][]*consolegraphql.Link)

	schedule(func() {
		cache.Dump()
	}, 10*time.Second)

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

//func mockAddressMetricData() []*consolegraphql.Metric {
//	var metrics []*consolegraphql.Metric
//	msg := "msg"
//	depth := &consolegraphql.Metric{
//		Name:  "queue-depth",
//		Type:  consolegraphql.MetricTypeGauge,
//		Value: math.Round(rand.Float64() * 100),
//		Units: &msg,
//	}
//	msgs := "msg/s"
//	in := &consolegraphql.Metric{
//		Name:  "in",
//		Type:  consolegraphql.MetricTypeRate,
//		Value: math.Round(rand.Float64() * 25),
//		Units: &msgs,
//	}
//	out := &consolegraphql.Metric{
//		Name:  "out",
//		Type:  consolegraphql.MetricTypeRate,
//		Value: math.Round(rand.Float64() * 25),
//		Units: &msgs,
//	}
//
//	metrics = append(metrics, depth, in, out)
//	return metrics
//}
//
//func mockLinkData(addr string) []*consolegraphql.Link {
//
//	var links []*consolegraphql.Link
//	intn := rand.Intn(12)
//
//	for i := 0; i < intn; i++ {
//		role := consolegraphql.LinkRoleReceiver
//		if rand.Intn(10) > 5 {
//			role = consolegraphql.LinkRoleSender
//		}
//		s := "amqps"
//		link := &consolegraphql.Link{
//			Name: fmt.Sprintf("mylink.%d", i),
//			Connection: &consolegraphql.Connection{
//				Hostname:    "host.local:1234",
//				ContainerID: uuid.New().String(),
//				Protocol:    &s,
//				Properties:  nil,
//			},
//			Address: addr,
//			Role:    role,
//			Metrics: mockLinkMetric(role),
//		}
//		links = append(links, link)
//	}
//
//	return links
//}
//
//func mockLinkMetric(role consolegraphql.LinkRole) []*consolegraphql.Metric {
//	var metrics []*consolegraphql.Metric
//	msgs := "msg/s"
//	s := "in"
//	if role == consolegraphql.LinkRoleReceiver {
//		s = "out"
//	}
//	gress := &consolegraphql.Metric{
//		Name:  s,
//		Type:  consolegraphql.MetricTypeRate,
//		Value: math.Round(rand.Float64() * 5),
//		Units: &msgs,
//	}
//	metrics = append(metrics, gress)
//	return metrics
//
//}

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
