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

type connectionK8sResolver struct{ *Resolver }

func (r *Resolver) Connection_consoleapi_enmasse_io_v1beta1() Connection_consoleapi_enmasse_io_v1beta1Resolver {
	return &connectionK8sResolver{r}
}

func (cr connectionK8sResolver) Links(ctx context.Context, obj *ConnectionConsoleapiEnmasseIoV1beta1, first *int, offset *int, filter *string, orderBy *string) (*LinkQueryResultConsoleapiEnmasseIoV1beta1, error) {
	if obj != nil {
		links, e := cr.Cache.Get("hierarchy", fmt.Sprintf("Link/%s/%s/%s/", obj.ObjectMeta.Namespace, obj.Spec.AddressSpace, obj.ObjectMeta.Name), nil)
		if e != nil {
			return nil, e
		}

		consolelinks := make([]*LinkConsoleapiEnmasseIoV1beta1, 0)
		for _, obj := range links {
			link := obj.(*consolegraphql.Link)
			consolelinks = append(consolelinks, &LinkConsoleapiEnmasseIoV1beta1{
				ObjectMeta: &link.ObjectMeta,
				Spec:       &link.Spec,
				Metrics:    make([]*consolegraphql.Metric, 0),
			})
		}

		return &LinkQueryResultConsoleapiEnmasseIoV1beta1{
			Total: len(links),
			Links: consolelinks,
		}, nil
	}
	return nil, nil
}

func (cr connectionK8sResolver) Metrics(ctx context.Context, obj *ConnectionConsoleapiEnmasseIoV1beta1) ([]*consolegraphql.Metric, error) {
	if obj != nil {

		linkCounts := make(map[string]int, 0)
		linkCounts["sender"] = 0
		linkCounts["receiver"] = 0
		roleCountingFilter := func(obj interface{}) (bool, bool, error) {
			asp, ok := obj.(*consolegraphql.Link)
			if !ok {
				return false, false, fmt.Errorf("unexpected type: %T", obj)
			}

			if val, ok := linkCounts[asp.Spec.Role]; ok {
				linkCounts[asp.Spec.Role] = val + 1
			}
			return false, true, nil
		}

		_, e := cr.Cache.Get("hierarchy", fmt.Sprintf("Link/%s/%s/%s/", obj.ObjectMeta.Namespace, obj.Spec.AddressSpace, obj.ObjectMeta.Name), roleCountingFilter)
		if e != nil {
			return nil, e
		}

		key := fmt.Sprintf("Connection/%s/%s/%s/", obj.ObjectMeta.Namespace, obj.Spec.AddressSpace, obj.ObjectMeta.Name)
		dynamic, e := cr.MetricCache.Get("id", key, nil)
		if e != nil {
			return nil, e
		}
		metrics := make([]*consolegraphql.Metric, 2)

		metrics[0] = &consolegraphql.Metric{
			MetricName:  "enmasse_senders",
			MetricType:  "gauge",
			MetricValue: float64(linkCounts["sender"]),
		}
		metrics[1] = &consolegraphql.Metric{
			MetricName:  "enmasse_receivers",
			MetricType:  "gauge",
			MetricValue: float64(linkCounts["receiver"]),
		}

		for _, i := range dynamic {
			metric := i.(*consolegraphql.Metric)
			metrics = append(metrics, metric)
		}
		return metrics, nil
	}
	return nil, nil
}

type connectionSpecK8sResolver struct{ *Resolver }

func (r *Resolver) ConnectionSpec_consoleapi_enmasse_io_v1beta1() ConnectionSpec_consoleapi_enmasse_io_v1beta1Resolver {
	return &connectionSpecK8sResolver{r}
}

func (cs connectionSpecK8sResolver) AddressSpace(ctx context.Context, obj *consolegraphql.ConnectionSpec) (*AddressSpaceConsoleapiEnmasseIoV1beta1, error) {
	if obj != nil {
		conrsctx := graphql.GetResolverContext(ctx).Parent.Parent
		con := conrsctx.Result.(**ConnectionConsoleapiEnmasseIoV1beta1)

		namespace := (*con).ObjectMeta.Namespace
		objs, e := cs.Cache.Get("hierarchy", fmt.Sprintf("AddressSpace/%s/%s", namespace, obj.AddressSpace), nil)
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

func (cs connectionSpecK8sResolver) Protocol(ctx context.Context, obj *consolegraphql.ConnectionSpec) (Protocol, error) {
	if obj != nil {
		return Protocol(obj.Protocol), nil
	}
	return ProtocolAmqp, nil
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

func (r *Resolver) LinkSpec_consoleapi_enmasse_io_v1beta1() LinkSpec_consoleapi_enmasse_io_v1beta1Resolver {
	return &linkSpecK8sResolver{r}
}

func (r *Resolver) Links(ctx context.Context, obj *ConnectionConsoleapiEnmasseIoV1beta1, first *int, offset *int, filter *string, orderBy *string) (*LinkQueryResultConsoleapiEnmasseIoV1beta1, error) {
	panic("implement me")
}

func (r *queryResolver) Connections(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*ConnectionQueryResultConsoleapiEnmasseIoV1beta1, error) {

	objects, e := r.Cache.Get("hierarchy", "Connection/", nil)
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
			Metrics: make([]*consolegraphql.Metric, 0),
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
