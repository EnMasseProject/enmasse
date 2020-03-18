/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"testing"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

func TestDeviceRegistryEval(t *testing.T) {

	if (iotv1alpha1.IoTConfig{}).EvalDeviceRegistryImplementation() != iotv1alpha1.DeviceRegistryIllegal {
		t.Errorf("Device registry should be evaluated as 'illegal' but wasn't")
	}

	if (iotv1alpha1.IoTConfig{
		Spec: iotv1alpha1.IoTConfigSpec{
			ServicesConfig: iotv1alpha1.ServicesConfig{
				DeviceRegistry: iotv1alpha1.DeviceRegistryServiceConfig{
					JDBC: &iotv1alpha1.JdbcDeviceRegistry{
						Server: iotv1alpha1.JdbcRegistryServer{
							External: &iotv1alpha1.ExternalJdbcRegistryServer{
								Management: &iotv1alpha1.ExternalJdbcRegistryService{},
							},
						},
					},
				},
			},
		},
	}).EvalDeviceRegistryImplementation() != iotv1alpha1.DeviceRegistryJdbc {
		t.Errorf("Device registry should be evaluated as 'jdbc' but wasn't")
	}

	if (iotv1alpha1.IoTConfig{
		Spec: iotv1alpha1.IoTConfigSpec{
			ServicesConfig: iotv1alpha1.ServicesConfig{
				DeviceRegistry: iotv1alpha1.DeviceRegistryServiceConfig{
					Infinispan: &iotv1alpha1.InfinispanDeviceRegistry{
						Server: iotv1alpha1.InfinispanRegistryServer{
							External: &iotv1alpha1.ExternalInfinispanRegistryServer{
								ExternalInfinispanServer: iotv1alpha1.ExternalInfinispanServer{
									Host: "127.0.0.1",
								},
							},
						},
					},
				},
			},
		},
	}).EvalDeviceRegistryImplementation() != iotv1alpha1.DeviceRegistryInfinispan {
		t.Errorf("Device registry should be evaluated as 'infinispan' but wasn't")
	}

}
