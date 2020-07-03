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

	appsv1 "k8s.io/api/apps/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/scheme"

	// "sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
)

func setup(t *testing.T) *BrokerController {
	s := scheme.Scheme
	s.AddKnownTypes(v1.SchemeGroupVersion, &v1.MessagingInfrastructure{})
	cl := fake.NewFakeClientWithScheme(s)
	certController := cert.NewCertController(cl, s, 1*time.Hour, 1*time.Hour)
	return NewBrokerController(cl, s, certController)
}

func TestReconcileBrokerPool(t *testing.T) {
	bc := setup(t)

	infra := v1.MessagingInfrastructure{
		ObjectMeta: metav1.ObjectMeta{Name: "infra1", Namespace: "test"},
		Spec: v1.MessagingInfrastructureSpec{
			Broker: v1.MessagingInfrastructureSpecBroker{
				ScalingStrategy: &v1.MessagingInfrastructureSpecBrokerScalingStrategy{
					Static: &v1.MessagingInfrastructureSpecBrokerScalingStrategyStatic{
						PoolSize: 2,
					},
				},
			},
		},
	}

	_, err := bc.certController.ReconcileCa(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)

	hosts, err := bc.ReconcileBrokers(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)

	setList := &appsv1.StatefulSetList{}
	err = bc.client.List(context.TODO(), setList, client.InNamespace("test"))
	assert.Nil(t, err)
	assert.Equal(t, 2, len(setList.Items))
	assert.Equal(t, 2, len(hosts))

	// Scale up
	infra.Spec.Broker.ScalingStrategy.Static.PoolSize = 3
	hosts, err = bc.ReconcileBrokers(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)
	err = bc.client.List(context.TODO(), setList, client.InNamespace("test"))
	assert.Nil(t, err)
	assert.Equal(t, 3, len(setList.Items))
	assert.Equal(t, 3, len(hosts))

	// Scale down
	infra.Spec.Broker.ScalingStrategy.Static.PoolSize = 1
	hosts, err = bc.ReconcileBrokers(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)
	err = bc.client.List(context.TODO(), setList, client.InNamespace("test"))
	assert.Nil(t, err)
	assert.Equal(t, 1, len(setList.Items))
	assert.Equal(t, 1, len(hosts))
}
