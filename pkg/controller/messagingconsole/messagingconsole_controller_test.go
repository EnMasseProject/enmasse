/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messagingconsole

import (
	"fmt"
	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/kubernetes/scheme"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"testing"
)

func setup(t *testing.T, consoleservice *v1.MessagingConsole) *ReconcileMessagingConsole {
	s := scheme.Scheme
	s.AddKnownTypes(v1.SchemeGroupVersion, consoleservice)
	objs := []runtime.Object{
		consoleservice,
	}
	cl := fake.NewFakeClient(objs...)
	r := &ReconcileMessagingConsole{client: cl, scheme: s}
	return r
}

func TestMessagingConsole(t *testing.T) {
	//consoleservice := &v1.MessagingConsole{
	//	ObjectMeta: metav1.ObjectMeta{Namespace: "infra", Name: "myconsole"},
	//	Spec: v1.MessagingConsoleSpec{
	//		CertificateSecret: &corev1.SecretReference{Name: "consoleserver-cert"},
	//	},
	//}
	//
	//r := setup(t, consoleservice)
	//
	//routeSecret := corev1.Secret{
	//	ObjectMeta: metav1.ObjectMeta{Namespace: "infra", Name: "consoleserver-cert"},
	//}
	//
	//err := r.client.Create(context.TODO(), &routeSecret)
	//if err != nil {
	//	t.Fatalf("route secret: (%v)", err)
	//}
	//
	//req := reconcile.Request{
	//	NamespacedName: types.NamespacedName{
	//		Name:      consoleservice.Name,
	//		Namespace: consoleservice.Namespace,
	//	},
	//}
	//_, err = r.Reconcile(req)

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
