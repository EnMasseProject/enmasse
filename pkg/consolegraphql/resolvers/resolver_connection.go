/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/99designs/gqlgen/graphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type connectionK8sResolver struct{ *Resolver }

func (r *Resolver) Connection_consoleapi_enmasse_io_v1() Connection_consoleapi_enmasse_io_v1Resolver {
	return &connectionK8sResolver{r}
}

func (cr connectionK8sResolver) Links(ctx context.Context, obj *consolegraphql.Connection, first *int, offset *int, filter *string, orderBy *string) (*LinkQueryResultConsoleapiEnmasseIoV1, error) {
	if obj != nil {

		fltrfunc, keyElements, e := BuildFilter(filter, "$.metadata.name")
		if e != nil {
			return nil, e
		}

		orderer, e := BuildOrderer(orderBy)
		if e != nil {
			return nil, e
		}

		links, e := cr.Cache.Get(cache.PrimaryObjectIndex, fmt.Sprintf("Link/%s/%s/%s/%s", obj.ObjectMeta.Namespace, obj.ObjectMeta.Name, keyElements), fltrfunc)
		if e != nil {
			return nil, e
		}

		e = orderer(links)
		if e != nil {
			return nil, e
		}

		lower, upper := CalcLowerUpper(offset, first, len(links))
		paged := links[lower:upper]

		consolelinks := make([]*consolegraphql.Link, 0)
		for _, obj := range paged {
			link := obj.(*consolegraphql.Link)
			consolelinks = append(consolelinks, &consolegraphql.Link{
				ObjectMeta: link.ObjectMeta,
				Spec:       link.Spec,
				Metrics:    link.Metrics,
			})
		}

		return &LinkQueryResultConsoleapiEnmasseIoV1{
			Total: len(links),
			Links: consolelinks,
		}, nil
	}
	return nil, nil
}

type connectionSpecK8sResolver struct{ *Resolver }

func (r *Resolver) ConnectionSpec_consoleapi_enmasse_io_v1() ConnectionSpec_consoleapi_enmasse_io_v1Resolver {
	return &connectionSpecK8sResolver{r}
}

func (cs connectionSpecK8sResolver) MessagingProject(ctx context.Context, obj *consolegraphql.ConnectionSpec) (*consolegraphql.MessagingProjectHolder, error) {
	if obj != nil {
		conrsctx := graphql.GetResolverContext(ctx).Parent.Parent
		con := conrsctx.Result.(**consolegraphql.Connection)

		namespace := (*con).ObjectMeta.Namespace
		objs, e := cs.Cache.Get(cache.PrimaryObjectIndex, fmt.Sprintf("MessagingProject/%s/%s", namespace, "default"), nil)
		if e != nil {
			return nil, e
		}
		if len(objs) == 0 {
			return nil, fmt.Errorf("messaging project in namespace '%s' not found", namespace)
		}

		as := objs[0].(*consolegraphql.MessagingProjectHolder)
		return as, nil
	}
	return nil, nil
}

func (cs connectionSpecK8sResolver) Protocol(ctx context.Context, obj *consolegraphql.ConnectionSpec) (Protocol, error) {
	if obj != nil {
		return Protocol(obj.Protocol), nil
	}
	return ProtocolAmqp, nil
}

func (cs connectionSpecK8sResolver) Principal(ctx context.Context, obj *consolegraphql.ConnectionSpec) (string, error) {
	if obj != nil {
		return obj.Principal, nil
	}
	return "", nil
}

func (cs connectionSpecK8sResolver) Properties(ctx context.Context, obj *consolegraphql.ConnectionSpec) ([]*KeyValue, error) {
	if obj != nil && len(obj.Properties) > 0 {
		kvs := make([]*KeyValue, len(obj.Properties))
		i := 0
		for k, v := range obj.Properties {
			kvs[i] = &KeyValue{
				Key:   k,
				Value: v,
			}
			i++
		}
		return kvs, nil
	}
	return []*KeyValue{}, nil
}

