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
)

func (r *Resolver) AddressSpaceSpec_enmasse_io_v1beta1() AddressSpaceSpec_enmasse_io_v1beta1Resolver {
	return &addressSpaceSpecK8sResolver{r}
}

func (r *queryResolver) AddressSpaces(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*AddressSpaceQueryResultConsoleapiEnmasseIoV1beta1, error) {
	objects, e := r.Cache.Get("AddressSpace/", nil)
	if e != nil {
		return nil, e
	}
	addressspaces := make([]*AddressSpaceConsoleapiEnmasseIoV1beta1, len(objects))
	for i, as := range objects {
		addr := as.(*v1beta1.AddressSpace)
		addressspaces[i] = &AddressSpaceConsoleapiEnmasseIoV1beta1{
			ObjectMeta: &addr.ObjectMeta,
			Spec:       &addr.Spec,
			Status:     &addr.Status,
			Connections: &ConnectionQueryResultConsoleapiEnmasseIoV1beta1{
				Total:       0,
				Connections: make([]*ConnectionConsoleapiEnmasseIoV1beta1, 0),
			},
			Metrics: make([]*MetricConsoleapiEnmasseIoV1beta1, 0),
		}
	}

	asqr := &AddressSpaceQueryResultConsoleapiEnmasseIoV1beta1{
		Total:         len(addressspaces),
		AddressSpaces: addressspaces,
	}
	return asqr, nil
}

type addressSpaceSpecK8sResolver struct{ *Resolver }

func (r *addressSpaceSpecK8sResolver) Plan(ctx context.Context, obj *v1beta1.AddressSpaceSpec) (*v1beta2.AddressSpacePlan, error) {
	if obj != nil {
		addressSpacePlan := obj.Plan
		spaceFilter := func(obj runtime.Object) (bool, error) {
			asp, ok := obj.(*v1beta2.AddressSpacePlan)
			if !ok {
				return false, fmt.Errorf("unexpected type: %T", obj)
			}
			return asp.Name == addressSpacePlan, nil
		}
		objs, e := r.Cache.Get("AddressSpacePlan", spaceFilter)
		if e != nil {
			return nil, e
		}

		if len(objs) == 0 {
			// There might be a plan change in progress, or the user may have created a space refering to
			// an unknown plan.
			return &v1beta2.AddressSpacePlan{
				ObjectMeta: v1.ObjectMeta{
					Name: addressSpacePlan,
				},
				Spec: v1beta2.AddressSpacePlanSpec{
					AddressPlans:     make([]string, 0),
					AddressSpaceType: obj.Type,
					DisplayName:      addressSpacePlan,
					LongDescription:  "",
					ShortDescription: "",
					InfraConfigRef:   "",
					DisplayOrder:     0,
					ResourceLimits:   nil,
				},
			}, nil
		}

		asp := objs[0].(*v1beta2.AddressSpacePlan)
		return asp, nil
	}
	return nil, nil
}

func (r *addressSpaceSpecK8sResolver) Type(ctx context.Context, obj *v1beta1.AddressSpaceSpec) (AddressSpaceType, error) {
	spaceType := AddressSpaceTypeStandard
	if obj != nil {
		if obj.Type == string(AddressSpaceTypeBrokered) {
			spaceType = AddressSpaceTypeBrokered
		}
	}
	return spaceType, nil
}

func (r *queryResolver) MessagingCertificateChain(ctx context.Context, input v1.ObjectMeta) (string, error) {
	panic("implement me")
}

func (r *mutationResolver) CreateAddressSpace(ctx context.Context, input v1beta1.AddressSpace) (*v1.ObjectMeta, error) {
	panic("implement me")
}

func (r *mutationResolver) PatchAddressSpace(ctx context.Context, input v1.ObjectMeta, patch string, patchType string) (*bool, error) {
	panic("implement me")
}

func (r *mutationResolver) DeleteAddressSpace(ctx context.Context, input v1.ObjectMeta) (*bool, error) {
	panic("implement me")
}

func (r *queryResolver) AddressSpaceCommand(ctx context.Context, input v1beta1.AddressSpace) (string, error) {
	panic("implement me")
}
