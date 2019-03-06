/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
*/
package consoleservice

import (
	"context"
	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"testing"

	"fmt"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/kubernetes/scheme"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

func setup(t *testing.T, consoleservice *adminv1beta1.ConsoleService) *ReconcileConsoleService {
	s := scheme.Scheme
	s.AddKnownTypes(adminv1beta1.SchemeGroupVersion, consoleservice)
	objs := []runtime.Object{
		consoleservice,
	}
	cl := fake.NewFakeClient(objs...)
	r := &ReconcileConsoleService{client: cl, scheme: s}
	return r
}

func TestConsoleService(t *testing.T) {
	consoleservice := &adminv1beta1.ConsoleService{
		ObjectMeta: metav1.ObjectMeta{Namespace: "infra", Name: "myconsole"},
		Spec: adminv1beta1.ConsoleServiceSpec{
			CertificateSecret: &corev1.SecretReference{Name: "consoleserver-cert"},
		},
	}

	r := setup(t, consoleservice)

	routeSecret := corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: "infra", Name: "consoleserver-cert"},
	}

	err := r.client.Create(context.TODO(), &routeSecret)
	if err != nil {
		t.Fatalf("route secret: (%v)", err)
	}

	req := reconcile.Request{
		NamespacedName: types.NamespacedName{
			Name:      consoleservice.Name,
			Namespace: consoleservice.Namespace,
		},
	}
	_, err = r.Reconcile(req)

	//if err != nil {
	//	t.Fatalf("reconcile: (%v)", err)
	//}
	//
	//service := &corev1.Service{}
	//err = r.client.Get(context.TODO(), client.ObjectKey{
	//	Name: *consoleservice.Spec.ServiceName,
	//	Namespace: consoleservice.Namespace,
	//}, service)
	//if err != nil {
	//	t.Fatalf("get service: (%v)", err)
	//}

	//route := &routev1.Route{}
	//err = r.client.Get(context.TODO(), client.ObjectKey{
	//	Name: consoleservice.Spec.RouteName,
	//	Namespace: consoleservice.Namespace,
	//}, route)
	//if err != nil {
	//	t.Fatalf("get route: (%v)", err)
	//}

	//dep := &appsv1.Deployment{}
	//err = r.client.Get(context.TODO(), req.NamespacedName, dep)
	//if err != nil {
	//	t.Fatalf("get deployment: (%v)", err)
	//}


	//if dep.Labels["name"] != "consoleservice" {
	//	t.Error("wrong label 'name'")
	//}
	//
	//if dep.Labels["component"] != "consoleservice" {
	//	t.Error("wrong label 'component': " + dep.Labels["component"])
	//}

	//if dep.Spec.Template.Spec.Volumes[0].Name != "none-authservice-cert" {
	//	t.Error("deployment volume for cert not set")
	//}
	//
	//if dep.Spec.Template.Spec.Containers[0].Name != "none-authservice" {
	//	t.Error("deployment container not set")
	//}
	//
	//if dep.Spec.Selector == nil {
	//	t.Error("null label selector")
	//}

	fmt.Printf("") //%#v", dep)

//	r.client.Create(context.TODO(), dep)  // Why??
}
