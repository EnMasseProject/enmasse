/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package messaginguser

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

func setup(t *testing.T, obj ...runtime.Object) ReconcileMessagingUser {
	scheme := createFakeScheme(t)
	client := fake.NewFakeClientWithScheme(scheme, obj...)

	return ReconcileMessagingUser{
		client: client,
		scheme: scheme,
		reader: client,
	}
}

func TestReconcileMessagingUser(t *testing.T) {

	reconciler := setup(t, &v1.MessagingUser{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "myuser",
			Namespace: "myns",
		},
		Spec: v1.MessagingUserSpec{
			Password: "foo",
		},
		Status: v1.MessagingUserStatus{},
	})
	reconciler.Reconcile(reconcile.Request{})

	user := &v1.MessagingUser{}
	err := reconciler.client.Get(context.TODO(), client.ObjectKey{
		Namespace: "myns",
		Name:      "myuser",
	}, user)
	assert.NoError(t, err)
	assert.Equal(t, v1.MessagingUserPhaseActive, user.Status.Phase)

}

func createFakeScheme(t *testing.T) *runtime.Scheme {
	s := scheme.Scheme
	s.AddKnownTypes(v1.SchemeGroupVersion, &v1.MessagingUser{})

	assert.NotNil(t, s)
	return s
}
