/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"sort"
)

type AddressSpacePlanByDisplayOrder []*v1beta2.AddressSpacePlan

func (a AddressSpacePlanByDisplayOrder) Len() int { return len(a) }
func (a AddressSpacePlanByDisplayOrder) Less(i, j int) bool {
	return a[i].Spec.DisplayOrder < a[j].Spec.DisplayOrder
}
func (a AddressSpacePlanByDisplayOrder) Swap(i, j int) { a[i], a[j] = a[j], a[i] }

type AddressPlanByDisplayOrder []*v1beta2.AddressPlan

func (a AddressPlanByDisplayOrder) Len() int { return len(a) }
func (a AddressPlanByDisplayOrder) Less(i, j int) bool {
	return a[i].Spec.DisplayOrder < a[j].Spec.DisplayOrder
}
func (a AddressPlanByDisplayOrder) Swap(i, j int) { a[i], a[j] = a[j], a[i] }

func (r *Resolver) AddressSpacePlanSpec_admin_enmasse_io_v1beta2() AddressSpacePlanSpec_admin_enmasse_io_v1beta2Resolver {
	return &addressSpacePlanSpecK8sResolver{r}
}

func (r *Resolver) AddressPlanSpec_admin_enmasse_io_v1beta2() AddressPlanSpec_admin_enmasse_io_v1beta2Resolver {
	return &addressPlanSpecK8sResolver{r}
}

func (r *queryResolver) AddressPlans(ctx context.Context, addressSpacePlan *string) ([]*v1beta2.AddressPlan, error) {
	var planFilter cache.ObjectFilter
	if addressSpacePlan != nil {
		spaceFilter := func(obj interface{}) (bool, bool, error) {
			asp, ok := obj.(*v1beta2.AddressSpacePlan)
			if !ok {
				return false, false, fmt.Errorf("unexpected type: %T", obj)
			}

			if asp.Name == *addressSpacePlan {
				return true, false, nil
			} else {
				return false, true, nil
			}
		}
		objs, e := r.Cache.Get("hierarchy", "AddressSpacePlan/", spaceFilter)
		if e != nil {
			return nil, e
		}
		if len(objs) == 0 {
			return nil, fmt.Errorf("address space plan '%s' not found", *addressSpacePlan)
		}

		asp := objs[0].(*v1beta2.AddressSpacePlan)

		planFilter = func(obj interface{}) (bool, bool, error) {
			ap, ok := obj.(*v1beta2.AddressPlan)
			if !ok {
				return false, false, fmt.Errorf("unexpected type: %T", obj)
			}

			for _, planName := range asp.Spec.AddressPlans {
				if ap.ObjectMeta.Name == planName {
					return true, true, nil
				}
			}
			return false, true, nil
		}
	}
	objects, e := r.Cache.Get("hierarchy", "AddressPlan/", planFilter)
	if e != nil {
		return nil, e
	}
	plans := make([]*v1beta2.AddressPlan, len(objects))
	for i, p := range objects {
		plans[i] = p.(*v1beta2.AddressPlan)
	}
	sort.Sort(AddressPlanByDisplayOrder(plans))
	return plans, e
}

func (r *queryResolver) AddressSpacePlans(ctx context.Context, addressSpaceType *AddressSpaceType) ([]*v1beta2.AddressSpacePlan, error) {
	var planFilter cache.ObjectFilter
	if addressSpaceType != nil {
		planFilter = func(obj interface{}) (bool, bool, error) {
			ap, ok := obj.(*v1beta2.AddressSpacePlan)
			if !ok {
				return false, false, fmt.Errorf("unexpected type: %T", obj)
			}

			return ap.Spec.AddressSpaceType == string(*addressSpaceType), true, nil
		}
	}

	objects, e := r.Cache.Get("hierarchy", "AddressSpacePlan/", planFilter)
	if e != nil {
		return nil, e
	}
	plans := make([]*v1beta2.AddressSpacePlan, len(objects))
	for i, p := range objects {
		plans[i] = p.(*v1beta2.AddressSpacePlan)
	}
	sort.Sort(AddressSpacePlanByDisplayOrder(plans))
	return plans, e

}

type addressSpacePlanSpecK8sResolver struct{ *Resolver }

func (r *addressSpacePlanSpecK8sResolver) AddressPlans(ctx context.Context, obj *v1beta2.AddressSpacePlanSpec) ([]*v1beta2.AddressPlan, error) {

	planNames := obj.AddressPlans

	planFilter := func(obj interface{}) (bool, bool, error) {
		ap, ok := obj.(*v1beta2.AddressPlan)
		if !ok {
			return false, false, fmt.Errorf("unexpected type: %T", obj)
		}

		for _, planName := range planNames {
			if ap.ObjectMeta.Name == planName {
				return true, true, nil
			}
		}
		return false, true, nil
	}

	objects, e := r.Cache.Get("hierarchy", "AddressPlan", planFilter)
	if e != nil {
		return nil, e
	}
	plans := make([]*v1beta2.AddressPlan, len(objects))
	for i, p := range objects {
		plans[i] = p.(*v1beta2.AddressPlan)
	}
	sort.Sort(AddressPlanByDisplayOrder(plans))
	return plans, nil
}

func (r *addressSpacePlanSpecK8sResolver) AddressSpaceType(ctx context.Context, obj *v1beta2.AddressSpacePlanSpec) (*AddressSpaceType, error) {
	if obj != nil {
		spaceType := AddressSpaceTypeStandard
		if obj.AddressSpaceType == string(AddressSpaceTypeBrokered) {
			spaceType = AddressSpaceTypeBrokered
		}
		return &spaceType, nil
	}
	return nil, nil
}

type addressPlanSpecK8sResolver struct{ *Resolver }

func (r *addressPlanSpecK8sResolver) AddressType(ctx context.Context, obj *v1beta2.AddressPlanSpec) (AddressType, error) {
	return AddressType(obj.AddressType), nil
}

func (r *queryResolver) AddressSpaceTypes(ctx context.Context) ([]AddressSpaceType, error) {
	types := make([]AddressSpaceType, 0)
	types = append(types, AddressSpaceTypeStandard, AddressSpaceTypeBrokered)
	return types, nil
}

func (r *queryResolver) AddressTypes(ctx context.Context) ([]AddressType, error) {
	types := make([]AddressType, 0)
	types = append(types, AddressTypeTopic, AddressTypeQueue, AddressTypeMulticast, AddressTypeAnycast)
	return types, nil
}
