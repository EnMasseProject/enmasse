/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package router

import (
	//"fmt"
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	logrtesting "github.com/go-logr/logr/testing"

	appsv1 "k8s.io/api/apps/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"

	// "k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/kubernetes/scheme"

	"sigs.k8s.io/controller-runtime/pkg/client/fake"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/enmasseproject/enmasse/pkg/state"
)

func setup(t *testing.T) *RouterController {
	s := scheme.Scheme
	s.AddKnownTypes(v1beta2.SchemeGroupVersion, &v1beta2.MessagingInfrastructure{})
	cl := fake.NewFakeClientWithScheme(s)
	certController := cert.NewCertController(cl, s, 1*time.Hour, 1*time.Hour)
	return NewRouterController(cl, s, certController)
}

func TestReconcileRouterReplicas(t *testing.T) {
	rc := setup(t)

	infra := v1beta2.MessagingInfrastructure{
		ObjectMeta: metav1.ObjectMeta{Name: "infra1", Namespace: "test"},
		Spec: v1beta2.MessagingInfrastructureSpec{
			Router: v1beta2.MessagingInfrastructureSpecRouter{
				ScalingStrategy: &v1beta2.MessagingInfrastructureSpecRouterScalingStrategy{
					Static: &v1beta2.MessagingInfrastructureSpecRouterScalingStrategyStatic{
						Replicas: 2,
					},
				},
			},
		},
	}

	err := rc.certController.ReconcileCa(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)

	hosts, err := rc.ReconcileRouters(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)

	set := &appsv1.StatefulSet{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: "router-infra1"},
	}
	err = rc.client.Get(context.TODO(), types.NamespacedName{Namespace: set.Namespace, Name: set.Name}, set)
	assert.Nil(t, err)
	assert.Equal(t, int32(2), *set.Spec.Replicas)

	assert.Equal(t, []state.Host{{Hostname: "router-infra1-0.router-infra1.test.svc", Ip: ""}, {Hostname: "router-infra1-1.router-infra1.test.svc", Ip: ""}}, hosts)

	// Scale up
	infra.Spec.Router.ScalingStrategy.Static.Replicas = 3
	hosts, err = rc.ReconcileRouters(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)

	set = &appsv1.StatefulSet{ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: "router-infra1"}}
	err = rc.client.Get(context.TODO(), types.NamespacedName{Namespace: set.Namespace, Name: set.Name}, set)
	assert.Nil(t, err)
	assert.Equal(t, int32(3), *set.Spec.Replicas)
	assert.Equal(t, []state.Host{{Hostname: "router-infra1-0.router-infra1.test.svc", Ip: ""}, {Hostname: "router-infra1-1.router-infra1.test.svc", Ip: ""}, {Hostname: "router-infra1-2.router-infra1.test.svc", Ip: ""}}, hosts)

	// Scale down
	infra.Spec.Router.ScalingStrategy.Static.Replicas = 1
	hosts, err = rc.ReconcileRouters(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)

	set = &appsv1.StatefulSet{ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: "router-infra1"}}
	err = rc.client.Get(context.TODO(), types.NamespacedName{Namespace: set.Namespace, Name: set.Name}, set)
	assert.Nil(t, err)
	assert.Equal(t, int32(1), *set.Spec.Replicas)
	assert.Equal(t, []state.Host{{Hostname: "router-infra1-0.router-infra1.test.svc", Ip: ""}}, hosts)
}
