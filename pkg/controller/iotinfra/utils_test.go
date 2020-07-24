/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"testing"

	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
)

func TestDeviceRegistryEval(t *testing.T) {

	if (iotv1.IoTInfrastructure{}).EvalDeviceRegistryImplementation() != iotv1.DeviceRegistryIllegal {
		t.Errorf("Device registry should be evaluated as 'illegal' but wasn't")
	}

	if (iotv1.IoTInfrastructure{
		Spec: iotv1.IoTInfrastructureSpec{
			ServicesConfig: iotv1.ServicesConfig{
				DeviceRegistry: iotv1.DeviceRegistryServiceConfig{
					JDBC: &iotv1.JdbcDeviceRegistry{
						Server: iotv1.JdbcRegistryServer{
							External: &iotv1.ExternalJdbcRegistryServer{
								Management: &iotv1.ExternalJdbcRegistryService{},
							},
						},
					},
				},
			},
		},
	}).EvalDeviceRegistryImplementation() != iotv1.DeviceRegistryJdbc {
		t.Errorf("Device registry should be evaluated as 'jdbc' but wasn't")
	}

	if (iotv1.IoTInfrastructure{
		Spec: iotv1.IoTInfrastructureSpec{
			ServicesConfig: iotv1.ServicesConfig{
				DeviceRegistry: iotv1.DeviceRegistryServiceConfig{
					Infinispan: &iotv1.InfinispanDeviceRegistry{
						Server: iotv1.InfinispanRegistryServer{
							External: &iotv1.ExternalInfinispanRegistryServer{
								ExternalInfinispanServer: iotv1.ExternalInfinispanServer{
									Host: "127.0.0.1",
								},
							},
						},
					},
				},
			},
		},
	}).EvalDeviceRegistryImplementation() != iotv1.DeviceRegistryInfinispan {
		t.Errorf("Device registry should be evaluated as 'infinispan' but wasn't")
	}

}
