/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/99designs/gqlgen/graphql"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
)

func (r *Resolver) MessagingProject_consoleapi_enmasse_io_v1() MessagingProject_consoleapi_enmasse_io_v1Resolver {
	return &messagingProjectK8sResolver{r}
}

func (r *Resolver) MessagingProjectSpec_enmasse_io_v1() MessagingProjectSpec_enmasse_io_v1Resolver {
	return &messagingProjectSpecK8sResolver{r}
}

func (r *queryResolver) MessagingProjects(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*MessagingProjectQueryResultConsoleapiEnmasseIoV1, error) {
	requestState := server.GetRequestStateFromContext(ctx)
	viewFilter := requestState.AccessController.ViewFilter()

	fltrfunc, keyElements, err := BuildFilter(filter, "$.metadata.namespace", "$.metadata.name")
	if err != nil {
		return nil, err
	}

	orderer, err := BuildOrderer(orderBy)
	if err != nil {
		return nil, err
	}

	objects, e := r.Cache.Get(cache.PrimaryObjectIndex, fmt.Sprintf("MessagingProject/%s", keyElements), cache.And(viewFilter, fltrfunc))
	if e != nil {
		return nil, e
	}

	e = orderer(objects)
	if e != nil {
		return nil, e
	}

	lower, upper := CalcLowerUpper(offset, first, len(objects))
	paged := objects[lower:upper]
	messagingProjects := make([]*consolegraphql.MessagingProjectHolder, len(paged))
	for i, _ := range paged {
		messagingProjects[i] = paged[i].(*consolegraphql.MessagingProjectHolder)
	}

	return &MessagingProjectQueryResultConsoleapiEnmasseIoV1{
		Total:             len(objects),
		MessagingProjects: messagingProjects,
	}, nil
}

type messagingProjectSpecK8sResolver struct{ *Resolver }

/*
func (r *messagingProjectSpecK8sResolver) Plan(ctx context.Context, obj *v1.MessagingProjectSpec) (*v1beta2.MessagingProjectPlan, error) {
	if obj != nil {
		messagingProjectPlan := obj.Plan
		spaceFilter := func(obj interface{}) (bool, bool, error) {
			asp, ok := obj.(*v1beta2.MessagingProjectPlan)
			if !ok {
				return false, false, fmt.Errorf("unexpected type: %T", obj)
			}
			if asp.Name == messagingProjectPlan {
				return true, false, nil
			} else {
				return false, true, nil
			}
		}
		objs, e := r.Cache.Get(cache.PrimaryObjectIndex, "MessagingProjectPlan", spaceFilter)
		if e != nil {
			return nil, e
		}

		if len(objs) == 0 {
			// There might be a plan change in progress, or the user may have created a space referring to
			// an unknown plan.
			return &v1beta2.MessagingProjectPlan{
				ObjectMeta: metav1.ObjectMeta{
					Name: messagingProjectPlan,
				},
				Spec: v1beta2.MessagingProjectPlanSpec{
					AddressPlans:         make([]string, 0),
					MessagingProjectType: obj.Type,
					DisplayName:          messagingProjectPlan,
				},
			}, nil
		}

		asp := objs[0].(*v1beta2.MessagingProjectPlan)
		return asp, nil
	}
	return nil, nil
}
*/

type messagingProjectK8sResolver struct{ *Resolver }

func (r *messagingProjectK8sResolver) Connections(ctx context.Context, obj *consolegraphql.MessagingProjectHolder, first *int, offset *int, filter *string, orderBy *string) (*ConnectionQueryResultConsoleapiEnmasseIoV1, error) {
	if obj != nil {
		fltrfunc, keyElements, e := BuildFilter(filter, "$.spec.messagingProject", "$.metadata.name")
		if e != nil {
			return nil, e
		}

		orderer, e := BuildOrderer(orderBy)
		if e != nil {
			return nil, e
		}

		key := fmt.Sprintf("Connection/%s/%s/%s", obj.Namespace, obj.Name, keyElements)
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

		return &ConnectionQueryResultConsoleapiEnmasseIoV1{
			Total:       len(objects),
			Connections: cons,
		}, nil
	}
	return nil, nil
}

