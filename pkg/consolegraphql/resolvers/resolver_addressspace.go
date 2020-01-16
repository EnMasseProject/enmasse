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
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
)

func (r *Resolver) AddressSpace_consoleapi_enmasse_io_v1beta1() AddressSpace_consoleapi_enmasse_io_v1beta1Resolver {
	return &addressSpaceK8sResolver{r}
}

func (r *Resolver) AddressSpaceSpec_enmasse_io_v1beta1() AddressSpaceSpec_enmasse_io_v1beta1Resolver {
	return &addressSpaceSpecK8sResolver{r}
}

func (r *queryResolver) AddressSpaces(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*AddressSpaceQueryResultConsoleapiEnmasseIoV1beta1, error) {
	fltrfunc, err := BuildFilter(filter)
	if err != nil {
		return nil, err
	}

	orderer, err := BuildOrderer(orderBy)
	if err != nil {
		return nil, err
	}

	objects, e := r.Cache.Get(cache.PrimaryObjectIndex, "AddressSpace/", fltrfunc)
	if e != nil {
		return nil, e
	}

	e = orderer(objects)
	if e != nil {
		return nil, e
	}

	lower, upper := CalcLowerUpper(offset, first, len(objects))
	paged := objects[lower:upper]
	addressspaces := make([]*consolegraphql.AddressSpaceHolder, len(paged))
	for i, _ := range paged {
		addressspaces[i] = paged[i].(*consolegraphql.AddressSpaceHolder)
	}

	return &AddressSpaceQueryResultConsoleapiEnmasseIoV1beta1{
		Total:         len(objects),
		AddressSpaces: addressspaces,
	}, nil
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
		objs, e := r.Cache.Get(cache.PrimaryObjectIndex, "AddressSpacePlan", spaceFilter)
		if e != nil {
			return nil, e
		}

		if len(objs) == 0 {
			// There might be a plan change in progress, or the user may have created a space referring to
			// an unknown plan.
			return &v1beta2.AddressSpacePlan{
				ObjectMeta: v1.ObjectMeta{
					Name: addressSpacePlan,
				},
				Spec: v1beta2.AddressSpacePlanSpec{
					AddressPlans:     make([]string, 0),
					AddressSpaceType: obj.Type,
					DisplayName:      addressSpacePlan,
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


func (r *addressSpaceK8sResolver) Connections(ctx context.Context, obj *consolegraphql.AddressSpaceHolder, first *int, offset *int, filter *string, orderBy *string) (*ConnectionQueryResultConsoleapiEnmasseIoV1beta1, error) {
	if obj != nil {
		fltrfunc, e := BuildFilter(filter)
		if e != nil {
			return nil, e
		}

		orderer, e := BuildOrderer(orderBy)
		if e != nil {
			return nil, e
		}

		key := fmt.Sprintf("Connection/%s/%s/", obj.Namespace, obj.Name)
		objects, e := r.Cache.Get(cache.PrimaryObjectIndex, key, fltrfunc)
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
		for i, _ := range paged {
			cons[i] = paged[i].(*consolegraphql.Connection)
		}

		return &ConnectionQueryResultConsoleapiEnmasseIoV1beta1{
			Total:       len(objects),
			Connections: cons,
		}, nil
	}
	return nil, nil
}

func (r *addressSpaceK8sResolver) Addresses(ctx context.Context, obj *consolegraphql.AddressSpaceHolder, first *int, offset *int, filter *string, orderBy *string) (*AddressQueryResultConsoleapiEnmasseIoV1beta1, error) {
	if obj != nil {
		fltrfunc, e := BuildFilter(filter)
		if e != nil {
			return nil, e
		}

		orderer, e := BuildOrderer(orderBy)
		if e != nil {
			return nil, e
		}

		key := fmt.Sprintf("Address/%s/%s/", obj.Namespace, obj.Name)
		objects, e := r.Cache.Get(cache.PrimaryObjectIndex, key, fltrfunc)
		if e != nil {
			return nil, e
		}

		e = orderer(objects)
		if e != nil {
			return nil, e
		}

		lower, upper := CalcLowerUpper(offset, first, len(objects))
		paged := objects[lower:upper]

		addresses := make([]*consolegraphql.AddressHolder, len(paged))
		for i, _ := range paged {
			addresses[i] = paged[i].(*consolegraphql.AddressHolder)
		}

		aqr := &AddressQueryResultConsoleapiEnmasseIoV1beta1{
			Total:     len(objects),
			Addresses: addresses,
		}

		return aqr, nil
	}
	return nil, nil
}

func (r *queryResolver) MessagingCertificateChain(ctx context.Context, input v1.ObjectMeta) (string, error) {
	panic("implement me")
}

func (r *mutationResolver) CreateAddressSpace(ctx context.Context, input v1beta1.AddressSpace) (*v1.ObjectMeta, error) {
	requestState := server.GetRequestStateFromContext(ctx)

	nw, e := requestState.EnmasseV1beta1Client.AddressSpaces(input.Namespace).Create(&input)
	if e != nil {
		return nil, e
	}
	return &nw.ObjectMeta, e
}

func (r *mutationResolver) PatchAddressSpace(ctx context.Context, input v1.ObjectMeta, patch string, patchType string) (*bool, error) {
	pt := types.PatchType(patchType)
	requestState := server.GetRequestStateFromContext(ctx)

	_, e := requestState.EnmasseV1beta1Client.AddressSpaces(input.Namespace).Patch(input.Name, pt, []byte(patch))
	b := e == nil
	return &b, e
}

func (r *mutationResolver) DeleteAddressSpace(ctx context.Context, input v1.ObjectMeta) (*bool, error) {
	requestState := server.GetRequestStateFromContext(ctx)

	e := requestState.EnmasseV1beta1Client.AddressSpaces(input.Namespace).Delete(input.Name, &v1.DeleteOptions{})
	b := e == nil
	return &b, e
}

func (r *queryResolver) AddressSpaceCommand(ctx context.Context, input v1beta1.AddressSpace) (string, error) {
	panic("implement me")
}
