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

	if deviceRegistryImplementation(&v1alpha1.IoTConfig{}) != DeviceRegistryIllegal {
		t.Errorf("Device registry should be evaluated as 'illegal' but wasn't")
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
		t.Errorf("Device registry should be evaluated as 'file based' but wasn't")
	}

	if deviceRegistryImplementation(&v1alpha1.IoTConfig{
		Spec: v1alpha1.IoTConfigSpec{
			ServicesConfig: v1alpha1.ServicesConfig{
				DeviceRegistry: v1alpha1.DeviceRegistryServiceConfig{
					Infinispan: &v1alpha1.InfinispanDeviceRegistry{
						ServerAddress: "127.0.0.1",
					},
				},
			},
		},
	}) != DeviceRegistryInfinispan {
		t.Errorf("Device registry should be evaluated as 'infinispan' but wasn't")
	}

}
