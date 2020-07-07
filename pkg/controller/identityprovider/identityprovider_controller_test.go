/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package identityprovider

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/scheme"
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"testing"

	"sigs.k8s.io/controller-runtime/pkg/client/fake"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
)



func setup(t *testing.T, obj ...runtime.Object) ReconcileIdentityProvider {
	scheme := createFakeScheme(t)
	client := fake.NewFakeClientWithScheme(scheme, obj...)

	return ReconcileIdentityProvider{
		client: client,
		scheme: scheme,
		reader: client,
	}
}

func TestReconcileAnonymousProvider(t *testing.T) {

	reconciler := setup(t, &v1.IdentityProvider{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "myip",
			Namespace: "myns",
		},
		Spec: v1.IdentityProviderSpec{
			AnonymousProvider: &v1.IdentityProviderSpecAnonymousProvider{},
		},
		Status: v1.IdentityProviderStatus{},
	})
	reconciler.Reconcile(reconcile.Request{})

	idp := &v1.IdentityProvider{}
	err := reconciler.client.Get(context.TODO(), client.ObjectKey{
		Namespace: "myns",
		Name: "myip",
	}, idp)
	assert.NoError(t, err)
	assert.Equal(t, v1.IdentityProviderTypeAnonymous, idp.Status.Type)
	assert.Equal(t, v1.IdentityProviderPhaseActive, idp.Status.Phase)

}

func TestReconcileNamespaceProvider(t *testing.T) {

	reconciler := setup(t, &v1.IdentityProvider{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "myip",
			Namespace: "myns",
		},
		Spec: v1.IdentityProviderSpec{
			NamespaceProvider: &v1.IdentityProviderSpecNamespaceProvider{},
		},
		Status: v1.IdentityProviderStatus{},
	})
	reconciler.Reconcile(reconcile.Request{})

	idp := &v1.IdentityProvider{}
	err := reconciler.client.Get(context.TODO(), client.ObjectKey{
		Namespace: "myns",
		Name: "myip",
	}, idp)
	assert.NoError(t, err)
	assert.Equal(t, v1.IdentityProviderTypeNamespace, idp.Status.Type)
	assert.Equal(t, v1.IdentityProviderPhaseActive, idp.Status.Phase)

}

func createFakeScheme(t *testing.T) *runtime.Scheme {
	s := scheme.Scheme
	s.AddKnownTypes(v1.SchemeGroupVersion, &v1.IdentityProvider{})

	assert.NotNil(t, s)
	return s
}