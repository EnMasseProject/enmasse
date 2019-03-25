/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"testing"

	"github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

func TestDeviceRegistryEval(t *testing.T) {

	if deviceRegistryImplementation(&v1alpha1.IoTConfig{}) != DeviceRegistryDefault {
		t.Errorf("Device registry should be evaluated as 'default' but wasn't")
	}

	if deviceRegistryImplementation(&v1alpha1.IoTConfig{
		Spec: v1alpha1.IoTConfigSpec{
			ServicesConfig: v1alpha1.ServicesConfig{
				DeviceRegistry: v1alpha1.DeviceRegistryServiceConfig{
					File: &v1alpha1.FileBasedDeviceRegistry{},
				},
			},
		},
	}) != DeviceRegistryFileBased {
		t.Errorf("Device registry should be evaluated as 'default' but wasn't")
	}

}
