/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messaginguser

import (
	"context"
	"testing"

	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
	keycloak "github.com/enmasseproject/enmasse/pkg/keycloak"

	"github.com/stretchr/testify/assert"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/kubernetes/scheme"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

func setup(t *testing.T, authservice *adminv1beta1.AuthenticationService, addressSpace *enmassev1beta1.AddressSpace, user *userv1beta1.MessagingUser) *ReconcileMessagingUser {
	s := scheme.Scheme
	s.AddKnownTypes(adminv1beta1.SchemeGroupVersion, authservice)
	s.AddKnownTypes(adminv1beta1.SchemeGroupVersion, addressSpace)
	s.AddKnownTypes(adminv1beta1.SchemeGroupVersion, user)
	objs := []runtime.Object{
		authservice,
		addressSpace,
		user,
		&corev1.Secret{
			ObjectMeta: metav1.ObjectMeta{
				Name:      "creds",
				Namespace: "test",
			},
			Data: map[string][]byte{
				"admin.username": []byte("admin"),
				"admin.password": []byte("admin"),
			},
		},
	}
	cl := fake.NewFakeClientWithScheme(s, objs...)
	r := &ReconcileMessagingUser{
		client: cl,
		reader: cl,
		newKeycloakClientFunc: func(host string, port int, user string, password string, cert []byte) (keycloak.KeycloakClient, error) {
			return &keycloak.FakeClient{
				Users: map[string][]*userv1beta1.MessagingUser{
					"realm1": make([]*userv1beta1.MessagingUser, 0),
				},
			}, nil

		},
		keycloakClients: make(map[string]keycloak.KeycloakClient),
		namespace:       "test",

		scheme: s,
	}

	return r
}

func TestReconcile(t *testing.T) {

	authservice := &adminv1beta1.AuthenticationService{
		ObjectMeta: metav1.ObjectMeta{Namespace: "test", Name: "standard"},
		Spec: adminv1beta1.AuthenticationServiceSpec{
			Type: adminv1beta1.Standard,
			Standard: &adminv1beta1.AuthenticationServiceSpecStandard{
				CredentialsSecret: &corev1.SecretReference{
					Name: "creds",
				},
			},
		},
		Status: adminv1beta1.AuthenticationServiceStatus{
			Host: "example.com",
			Port: 5671,
		},
	}

	addressSpace := &enmassev1beta1.AddressSpace{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "ns",
			Name:      "myspace",
			Annotations: map[string]string{
				"enmasse.io/realm-name": "realm1",
			},
		},
		Spec: enmassev1beta1.AddressSpaceSpec{
			Type: "standard",
			Plan: "standard-small",
			AuthenticationService: &enmassev1beta1.AuthenticationService{
				Name: "standard",
			},
		},
	}

	user := &userv1beta1.MessagingUser{
		ObjectMeta: metav1.ObjectMeta{Namespace: "ns", Name: "myspace.test"},
		Spec: userv1beta1.MessagingUserSpec{
			Username: "test",
			Authentication: userv1beta1.AuthenticationSpec{
				Type:     userv1beta1.Password,
				Password: []byte("secret!"),
			},
			Authorization: []userv1beta1.AuthorizationSpec{
				userv1beta1.AuthorizationSpec{
					Operations: []userv1beta1.AuthorizationOperation{
						userv1beta1.Send,
						userv1beta1.Recv,
						userv1beta1.Manage,
					},
					Addresses: []string{
						"*",
					},
				},
			},
		},
	}

	r := setup(t, authservice, addressSpace, user)

	userType := types.NamespacedName{
		Name:      user.Name,
		Namespace: user.Namespace,
	}
	req := reconcile.Request{
		NamespacedName: userType,
	}

	// First iteration should add the finalizer
	result, err := r.Reconcile(req)
	assert.Nil(t, err, "Unexpected reconcile error")
	assert.True(t, result.Requeue)

	// Refetch
	err = r.client.Get(context.TODO(), userType, user)
	assert.Nil(t, err)
	assert.Contains(t, user.ObjectMeta.Finalizers, FINALIZER_NAME)

	result, err = r.Reconcile(req)
	assert.Nil(t, err, "Unexpected reconcile error")
	assert.False(t, result.Requeue)

	// Refetch
	err = r.client.Get(context.TODO(), userType, user)
	assert.Nil(t, err)

	client, ok := r.keycloakClients["standard"]
	if assert.True(t, ok, "Unable to find expected keycloak client") {

		usermap := client.(*keycloak.FakeClient).Users
		userlist, ok := usermap["realm1"]

		if assert.True(t, ok, "Unable to find expected realm in fake client") {
			assert.Equal(t, 1, len(userlist), "Unexpected length of user list")
			assert.Equal(t, user, userlist[0], "Stored used does not equal reconciled user")
		}
	}
}
