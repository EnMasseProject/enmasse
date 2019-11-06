/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package authenticationservice

import (
	"context"
	"os"
	"testing"

	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"

	"fmt"

	appsv1 "k8s.io/api/apps/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/kubernetes/scheme"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

func setup(t *testing.T, authservice *adminv1beta1.AuthenticationService) *ReconcileAuthenticationService {
	s := scheme.Scheme
	s.AddKnownTypes(adminv1beta1.SchemeGroupVersion, authservice)
	objs := []runtime.Object{
		authservice,
	}
	cl := fake.NewFakeClientWithScheme(s, objs...)
	r := &ReconcileAuthenticationService{client: cl, scheme: s}
	return r
}

func TestNoneAuthService(t *testing.T) {
	authservice := &adminv1beta1.AuthenticationService{
		ObjectMeta: metav1.ObjectMeta{Namespace: "infra", Name: "none"},
		Spec: adminv1beta1.AuthenticationServiceSpec{
			Type: adminv1beta1.None,
		},
	}

	r := setup(t, authservice)

	if err := os.Setenv("NONE_AUTHSERVICE_IMAGE", "none-authservice"); err != nil {
		t.Fatalf("Failed to set image version for test: %v", err)
	}

	req := reconcile.Request{
		NamespacedName: types.NamespacedName{
			Name:      authservice.Name,
			Namespace: authservice.Namespace,
		},
	}
	_, err := r.Reconcile(req)

	if err != nil {
		t.Fatalf("reconcile: (%v)", err)
	}

	dep := &appsv1.Deployment{}
	err = r.client.Get(context.TODO(), req.NamespacedName, dep)
	if err != nil {
		t.Fatalf("get deployment: (%v)", err)
	}

	if dep.Labels["name"] != "none" {
		t.Error("wrong label 'name'")
	}

	if dep.Labels["component"] != "none-authservice" {
		t.Error("wrong label 'component': " + dep.Labels["component"])
	}

	if dep.Spec.Template.Spec.Volumes[0].Name != "none-authservice-cert" {
		t.Error("deployment volume for cert not set")
	}

	if dep.Spec.Template.Spec.Containers[0].Name != "none-authservice" {
		t.Error("deployment container not set")
	}

	if dep.Spec.Selector == nil {
		t.Error("null label selector")
	}

	fmt.Printf("") //%#v", dep)

	_ = r.client.Create(context.TODO(), dep)
}
