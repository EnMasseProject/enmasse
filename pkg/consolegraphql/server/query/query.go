/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package query

import (
	"context"
	"github.com/99designs/gqlgen/graphql"
	"github.com/99designs/gqlgen/graphql/handler"
	"github.com/99designs/gqlgen/graphql/handler/extension"
	"github.com/99designs/gqlgen/graphql/handler/transport"
	"github.com/99designs/gqlgen/graphql/playground"
	"github.com/alexedwards/scs/v2"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/resolvers"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/vektah/gqlparser/v2/ast"
	"github.com/vektah/gqlparser/v2/gqlerror"
	"k8s.io/client-go/rest"
	"log"
	"net/http"
	"time"
)

func CreateQueryServer(resolver resolvers.Resolver, playMode bool, devMode bool, sessionManager *scs.SessionManager, errorCountMetric *prometheus.CounterVec, timeMetric *prometheus.HistogramVec, config *rest.Config, impersonationConfig *ImpersonationConfig, setsCreator clientSetsCreator) *http.ServeMux {
	queryServer := http.NewServeMux()

	gqlServer := handler.New(resolvers.NewExecutableSchema(resolvers.Config{Resolvers: &resolver}))

	queryEndpoint := "/graphql/query"
	if playMode || devMode {
		playgroundHandler := playground.Handler("GraphQL playground", queryEndpoint)
		queryServer.Handle("/graphql/", playgroundHandler)
		gqlServer.Use(extension.Introspection{})
	}

	gqlServer.AddTransport(transport.POST{})

	gqlServer.SetErrorPresenter(func(ctx context.Context, err error) *gqlerror.Error {
		rctx := graphql.GetOperationContext(ctx)
		if rctx != nil {
			log.Printf("Query error - op: %s query: %s vars: %+v error: %s\n", rctx.OperationName, rctx.RawQuery, rctx.Variables, err)
		}
		errorCountMetric.WithLabelValues(rctx.OperationName).Inc()
		return graphql.DefaultErrorPresenter(ctx, err)
	})

	gqlServer.AroundOperations(func(ctx context.Context, next graphql.OperationHandler) graphql.ResponseHandler {
		octx := graphql.GetOperationContext(ctx)
		requestState := server.GetRequestStateFromContext(ctx)
		if requestState == nil {
			panic("missing request state")
		}

		// CVE-2020-14319 defence
		if octx.Operation.Operation == ast.Mutation && requestState.NewSession {
			return func(ctx context.Context) *graphql.Response {
				log.Printf("[%s] Query error - op: %s query: %s vars: %+v", requestState.User.Name, octx.Operation.Name, octx.RawQuery, octx.Variables)
				return graphql.ErrorResponse(ctx, "unable to invoke mutation %s at this time", octx.Operation.Name)
			}
		}

		return next(ctx)
	})

	gqlServer.AroundResponses(func(ctx context.Context, next graphql.ResponseHandler) *graphql.Response {
		loggedOnUser := "<unknown>"
		start := time.Now()
		rctx := graphql.GetOperationContext(ctx)

		result := next(ctx)
		loggedOnUser = UpdateAccessControllerState(ctx, loggedOnUser, sessionManager)
		if rctx != nil {
			since := time.Since(start)
			log.Printf("[%s] Query execution - op: %s %s\n", loggedOnUser, rctx.OperationName, since)
			timeMetric.WithLabelValues(rctx.OperationName).Observe(since.Seconds())
		}
		return result
	})

	if devMode {
		queryServer.Handle(queryEndpoint, DevelopmentHandler(gqlServer, sessionManager, config.BearerToken, setsCreator))
	} else {
		queryServer.Handle(queryEndpoint, AuthHandler(gqlServer, sessionManager, impersonationConfig, setsCreator))
	}
	return queryServer
}
