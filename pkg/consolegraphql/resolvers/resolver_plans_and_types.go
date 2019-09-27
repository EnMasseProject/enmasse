/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"io/ioutil"
	"os"
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

func (r *queryResolver) AddressPlans(ctx context.Context, addressSpacePlan *string, addressType *AddressType) ([]*v1beta2.AddressPlan, error) {
	var bySpacePlanFilter cache.ObjectFilter
	var byTypePlanFilter cache.ObjectFilter
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
		objs, e := r.Cache.Get(cache.PrimaryObjectIndex, "AddressSpacePlan/", spaceFilter)
		if e != nil {
			return nil, e
		}
		if len(objs) == 0 {
			return nil, fmt.Errorf("address space plan '%s' not found", *addressSpacePlan)
		}

		asp := objs[0].(*v1beta2.AddressSpacePlan)

		bySpacePlanFilter = func(obj interface{}) (bool, bool, error) {
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

	if addressType != nil {
		byTypePlanFilter = func(obj interface{}) (bool, bool, error) {
			ap, ok := obj.(*v1beta2.AddressPlan)
			if !ok {
				return false, false, fmt.Errorf("unexpected type: %T", obj)
			}

			return ap.Spec.AddressType == string(*addressType), true, nil
		}
	}

	objects, e := r.Cache.Get(cache.PrimaryObjectIndex, "AddressPlan/", cache.And(bySpacePlanFilter, byTypePlanFilter))
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

	objects, e := r.Cache.Get(cache.PrimaryObjectIndex, "AddressSpacePlan/", planFilter)
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

	objects, e := r.Cache.Get(cache.PrimaryObjectIndex, "AddressPlan", planFilter)
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
	err := lazyLoadAddressSpaceTypes()
	if err != nil {
		return nil, err
	}

	types := make([]AddressSpaceType, 0)
	for _, t := range addressSpaceTypes {
		types = append(types, t.Spec.AddressSpaceType)
	}
	return types, nil
}

func (r *queryResolver) AddressTypes(ctx context.Context) ([]AddressType, error) {
	contains := func(s []AddressType, e AddressType) bool {
		for _, a := range s {
			if a == e {
				return true
			}
		}
		return false
	}

	err := lazyLoadAddressTypes()
	if err != nil {
		return nil, err
	}
	types := make([]AddressType, 0)
	for _, t := range addressTypes {
		if !contains(types, t.Spec.AddressType) {
			types = append(types, t.Spec.AddressType)
		}
	}
	return types, nil
}

func (r *queryResolver) AddressSpaceTypesV2(ctx context.Context) ([]*AddressSpaceTypeConsoleapiEnmasseIoV1beta1, error) {
	err := lazyLoadAddressSpaceTypes()
	return addressSpaceTypes, err
}

func (r *queryResolver) AddressTypesV2(ctx context.Context, addressSpaceType *AddressSpaceType) ([]*AddressTypeConsoleapiEnmasseIoV1beta1, error) {
	err := lazyLoadAddressTypes()
	if err != nil {
		return nil, err
	}
	if addressSpaceType != nil {
		filtered := make([]*AddressTypeConsoleapiEnmasseIoV1beta1, 0)
		for _, t := range addressTypes {
			if t.Spec.AddressSpaceType == *addressSpaceType {
				filtered = append(filtered, t)
			}
		}
		return filtered, nil

	} else {
		return addressTypes, err
	}
}

const defaultAddressSpaceTypes = "addressSpaceTypes.json"
const defaultAddressTypes = "addressTypes.json"

var addressSpaceTypes []*AddressSpaceTypeConsoleapiEnmasseIoV1beta1
var addressTypes []*AddressTypeConsoleapiEnmasseIoV1beta1

func lazyLoadAddressSpaceTypes() error {
	if addressSpaceTypes != nil {
		return nil
	}

	fileName := os.Getenv("ENMASSE_ADDRESS_SPACE_TYPE_FILE")
	if fileName == "" {
		fileName = defaultAddressSpaceTypes
	}

	addressSpaceTypes = make([]*AddressSpaceTypeConsoleapiEnmasseIoV1beta1, 0)
	err := loadTypesFrom(fileName, &addressSpaceTypes)
	return err
}

func lazyLoadAddressTypes() error {
	if addressTypes != nil {
		return nil
	}

	fileName := os.Getenv("ENMASSE_ADDRESS_TYPE_FILE")
	if fileName == "" {
		fileName = defaultAddressTypes
	}

	addressTypes = make([]*AddressTypeConsoleapiEnmasseIoV1beta1, 0)
	err := loadTypesFrom(fileName, &addressTypes)
	return err
}

func loadTypesFrom(fileName string, data interface{}) error {
	file, err := os.Open(fileName)
	if err != nil {
		return err
	}

	defer func() { _ = file.Close() }()

	byteValue, err := ioutil.ReadAll(file)
	if err != nil {
		return err
	}

	err = json.Unmarshal(byteValue, data)
	return err
}

