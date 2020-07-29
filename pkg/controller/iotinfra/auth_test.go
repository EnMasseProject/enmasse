/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"testing"

	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
)

func TestAdapterStatus(t *testing.T) {

	FALSE := false

	infra := &iotv1.IoTInfrastructure{
		Spec: iotv1.IoTInfrastructureSpec{
			AdaptersConfig: iotv1.AdaptersConfig{
				MqttAdapterConfig: iotv1.MqttAdapterConfig{
					CommonAdapterConfig: iotv1.CommonAdapterConfig{
						AdapterConfig: iotv1.AdapterConfig{
							Enabled: &FALSE,
						},
					},
				},
			},
		},
		Status: iotv1.IoTInfrastructureStatus{
			Adapters: map[string]iotv1.AdapterStatus{},
		},
	}

	as := infra.Status.Adapters

	as["mqtt"] = iotv1.AdapterStatus{}
	as["lorawan"] = iotv1.AdapterStatus{}
	as["http"] = iotv1.AdapterStatus{}

	prepareAdapterStatus(infra)
	as = infra.Status.Adapters

	if len(as) != len(adapters) {
		t.Fatalf("Length must be %d, but was %d", len(adapters), len(as))
		return
	}

	if as["mqtt"].Enabled {
		t.Error("MQTT must be disabled")
	}
	if !as["lorawan"].Enabled {
		t.Error("'lorawan' must be enabled")
	}
	if !as["http"].Enabled {
		t.Error("'http' must be enabled")
	}
	if !as["sigfox"].Enabled {
		t.Error("'sigfox' must be enabled")
	}

}
