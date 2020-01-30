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
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"strings"
)

func (r *Resolver) Address_consoleapi_enmasse_io_v1beta1() Address_consoleapi_enmasse_io_v1beta1Resolver {
	return &addressK8sResolver{r}
}

type addressK8sResolver struct{ *Resolver }

const infraUuidAnnotation = "enmasse.io/infra-uuid"

func (ar addressK8sResolver) Links(ctx context.Context, obj *consolegraphql.AddressHolder, first *int, offset *int, filter *string, orderBy *string) (*LinkQueryResultConsoleapiEnmasseIoV1beta1, error) {
	if obj != nil {
		fltrfunc, e := BuildFilter(filter)
		if e != nil {
			return nil, e
		}

		orderer, e := BuildOrderer(orderBy)
		if e != nil {
			return nil, e
		}

		addrtoks, e := tokenizeAddress(obj.ObjectMeta.Name)
		if e != nil {
			return nil, e
		}
		// N.B. address name not prefixed in the link index
		links, e := ar.Cache.Get(cache.AddressLinkObjectIndex, fmt.Sprintf("Link/%s/%s/%s/", obj.ObjectMeta.Namespace, addrtoks[0], addrtoks[1]), fltrfunc)
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

func (r *Resolver) AddressSpec_enmasse_io_v1beta1() AddressSpec_enmasse_io_v1beta1Resolver {
	return &addressSpecK8sResolver{r}
}

type addressSpecK8sResolver struct{ *Resolver }

func (r *queryResolver) Addresses(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*AddressQueryResultConsoleapiEnmasseIoV1beta1, error) {
	requestState := server.GetRequestStateFromContext(ctx)
	viewFilter := requestState.AccessController.ViewFilter()

	fltrfunc, e := BuildFilter(filter)
	if e != nil {
		return nil, e
	}

	orderer, e := BuildOrderer(orderBy)
	if e != nil {
		return nil, e
	}

	objects, e := r.Cache.Get(cache.PrimaryObjectIndex, "Address/", cache.And(viewFilter, fltrfunc))
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

func (r *addressSpecK8sResolver) Plan(ctx context.Context, obj *v1beta1.AddressSpec) (*v1beta2.AddressPlan, error) {
	if obj != nil {
		addressPlanName := obj.Plan
		planFilter := func(obj interface{}) (bool, bool, error) {
			asp, ok := obj.(*v1beta2.AddressPlan)
			if !ok {
				return false, false, fmt.Errorf("unexpected type: %T", obj)
			}
			return asp.Name == addressPlanName, true, nil
		}

		objs, e := r.Cache.Get(cache.PrimaryObjectIndex, "AddressPlan", planFilter)
		if e != nil {
			return nil, e
		}

		if len(objs) == 0 {
			// There might be a plan change in progress, or the user may have created an address referring to
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
	requestState := server.GetRequestStateFromContext(ctx)

	nw, e := requestState.EnmasseV1beta1Client.Addresses(input.Namespace).Create(&input)
	if e != nil {
		return nil, e
	}
	return &nw.ObjectMeta, e
}

func (r *mutationResolver) PatchAddress(ctx context.Context, input metav1.ObjectMeta, patch string, patchType string) (*bool, error) {
	pt := types.PatchType(patchType)
	requestState := server.GetRequestStateFromContext(ctx)

	_, e := requestState.EnmasseV1beta1Client.Addresses(input.Namespace).Patch(input.Name, pt, []byte(patch))
	b := e == nil
	return &b, e
}

func (r *mutationResolver) DeleteAddress(ctx context.Context, input metav1.ObjectMeta) (*bool, error) {
	requestState := server.GetRequestStateFromContext(ctx)

	e := requestState.EnmasseV1beta1Client.Addresses(input.Namespace).Delete(input.Name, &metav1.DeleteOptions{})
	b := e == nil
	return &b, e
}

func (r *mutationResolver) PurgeAddress(ctx context.Context, input metav1.ObjectMeta) (*bool, error) {
	requestState := server.GetRequestStateFromContext(ctx)

	f := false
	t := true
	addressToks, e := tokenizeAddress(input.Name)
	if e != nil {
		return &f, e
	}

	addressSpaces, e := r.Cache.Get(cache.PrimaryObjectIndex, fmt.Sprintf("AddressSpace/%s/%s", input.Namespace, addressToks[0]), nil)
	if e != nil {
		return nil, e
	}

	if len(addressSpaces) == 0 {
		return &f, fmt.Errorf("address space: '%s' not found ", addressToks[0])
	}

	as := addressSpaces[0].(*consolegraphql.AddressSpaceHolder).AddressSpace

	if as.ObjectMeta.Annotations == nil {
		return &f, fmt.Errorf("address space: '%s' does not have expected '%s' annotation ", as.Name, infraUuidAnnotation)
	}
	infraUid := as.ObjectMeta.Annotations[infraUuidAnnotation]
	if infraUid == "" {
		return &f, fmt.Errorf("address space: '%s' does not have expected '%s' annotation ", as.Name, infraUuidAnnotation)
	}

	addresses, e := r.Cache.Get(cache.PrimaryObjectIndex, fmt.Sprintf("Address/%s/%s/%s", input.Namespace, as.Name, input.Name), nil)
	if e != nil {
		return nil, e
	}

	if len(addresses) == 0 {
		return &f, fmt.Errorf("address: '%s' not found ", input.Name)
	}

	address := addresses[0].(*consolegraphql.AddressHolder).Address
	switch address.Spec.Type {
	case "subscription":
	case "queue":
	default:
		return &f, fmt.Errorf("address: '%s' cannot be purged, it is not of a supported type '%s'", address.Name, address.Spec.Type)
	}

	collector := r.GetCollector(infraUid)
	if collector == nil {
		return &f, fmt.Errorf("cannot find collector for infraUuid '%s' (address space %s) at this time", infraUid, as.Name)
	}

	commandDelegate, e := collector.CommandDelegate(requestState.UserAccessToken)
	if e != nil {
		return nil, e
	}

	e = commandDelegate.PurgeAddress(input)
	if e != nil {
		return nil, e
	}

	return &t, nil
}

func (r *queryResolver) AddressCommand(ctx context.Context, input v1beta1.Address) (string, error) {

	if input.TypeMeta.APIVersion == "" {
		input.TypeMeta.APIVersion = "enmasse.io/v1beta1"
	}
	if input.TypeMeta.Kind == "" {
		input.TypeMeta.Kind = "Address"
	}

	namespace := input.Namespace
	input.Namespace = ""

	return generateApplyCommand(input, namespace)
}

func tokenizeAddress(name string) ([]string, error) {
	addressToks := strings.SplitN(name, ".", 2)
	if len(addressToks) != 2 {
		return []string{}, fmt.Errorf("unexpectedly formatted address: '%s'.  expected separator not found ", name)
	}
	return addressToks, nil
}
