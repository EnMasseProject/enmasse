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
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"time"
)

type connectionK8sResolver struct{ *Resolver }

func (r *Resolver) Connection_consoleapi_enmasse_io_v1beta1() Connection_consoleapi_enmasse_io_v1beta1Resolver {
	return &connectionK8sResolver{r}
}

func (cr connectionK8sResolver) Links(ctx context.Context, obj *consolegraphql.Connection, first *int, offset *int, filter *string, orderBy *string) (*LinkQueryResultConsoleapiEnmasseIoV1beta1, error) {
	if obj != nil {
		fltrfunc, e := BuildFilter(filter)
		if e != nil {
			return nil, e
		}

		orderer, e := BuildOrderer(orderBy)
		if e != nil {
			return nil, e
		}

		links, e := cr.Cache.Get("hierarchy", fmt.Sprintf("Link/%s/%s/%s/", obj.ObjectMeta.Namespace, obj.Spec.AddressSpace, obj.ObjectMeta.Name), fltrfunc)
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

		return &LinkQueryResultConsoleapiEnmasseIoV1beta1{
			Total: len(links),
			Links: consolelinks,
		}, nil
	}
	return nil, nil
}

type connectionSpecK8sResolver struct{ *Resolver }

func (r *Resolver) ConnectionSpec_consoleapi_enmasse_io_v1beta1() ConnectionSpec_consoleapi_enmasse_io_v1beta1Resolver {
	return &connectionSpecK8sResolver{r}
}

func (cs connectionSpecK8sResolver) AddressSpace(ctx context.Context, obj *consolegraphql.ConnectionSpec) (*consolegraphql.AddressSpaceHolder, error) {
	if obj != nil {
		conrsctx := graphql.GetResolverContext(ctx).Parent.Parent
		con := conrsctx.Result.(**consolegraphql.Connection)

		namespace := (*con).ObjectMeta.Namespace
		objs, e := cs.Cache.Get("hierarchy", fmt.Sprintf("AddressSpace/%s/%s", namespace, obj.AddressSpace), nil)
		if e != nil {
			return nil, e
		}
		if len(objs) == 0 {
			return nil, fmt.Errorf("address space '%s' in namespace '%s' not found", obj.AddressSpace, namespace)
		}

		as := objs[0].(*consolegraphql.AddressSpaceHolder)
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

func (r *queryResolver) Connections(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*ConnectionQueryResultConsoleapiEnmasseIoV1beta1, error) {
	fltrfunc, e := BuildFilter(filter)
	if e != nil {
		return nil, e
	}

	orderer, e := BuildOrderer(orderBy)
	if e != nil {
		return nil, e
	}

	objects, e := r.Cache.Get("hierarchy", "Connection/", fltrfunc)
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
	for i, a := range paged {
		con := a.(*consolegraphql.Connection)
		now := time.Now()

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

		_, e := r.Cache.Get("hierarchy", fmt.Sprintf("Link/%s/%s/%s/", con.ObjectMeta.Namespace, con.Spec.AddressSpace, con.ObjectMeta.Name), roleCountingFilter)
		if e != nil {
			return nil, e
		}

		metrics := make([]*consolegraphql.Metric, 0)

		senders, metrics := consolegraphql.FindOrCreateSimpleMetric(metrics,"enmasse_senders", "gauge" )
		senders.Update(float64(linkCounts["sender"]), now)
		receivers, metrics := consolegraphql.FindOrCreateSimpleMetric(metrics,"enmasse_receivers", "gauge" )
		receivers.Update(float64(linkCounts["receiver"]), now)

		for _, metric := range con.Metrics {
			metrics = append(metrics, metric)
		}

		cons[i] = &consolegraphql.Connection{
			ObjectMeta: con.ObjectMeta,
			Spec:       con.Spec,
			Metrics:    metrics,
		}

	}

	cqr := &ConnectionQueryResultConsoleapiEnmasseIoV1beta1{
		Total:       len(objects),
		Connections: cons,
	}

	return cqr, nil
}

func (r *mutationResolver) CloseConnection(ctx context.Context, input v1.ObjectMeta) (*bool, error) {
	panic("implement me")
}
