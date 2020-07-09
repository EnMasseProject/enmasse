/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
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
	if err := iotv1alpha1.AddToScheme(s); err != nil {
		assert.NoError(t, err, "Expect to add to scheme")
		return
	}

	TRUE := true
	FALSE := false

	config := &iotv1alpha1.IoTConfig{
		Spec: iotv1alpha1.IoTConfigSpec{
			AdaptersConfig: iotv1alpha1.AdaptersConfig{
				AmqpAdapterConfig: iotv1alpha1.AmqpAdapterConfig{
					CommonAdapterConfig: iotv1alpha1.CommonAdapterConfig{
						AdapterConfig: iotv1alpha1.AdapterConfig{
							Enabled: &TRUE,
						},
					},
				},
				MqttAdapterConfig: iotv1alpha1.MqttAdapterConfig{
					CommonAdapterConfig: iotv1alpha1.CommonAdapterConfig{
						AdapterConfig: iotv1alpha1.AdapterConfig{
							Enabled: &FALSE,
						},
					},
				},
				HttpAdapterConfig: iotv1alpha1.HttpAdapterConfig{
					CommonAdapterConfig: iotv1alpha1.CommonAdapterConfig{
						AdapterConfig: iotv1alpha1.AdapterConfig{
							Enabled: &FALSE,
						},
					},
				},
				LoraWanAdapterConfig: iotv1alpha1.LoraWanAdapterConfig{
					CommonAdapterConfig: iotv1alpha1.CommonAdapterConfig{
						AdapterConfig: iotv1alpha1.AdapterConfig{
							Enabled: &FALSE,
						},
					},
				},
				SigfoxAdapterConfig: iotv1alpha1.SigfoxAdapterConfig{
					CommonAdapterConfig: iotv1alpha1.CommonAdapterConfig{
						AdapterConfig: iotv1alpha1.AdapterConfig{
							Enabled: &FALSE,
						},
					},
				},
			},
		},
	}

	client := fake.NewFakeClientWithScheme(s, config)

	a := newAggregator(config)
	phase, message := a.Aggregate(context.Background(), client, config, nil)

	// overall result

	assert.Equal(t, iotv1alpha1.ConfigPhaseConfiguring, phase)
	assert.Equal(t, "7 components out of 11 are non-ready: CommandMeshReady, AuthServiceReady, TenantServiceReady, DeviceConnectionServiceReady, DeviceRegistryAdapterServiceReady, DeviceRegistryManagementServiceReady, AmqpAdapterReady", message)

	// config condition

	recon := config.Status.GetConfigCondition(iotv1alpha1.ConfigConditionTypeReconciled)
	assert.Equal(t, corev1.ConditionTrue, recon.Status)
	assert.Equal(t, "AsExpected", recon.Reason)
	assert.Equal(t, "", recon.Message)

}