func (r *Resolver) LinkSpec_consoleapi_enmasse_io_v1() LinkSpec_consoleapi_enmasse_io_v1Resolver {
	return &linkSpecK8sResolver{r}
}

func (r *queryResolver) Connections(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*ConnectionQueryResultConsoleapiEnmasseIoV1, error) {
	requestState := server.GetRequestStateFromContext(ctx)
	viewFilter := requestState.AccessController.ViewFilter()

	fltrfunc, keyElements, e := BuildFilter(filter, "$.metadata.namespace", "$.spec.addressSpace", "$.metadata.name")
	if e != nil {
		return nil, e
	}

	orderer, e := BuildOrderer(orderBy)
	if e != nil {
		return nil, e
	}

	objects, e := r.Cache.Get(cache.PrimaryObjectIndex, fmt.Sprintf("Connection/%s", keyElements), cache.And(viewFilter, fltrfunc))
	if e != nil {
		return nil, e
	}

	e = orderer(objects)
	if e != nil {
		return nil, e
	}

	lower, upper := CalcLowerUpper(offset, first, len(objects))
	paged := objects[lower:upper]

	cons := make([]*consolegraphql.Connection, len(paged))
	for i, _ := range paged {
		cons[i] = paged[i].(*consolegraphql.Connection)
	}

	return &ConnectionQueryResultConsoleapiEnmasseIoV1{
		Total:       len(objects),
		Connections: cons,
	}, nil
}

func (r *mutationResolver) CloseConnections(ctx context.Context, input []*v1.ObjectMeta) (*bool, error) {

	t := false

	/*
		requestState := server.GetRequestStateFromContext(ctx)
		viewFilter := requestState.AccessController.ViewFilter()
			connFilter := func(obj interface{}) (bool, bool, error) {
				con, ok := obj.(*consolegraphql.Connection)
				if !ok {
					return false, false, fmt.Errorf("unexpected type: %T", obj)
				}

				for i, c := range input {
					if con.Namespace == c.Namespace && con.Name == c.Name {
						input = append(input[:i], input[i+1:]...)
						return true, len(input) > 0, nil
					}
				}

				return false, len(input) > 0, nil
			}

			objects, e := r.Cache.Get(cache.PrimaryObjectIndex, "Connection/", cache.And(viewFilter, connFilter))
			if e != nil {
				return nil, e
			}

			// Connections are ephemeral - the absence of one should not prevent the closure of the others.
			for _, notFound := range input {
				graphql.AddErrorf(ctx, "connection: '%s' not found in namespace '%s'", notFound.Name, notFound.Namespace)
			}

			for _, obj := range objects {
				con, ok := obj.(*consolegraphql.Connection)
				if !ok {
					return nil, fmt.Errorf("unexpected type: %T", obj)
				}

					infraUid, e := r.GetInfraUid(con.Namespace, con.Spec.AddressSpace)
					if e != nil {
						graphql.AddErrorf(ctx, "failed to close connection: '%s' in namespace: '%s' - %+v", con.Name, con.Namespace, e)
						continue
					}

					collector := r.GetCollector(infraUid)
					if collector == nil {
						graphql.AddErrorf(ctx, "failed to close connection: '%s' in namespace: '%s' - cannot find collector for infraUuid '%s' at this time",
							con.Name, con.Namespace, infraUid)
						continue
					}

				token := requestState.UserAccessToken

				commandDelegate, e := collector.CommandDelegate(token, requestState.ImpersonatedUser)
				if e != nil {
					graphql.AddErrorf(ctx, "failed to close connection: '%s' in namespace '%s', %+v", con.Name, con.Namespace, e)
					continue
				}

				e = commandDelegate.CloseConnection(con.ObjectMeta)
				if e != nil {
					graphql.AddErrorf(ctx, "connection: '%s' in namespace '%s' could not be closed : %+v", con.Name, con.Namespace, e)
				}

			}
	*/
	return &t, nil
}
