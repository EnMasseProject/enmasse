/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"testing"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

func TestAdapterStatus(t *testing.T) {

	FALSE := false

	config := &iotv1alpha1.IoTConfig{
		Spec: iotv1alpha1.IoTConfigSpec{
			AdaptersConfig: iotv1alpha1.AdaptersConfig{
				MqttAdapterConfig: iotv1alpha1.MqttAdapterConfig{
					CommonAdapterConfig: iotv1alpha1.CommonAdapterConfig{
						AdapterConfig: iotv1alpha1.AdapterConfig{
							Enabled: &FALSE,
						},
					},
				},
			},
		},
		Status: iotv1alpha1.IoTConfigStatus{
			Adapters: map[string]iotv1alpha1.AdapterStatus{},
		},
	}

	as := config.Status.Adapters

	as["mqtt"] = iotv1alpha1.AdapterStatus{}
	as["lorawan"] = iotv1alpha1.AdapterStatus{}
	as["http"] = iotv1alpha1.AdapterStatus{}

	prepareAdapterStatus(config)
	as = config.Status.Adapters

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
