/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/99designs/gqlgen/graphql"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type connectionSpecK8sResolver struct{ *Resolver }

func (c connectionSpecK8sResolver) AddressSpace(ctx context.Context, obj *consolegraphql.ConnectionSpec) (*AddressSpaceConsoleapiEnmasseIoV1beta1, error) {
	if obj != nil {
		conrsctx := graphql.GetResolverContext(ctx).Parent.Parent
		con := conrsctx.Result.(**ConnectionConsoleapiEnmasseIoV1beta1)

		namespace := (*con).ObjectMeta.Namespace
		objs, e := c.Cache.Get(fmt.Sprintf("AddressSpace/%s/%s", namespace, obj.AddressSpace), nil)
		if e != nil {
			return nil, e
		}
		if len(objs) == 0 {
			return nil, fmt.Errorf("address space '%s' in namespace '%s' not found", obj.AddressSpace, namespace)
		}

		as := objs[0].(*v1beta1.AddressSpace)
		return &AddressSpaceConsoleapiEnmasseIoV1beta1{
			ObjectMeta: &as.ObjectMeta,
			Spec:       &as.Spec,
			Status:     &as.Status,
		}, nil
	}
	return nil, nil
}

func (c connectionSpecK8sResolver) Protocol(ctx context.Context, obj *consolegraphql.ConnectionSpec) (Protocol, error) {
	if obj != nil {
		return Protocol(obj.Protocol), nil
	}
	return ProtocolAmqp, nil
}

func (c connectionSpecK8sResolver) Properties(ctx context.Context, obj *consolegraphql.ConnectionSpec) ([]*KeyValue, error) {
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

func (r *Resolver) ConnectionSpec_consoleapi_enmasse_io_v1beta1() ConnectionSpec_consoleapi_enmasse_io_v1beta1Resolver {
	return &connectionSpecK8sResolver{r}
}

func (r *queryResolver) Connections(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*ConnectionQueryResultConsoleapiEnmasseIoV1beta1, error) {

	objects, e := r.Cache.Get("Connection/", nil)
	if e != nil {
		return nil, e
	}
	cons := make([]*ConnectionConsoleapiEnmasseIoV1beta1, len(objects))
	for i, a := range objects {
		con := a.(*consolegraphql.Connection)
		cons[i] = &ConnectionConsoleapiEnmasseIoV1beta1{
			ObjectMeta: &con.ObjectMeta,
			Spec:       &con.Spec,
			Links: &LinkQueryResultConsoleapiEnmasseIoV1beta1{
				Total: 0,
				Links: make([]*LinkConsoleapiEnmasseIoV1beta1, 0),
			},
			Metrics: make([]*MetricConsoleapiEnmasseIoV1beta1, 0),
		}
	}

	cqr := &ConnectionQueryResultConsoleapiEnmasseIoV1beta1{
		Total:       len(cons),
		Connections: cons,
	}

	return cqr, nil
}

func (r *mutationResolver) CloseConnection(ctx context.Context, input v1.ObjectMeta) (*bool, error) {
	panic("implement me")
}
