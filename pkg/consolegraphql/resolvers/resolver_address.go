/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"strings"
	time2 "time"
)

func (r *Resolver) Address_consoleapi_enmasse_io_v1beta1() Address_consoleapi_enmasse_io_v1beta1Resolver {
	return &addressK8sResolver{r}
}

type addressK8sResolver struct{ *Resolver }

func (ar addressK8sResolver) Links(ctx context.Context, obj *AddressConsoleapiEnmasseIoV1beta1, first *int, offset *int, filter *string, orderBy *string) (*LinkQueryResultConsoleapiEnmasseIoV1beta1, error) {
	if obj != nil {
		addrtoks := strings.SplitN(obj.ObjectMeta.Name, ".", 2)
		// N.B. address name not prefixed in the link index
		links, e := ar.Cache.Get("addressLinkHierarchy", fmt.Sprintf("Link/%s/%s/%s/", obj.ObjectMeta.Namespace, addrtoks[0], addrtoks[1]), nil)
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

func (r *Resolver) AddressSpec_enmasse_io_v1beta1() AddressSpec_enmasse_io_v1beta1Resolver {
	return &addressSpecK8sResolver{r}
}

func (ar addressK8sResolver) Metrics(ctx context.Context, obj *AddressConsoleapiEnmasseIoV1beta1) ([]*consolegraphql.Metric, error) {

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

		addrtoks := strings.SplitN(obj.ObjectMeta.Name, ".", 2)
		// address name not addressspace name prefixed in the link index
		_, e := ar.Cache.Get("addressLinkHierarchy", fmt.Sprintf("Link/%s/%s/%s/", obj.ObjectMeta.Namespace, addrtoks[0], addrtoks[1]), roleCountingFilter)
		if e != nil {
			return nil, e
		}

		key := fmt.Sprintf("Address/%s/%s/%s/", obj.ObjectMeta.Namespace, addrtoks[0], addrtoks[1])
		dynamic, e := ar.MetricCache.Get("id", key, nil)
		metrics := make([]*consolegraphql.Metric, 2)

		metrics[0] = &consolegraphql.Metric{
			Value:        consolegraphql.NewSimpleMetricValue("enmasse_senders", "gauge", float64(linkCounts["sender"]), "", time2.Now()),
			//
			//MetricName:  "enmasse_senders",
			//MetricType:  "gauge",
			//MetricValue: float64(linkCounts["sender"]),
		}
		metrics[1] = &consolegraphql.Metric{
			Value:        consolegraphql.NewSimpleMetricValue("enmasse_receivers", "gauge", float64(linkCounts["receiver"]), "", time2.Now()),
			//MetricName:  "enmasse_receivers",
			//MetricType:  "gauge",
			//MetricValue: float64(linkCounts["receiver"]),
		}

		for _, i := range dynamic {
			metric := i.(*consolegraphql.Metric)
			metrics = append(metrics, metric)
		}

		return metrics, nil
	}
	return nil, nil
}

type addressSpecK8sResolver struct{ *Resolver }

func (r *queryResolver) Addresses(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*AddressQueryResultConsoleapiEnmasseIoV1beta1, error) {
	//lower, upper := calcLowerUpper(offset, first, len(obj.Links))
	objects, e := r.Cache.Get("hierarchy", "Address/", nil)
	if e != nil {
		return nil, e
	}
	addresses := make([]*AddressConsoleapiEnmasseIoV1beta1, len(objects))
	for i, a := range objects {
		addr := a.(*v1beta1.Address)
		addresses[i] = &AddressConsoleapiEnmasseIoV1beta1{
			ObjectMeta: &addr.ObjectMeta,
			Spec:       &addr.Spec,
			Status:     &addr.Status,
			Links: &LinkQueryResultConsoleapiEnmasseIoV1beta1{
				Total: 0,
				Links: make([]*LinkConsoleapiEnmasseIoV1beta1, 0),
			},
			Metrics: make([]*consolegraphql.Metric, 0),
		}
	}

	aqr := &AddressQueryResultConsoleapiEnmasseIoV1beta1{
		Total:     len(addresses),
		Addresses: addresses,
	}

	return aqr, nil

}

func (r *addressSpecK8sResolver) Plan(ctx context.Context, obj *v1beta1.AddressSpec) (*v1beta2.AddressPlan, error) {
	if obj != nil {
		addressPlanName := obj.Plan
		planFilter := func(obj interface{}) (bool, bool, error) {
			asp, ok := obj.(*v1beta2.AddressSpacePlan)
			if !ok {
				return false, false, fmt.Errorf("unexpected type: %T", obj)
			}
			return asp.Name == addressPlanName, true, nil
		}

		objs, e := r.Cache.Get("hierarchy", "AddressPlan", planFilter)
		if e != nil {
			return nil, e
		}

		if len(objs) == 0 {
			// There might be a plan change in progress, or the user may have created a space referring to
			// an unknown plan.
			return &v1beta2.AddressPlan{
				ObjectMeta: metav1.ObjectMeta{
					Name: addressPlanName,
				},
				Spec: v1beta2.AddressPlanSpec{
					DisplayName: addressPlanName,
				},
			}, nil
		}

		ap := objs[0].(*v1beta2.AddressPlan)
		return ap, nil
	}
	return nil, nil
}

func (r *addressSpecK8sResolver) Type(ctx context.Context, obj *v1beta1.AddressSpec) (AddressType, error) {
	return AddressType(obj.Type), nil
}

func (r *mutationResolver) CreateAddress(ctx context.Context, input v1beta1.Address) (*metav1.ObjectMeta, error) {
	nw, e := r.CoreConfig.Addresses(input.Namespace).Create(&input)
	if e != nil {
		return nil, e
	}
	return &nw.ObjectMeta, e
}

func (r *mutationResolver) PatchAddress(ctx context.Context, input metav1.ObjectMeta, patch string, patchType string) (*bool, error) {
	pt := types.PatchType(patchType)
	//fmt.Printf("JSON patch : %s patch type %s pt %s", patch, patchType, pt)
	_, e := r.CoreConfig.Addresses(input.Namespace).Patch(input.Name, pt, []byte(patch))
	b := e == nil
	return &b, e
}

func (r *mutationResolver) DeleteAddress(ctx context.Context, input metav1.ObjectMeta) (*bool, error) {
	e := r.CoreConfig.Addresses(input.Namespace).Delete(input.Name, &metav1.DeleteOptions{})
	b := e == nil
	return &b, e
}

func (r *mutationResolver) PurgeAddress(ctx context.Context, input metav1.ObjectMeta) (*bool, error) {
	panic("implement me")
}

func (r *queryResolver) AddressCommand(ctx context.Context, input v1beta1.Address) (string, error) {
	panic("implement me")
}
