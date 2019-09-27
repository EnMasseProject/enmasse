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
)

func (r *Resolver) Link_consoleapi_enmasse_io_v1beta1() Link_consoleapi_enmasse_io_v1beta1Resolver {
	return &linkK8sResolver{r}
}

type linkK8sResolver struct{ *Resolver }

func (l linkK8sResolver) Metrics(ctx context.Context, obj *LinkConsoleapiEnmasseIoV1beta1) ([]*consolegraphql.Metric, error) {
	if obj != nil {

		key := fmt.Sprintf("Link/%s/%s/%s/", obj.ObjectMeta.Namespace, obj.Spec.AddressSpace, obj.ObjectMeta.Name)
		dynamic, e := l.MetricCache.Get("id", key, nil)
		if e != nil {
			return nil, e
		}
		metrics := make([]*consolegraphql.Metric, 0)

		for _, i := range dynamic {
			metric := i.(*consolegraphql.Metric)
			metrics = append(metrics, metric)
		}

		return metrics, nil
	}
	return nil, nil
}

type linkSpecK8sResolver struct{ *Resolver }

func (l linkSpecK8sResolver) Connection(ctx context.Context, obj *consolegraphql.LinkSpec) (*ConnectionConsoleapiEnmasseIoV1beta1, error) {
	if obj != nil {
		linkrsctx := graphql.GetResolverContext(ctx).Parent.Parent
		link := linkrsctx.Result.(**LinkConsoleapiEnmasseIoV1beta1)

		namespace := (*link).ObjectMeta.Namespace
		objs, e := l.Cache.Get("hierarchy", fmt.Sprintf("Connection/%s/%s/%s", namespace, obj.AddressSpace, obj.Connection), nil)
		if e != nil {
			return nil, e
		}
		if len(objs) == 0 {
			return nil, fmt.Errorf("connection '%s' in address space %s in namespace '%s' not found", obj.Connection, obj.AddressSpace, namespace)
		}

		con := objs[0].(*consolegraphql.Connection)
		return &ConnectionConsoleapiEnmasseIoV1beta1{
			ObjectMeta: &con.ObjectMeta,
			Spec:       &con.Spec,
		}, nil
	}
	return nil, nil
}

func (l linkSpecK8sResolver) Role(ctx context.Context, obj *consolegraphql.LinkSpec) (LinkRole, error) {
	if obj != nil {
		if obj.Role == "sender" {
			return LinkRoleSender, nil
		} else {
			return LinkRoleReceiver, nil
		}
	}
	return LinkRoleSender, nil
}
