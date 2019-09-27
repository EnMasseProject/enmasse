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
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
)

func (r *Resolver) AddressSpec_enmasse_io_v1beta1() AddressSpec_enmasse_io_v1beta1Resolver {
	return &addressSpecK8sResolver{r}
}

type addressSpecK8sResolver struct{ *Resolver }

func (r *queryResolver) Addresses(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*AddressQueryResultConsoleapiEnmasseIoV1beta1, error) {
	//lower, upper := calcLowerUpper(offset, first, len(obj.Links))
	objects, e := r.Cache.Get("Address/", nil)
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
			Metrics: make([]*MetricConsoleapiEnmasseIoV1beta1, 0),
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
		planFilter := func(obj runtime.Object) (bool, error) {
			asp, ok := obj.(*v1beta2.AddressSpacePlan)
			if !ok {
				return false, fmt.Errorf("unexpected type: %T", obj)
			}
			return asp.Name == addressPlanName, nil
		}

		objs, e := r.Cache.Get("AddressPlan", planFilter)
		if e != nil {
			return nil, e
		}

		if len(objs) == 0 {
			// There might be a plan change in progress, or the user may have created a space refering to
			// an unknown plan.
			return &v1beta2.AddressPlan{
				ObjectMeta: v1.ObjectMeta{
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

func (r *mutationResolver) CreateAddress(ctx context.Context, input v1beta1.Address) (*v1.ObjectMeta, error) {
	nw, e := r.CoreConfig.Addresses(input.Namespace).Create(&input)
	if e != nil {
		return nil, e
	}
	return &nw.ObjectMeta, e
}

func (r *mutationResolver) PatchAddress(ctx context.Context, input v1.ObjectMeta, patch string, patchType string) (*bool, error) {
	pt := types.PatchType(patchType)
	//fmt.Printf("JSON patch : %s patch type %s pt %s", patch, patchType, pt)
	_, e := r.CoreConfig.Addresses(input.Namespace).Patch(input.Name, pt, []byte(patch))
	b := e == nil
	return &b, e
}

func (r *mutationResolver) DeleteAddress(ctx context.Context, input v1.ObjectMeta) (*bool, error) {
	e := r.CoreConfig.Addresses(input.Namespace).Delete(input.Name, &v1.DeleteOptions{})
	b := e == nil
	return &b, e
}

func (r *mutationResolver) PurgeAddress(ctx context.Context, input v1.ObjectMeta) (*bool, error) {
	panic("implement me")
}

func (r *queryResolver) AddressCommand(ctx context.Context, input v1beta1.Address) (string, error) {
	panic("implement me")
}