func (r *messagingProjectK8sResolver) Addresses(ctx context.Context, obj *consolegraphql.MessagingProjectHolder, first *int, offset *int, filter *string, orderBy *string) (*AddressQueryResultConsoleapiEnmasseIoV1, error) {
	if obj != nil {
		requestState := server.GetRequestStateFromContext(ctx)
		viewFilter := requestState.AccessController.ViewFilter()

		fltrfunc, keyElements, e := BuildFilter(filter, "$.metadata.name")
		if e != nil {
			return nil, e
		}

		orderer, e := BuildOrderer(orderBy)
		if e != nil {
			return nil, e
		}

		var key string
		if keyElements == "" {
			key = fmt.Sprintf("Address/%s/%s", obj.Namespace, obj.Name)
		} else {
			key = fmt.Sprintf("Address/%s/%s", obj.Namespace, keyElements)
		}
		objects, e := r.Cache.Get(cache.PrimaryObjectIndex, key, cache.And(viewFilter, fltrfunc))
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

		aqr := &AddressQueryResultConsoleapiEnmasseIoV1{
			Total:     len(objects),
			Addresses: addresses,
		}

		return aqr, nil
	}
	return nil, nil
}

func (r *queryResolver) MessagingCertificateChain(ctx context.Context, input metav1.ObjectMeta) (string, error) {

	objects, e := r.Cache.Get(cache.PrimaryObjectIndex, "MessagingProject/"+input.Namespace+"/"+input.Name, nil)
	if e != nil {
		return "", e
	}
	if len(objects) != 1 {
		return "", fmt.Errorf("Did not return one address space for %s %s.  Instead found: %d", input.Name, input.Namespace, len(objects))
	}
	return string(objects[0].(*consolegraphql.MessagingProjectHolder).MessagingProject.Status.CACertificate), nil

}

func (r *mutationResolver) CreateMessagingProject(ctx context.Context, input v1.MessagingProject) (*metav1.ObjectMeta, error) {
	requestState := server.GetRequestStateFromContext(ctx)

	nw, e := requestState.EnmasseV1Client.MessagingProjects(input.Namespace).Create(&input)
	if e != nil {
		return nil, e
	}
	return &nw.ObjectMeta, e
}

func (r *mutationResolver) PatchMessagingProject(ctx context.Context, input metav1.ObjectMeta, patch string, patchType string) (*bool, error) {
	pt := types.PatchType(patchType)
	requestState := server.GetRequestStateFromContext(ctx)

	_, e := requestState.EnmasseV1Client.MessagingProjects(input.Namespace).Patch(input.Name, pt, []byte(patch))
	b := e == nil
	return &b, e
}

func (r *mutationResolver) DeleteMessagingProject(ctx context.Context, input metav1.ObjectMeta) (*bool, error) {
	return r.DeleteMessagingProjects(ctx, []*metav1.ObjectMeta{&input})
}

func (r *mutationResolver) DeleteMessagingProjects(ctx context.Context, input []*metav1.ObjectMeta) (*bool, error) {
	requestState := server.GetRequestStateFromContext(ctx)
	t := true

	for _, as := range input {
		e := requestState.EnmasseV1Client.MessagingProjects(as.Namespace).Delete(as.Name, &metav1.DeleteOptions{})
		if e != nil {
			graphql.AddErrorf(ctx, "failed to delete address space: '%s' in namespace: '%s', %+v", as.Name, as.Namespace, e)
		}
	}
	return &t, nil
}

func (r *queryResolver) MessagingProjectCommand(ctx context.Context, input v1.MessagingProject) (string, error) {

	if input.TypeMeta.APIVersion == "" {
		input.TypeMeta.APIVersion = "enmasse.io/v1"
	}
	if input.TypeMeta.Kind == "" {
		input.TypeMeta.Kind = "MessagingProject"
	}

	namespace := input.Namespace
	input.Namespace = ""

	return generateApplyCommand(input, namespace)
}
