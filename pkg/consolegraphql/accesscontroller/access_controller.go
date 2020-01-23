/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package accesscontroller

import (
	"encoding/gob"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	authv1 "k8s.io/api/authorization/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/meta"
	"k8s.io/client-go/kubernetes"
)

// Access controllers make decisions concerning access to objects.  An instance must not be shared between threads.
type AccessController interface {
	CanRead(obj interface{}) (bool, error)
	ViewFilter() cache.ObjectFilter
	GetState() (bool, interface{})
}

type AccessControlledResource interface {
	GetControllingResourceAttributes() *authv1.ResourceAttributes
}

type allowAll struct {
}

func NewAllowAllAccessController() AccessController {
	return &allowAll{}
}

func (a *allowAll) CanRead(obj interface{}) (bool, error) {
	return true, nil
}

func (a *allowAll) ViewFilter() cache.ObjectFilter {
	return func(obj interface{}) (bool, bool, error) {
		return true, true, nil
	}
}

func (a *allowAll) GetState() (bool, interface{}) {
	return false, nil
}

type kubernetesRbac struct {
	clientSet    kubernetes.Interface
	cached       map[authv1.ResourceAttributes]bool
	cacheUpdated bool
}

// Creates a RBAC-based access controller with the given authentication state.  The authentication state may be nil
// or that returned by the GetState method of a previous instance.
func NewKubernetesRBACAccessController(clientSet kubernetes.Interface, existingState interface{}) AccessController {
	gob.Register(map[authv1.ResourceAttributes]bool{})
	state := make(map[authv1.ResourceAttributes]bool, 0)
	if existingState != nil {
		if s, ok := existingState.(map[authv1.ResourceAttributes]bool); ok {
			for k, v := range s {
				state[k] = v
			}
		}
	}
	return &kubernetesRbac{
		clientSet: clientSet,
		cached:    state,
	}
}

func (k *kubernetesRbac) CanRead(obj interface{}) (bool, error) {
	switch o := obj.(type) {
	case AccessControlledResource:
		attributes := o.GetControllingResourceAttributes()
		if attributes == nil {
			return false, nil
		}

		resourceAttributes := attributes.DeepCopy()
		resourceAttributes.Verb = "get"
		return k.makeAccessDecision(resourceAttributes)
	case *corev1.Namespace:
		gvk := o.TypeMeta.GroupVersionKind()
		plural, _ := meta.UnsafeGuessKindToResource(gvk)

		attributes := &authv1.ResourceAttributes{
			Verb:      "get",
			Resource:  plural.Resource, // Resource is Kind
			Version:   gvk.Version,
			Namespace: o.Name,
		}
		return k.makeAccessDecision(attributes)
	default:
		return true, nil
	}
}

func (k *kubernetesRbac) ViewFilter() cache.ObjectFilter {
	return FilterAdapter(k)
}

func (k *kubernetesRbac) GetState() (bool, interface{}) {
	return k.cacheUpdated, k.cached
}

func (k *kubernetesRbac) makeAccessDecision(resourceAttributes *authv1.ResourceAttributes) (bool, error) {
	if decision, present := k.cached[*resourceAttributes]; present {
		return decision, nil
	}

	ssar := &authv1.SelfSubjectAccessReview{
		Spec: authv1.SelfSubjectAccessReviewSpec{
			ResourceAttributes: resourceAttributes,
		},
	}

	resp, err := k.clientSet.AuthorizationV1().SelfSubjectAccessReviews().Create(ssar)
	if err != nil {
		return false, err
	} else {
		k.cached[*resourceAttributes] = resp.Status.Allowed
		k.cacheUpdated = true
		return resp.Status.Allowed, nil
	}
}

func FilterAdapter(controller AccessController) cache.ObjectFilter {
	return func(obj interface{}) (bool, bool, error) {
		readable, err := controller.CanRead(obj)
		return readable, true, err
	}
}
