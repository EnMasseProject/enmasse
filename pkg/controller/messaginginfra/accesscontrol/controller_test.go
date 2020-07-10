/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package accesscontrol

import (
	"context"
	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	logrtesting "github.com/go-logr/logr/testing"
	"github.com/stretchr/testify/assert"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/scheme"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"testing"
	"time"
)

func setup(t *testing.T) *AccessControlController {
	s := scheme.Scheme
	s.AddKnownTypes(v1.SchemeGroupVersion, &v1.MessagingInfrastructure{})
	cl := fake.NewFakeClientWithScheme(s)
	certController := cert.NewCertController(cl, s, 1*time.Hour, 1*time.Hour)
	return NewAccessControlController(cl, s, certController)
}

func TestReconcileAccessController(t *testing.T) {
	bc := setup(t)

	two := int32(2)
	infra := v1.MessagingInfrastructure{
		ObjectMeta: metav1.ObjectMeta{Name: "infra1", Namespace: "test"},
		Spec: v1.MessagingInfrastructureSpec{
			AccessControl: v1.MessagingInfrastructureSpecAccessControl{
				Replicas: &two,
			},
		},
	}

	_, err := bc.certController.ReconcileCa(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)

	_, err = bc.ReconcileAccessControl(context.TODO(), logrtesting.TestLogger{}, &infra)
	assert.Nil(t, err)

	deployments := &appsv1.DeploymentList{}
	err = bc.client.List(context.TODO(), deployments, client.InNamespace("test"))
	assert.Nil(t, err)
	assert.Equal(t, 1, len(deployments.Items))
	assert.Equal(t, "access-control-infra1", deployments.Items[0].Name)
	assert.Equal(t, two, *deployments.Items[0].Spec.Replicas)

	services := &corev1.ServiceList{}
	err = bc.client.List(context.TODO(), services, client.InNamespace("test"))
	assert.Nil(t, err)
	assert.Equal(t, 1, len(services.Items))
	assert.Equal(t, "access-control-infra1", services.Items[0].Name)

	secrets := &corev1.SecretList{}
	err = bc.client.List(context.TODO(), secrets, client.InNamespace("test"))
	assert.Nil(t, err)
	assert.Equal(t, 2, len(secrets.Items))
	names := make([]string, 0)
	for _, item := range secrets.Items {
		names = append(names, item.Name)
	}
	assert.Contains(t, names, "infra1-ca")
	assert.Contains(t, names, "access-control-infra1-cert")
}
