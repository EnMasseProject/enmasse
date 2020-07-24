/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"context"
	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/stretchr/testify/assert"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"testing"
)

func TestEnabled(t *testing.T) {

	s := runtime.NewScheme()

	if err := appsv1.AddToScheme(s); err != nil {
		assert.NoError(t, err, "Expect to add to scheme")
		return
	}
	if err := iotv1.AddToScheme(s); err != nil {
		assert.NoError(t, err, "Expect to add to scheme")
		return
	}

	TRUE := true
	FALSE := false

	infra := &iotv1.IoTInfrastructure{
		Spec: iotv1.IoTInfrastructureSpec{
			AdaptersConfig: iotv1.AdaptersConfig{
				AmqpAdapterConfig: iotv1.AmqpAdapterConfig{
					CommonAdapterConfig: iotv1.CommonAdapterConfig{
						AdapterConfig: iotv1.AdapterConfig{
							Enabled: &TRUE,
						},
					},
				},
				MqttAdapterConfig: iotv1.MqttAdapterConfig{
					CommonAdapterConfig: iotv1.CommonAdapterConfig{
						AdapterConfig: iotv1.AdapterConfig{
							Enabled: &FALSE,
						},
					},
				},
				HttpAdapterConfig: iotv1.HttpAdapterConfig{
					CommonAdapterConfig: iotv1.CommonAdapterConfig{
						AdapterConfig: iotv1.AdapterConfig{
							Enabled: &FALSE,
						},
					},
				},
				LoraWanAdapterConfig: iotv1.LoraWanAdapterConfig{
					CommonAdapterConfig: iotv1.CommonAdapterConfig{
						AdapterConfig: iotv1.AdapterConfig{
							Enabled: &FALSE,
						},
					},
				},
				SigfoxAdapterConfig: iotv1.SigfoxAdapterConfig{
					CommonAdapterConfig: iotv1.CommonAdapterConfig{
						AdapterConfig: iotv1.AdapterConfig{
							Enabled: &FALSE,
						},
					},
				},
			},
		},
	}

	client := fake.NewFakeClientWithScheme(s, infra)

	a := newAggregator(infra)
	phase, message := a.Aggregate(context.Background(), client, infra, nil)

	// overall result

	assert.Equal(t, iotv1.InfrastructurePhaseConfiguring, phase)
	assert.Equal(t, "7 components out of 11 are non-ready: CommandMeshReady, AuthServiceReady, TenantServiceReady, DeviceConnectionServiceReady, DeviceRegistryAdapterServiceReady, DeviceRegistryManagementServiceReady, AmqpAdapterReady", message)

	// infra condition

	recon := infra.Status.GetInfrastructureCondition(iotv1.InfrastructureConditionTypeReconciled)
	assert.Equal(t, corev1.ConditionTrue, recon.Status)
	assert.Equal(t, "AsExpected", recon.Reason)
	assert.Equal(t, "", recon.Message)

}
