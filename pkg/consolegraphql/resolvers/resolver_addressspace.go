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
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (r *Resolver) AddressSpace_consoleapi_enmasse_io_v1beta1() AddressSpace_consoleapi_enmasse_io_v1beta1Resolver {
	return &addressSpaceK8sResolver{r}
}

func (r *Resolver) AddressSpaceSpec_enmasse_io_v1beta1() AddressSpaceSpec_enmasse_io_v1beta1Resolver {
	return &addressSpaceSpecK8sResolver{r}
}

func (r *queryResolver) AddressSpaces(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*AddressSpaceQueryResultConsoleapiEnmasseIoV1beta1, error) {
	objects, e := r.Cache.Get("hierarchy", "AddressSpace/", nil)
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
			Metrics: make([]*consolegraphql.Metric, 0),
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
		spaceFilter := func(obj interface{}) (bool, bool, error) {
			asp, ok := obj.(*v1beta2.AddressSpacePlan)
			if !ok {
				return false, false, fmt.Errorf("unexpected type: %T", obj)
			}
			if asp.Name == addressSpacePlan {
				return true, false, nil
			} else {
				return false, true, nil
			}
		}
		objs, e := r.Cache.Get("hierarchy", "AddressSpacePlan", spaceFilter)
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

type addressSpaceK8sResolver struct{ *Resolver }

func (r *addressSpaceK8sResolver) Metrics(ctx context.Context, obj *AddressSpaceConsoleapiEnmasseIoV1beta1) ([]*consolegraphql.Metric, error) {
	if obj != nil {
		metrics := make([]*consolegraphql.Metric, 2)

		connections := 0
		_, e := r.Cache.Get("hierarchy", fmt.Sprintf("Connection/%s/%s/", obj.ObjectMeta.Namespace, obj.ObjectMeta.Name), func(obj interface{}) (bool, bool, error) {
			connections++
			return false, true, nil
		})
		if e != nil {
			return nil, e
		}

		addresses := 0
		_, e = r.Cache.Get("hierarchy", fmt.Sprintf("Address/%s/%s/", obj.ObjectMeta.Namespace, obj.ObjectMeta.Name), func(obj interface{}) (bool, bool, error) {
			addresses++
			return false, true, nil
		})
		if e != nil {
			return nil, e
		}

		metrics[0] = &consolegraphql.Metric{
			MetricName:  "enmasse-connections",
			MetricType:  "gauge",
			MetricValue: float64(connections),
		}
		metrics[1] = &consolegraphql.Metric{
			MetricName:  "enmasse-addresses",
			MetricType:  "gauge",
			MetricValue: float64(addresses),
		}
		return metrics, nil
	} else {
		return nil, nil
	}
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
