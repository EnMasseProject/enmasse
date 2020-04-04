/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package broker

import (
	//"fmt"
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	logrtesting "github.com/go-logr/logr/testing"

	// corev1 "k8s.io/api/core/v1"
	appsv1 "k8s.io/api/apps/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/scheme"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
)

func setup(t *testing.T) *BrokerController {
	s := scheme.Scheme
	s.AddKnownTypes(v1beta2.SchemeGroupVersion, &v1beta2.MessagingInfra{})
	cl := fake.NewFakeClientWithScheme(s)
	certController := cert.NewCertController(cl, s, 1*time.Hour, 1*time.Hour)
	return NewBrokerController(cl, s, certController)
}

func TestReconcileBrokerPool(t *testing.T) {
	bc := setup(t)

	infra := v1beta2.MessagingInfra{
		ObjectMeta: metav1.ObjectMeta{Name: "infra1", Namespace: "test"},
		Spec: v1beta2.MessagingInfraSpec{
			Broker: v1beta2.MessagingInfraSpecBroker{
				ScalingStrategy: &v1beta2.MessagingInfraSpecBrokerScalingStrategy{
					Static: &v1beta2.MessagingInfraSpecBrokerScalingStrategyStatic{
						PoolSize: 2,
					},
				},
			},
		},
	}

	err := bc.certController.ReconcileCa(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)

	err = bc.ReconcileBrokers(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)

	setList := &appsv1.StatefulSetList{}
	err = bc.client.List(context.TODO(), setList, client.InNamespace("test"))
	assert.Nil(t, err)
	assert.Equal(t, 2, len(setList.Items))

	// Scale up
	infra.Spec.Broker.ScalingStrategy.Static.PoolSize = 3
	err = bc.ReconcileBrokers(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)
	err = bc.client.List(context.TODO(), setList, client.InNamespace("test"))
	assert.Nil(t, err)
	assert.Equal(t, 3, len(setList.Items))

	// Scale down
	infra.Spec.Broker.ScalingStrategy.Static.PoolSize = 1
	err = bc.ReconcileBrokers(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)
	err = bc.client.List(context.TODO(), setList, client.InNamespace("test"))
	assert.Nil(t, err)
	assert.Equal(t, 1, len(setList.Items))
}
