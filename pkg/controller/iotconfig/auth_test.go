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

	config := &iotv1alpha1.IoTConfig{
		Status: iotv1alpha1.IoTConfigStatus{
			Adapters: map[string]iotv1alpha1.AdapterStatus{},
		},
	}

	as := config.Status.Adapters

	as["mqtt"] = iotv1alpha1.AdapterStatus{}
	as["lorawan"] = iotv1alpha1.AdapterStatus{}
	as["http"] = iotv1alpha1.AdapterStatus{
		InterServicePassword: "foobar",
	}

	config.Status.Adapters = ensureAdapterStatus(as)
	as = config.Status.Adapters

	err := initConfigStatus(config)
	if err != nil {
		t.Fatal("initConfigStatus failed: ", err)
		return
	}

	if len(as) != 4 {
		t.Fatalf("Length must be 4, but was %d", len(as))
		return
	}

	if as["mqtt"].InterServicePassword == "" {
		t.Error("InterServicePassword for 'mqtt' is not set")
	}
	if as["lorawan"].InterServicePassword == "" {
		t.Error("InterServicePassword for 'lorawan' is not set")
	}
	if as["http"].InterServicePassword != "foobar" {
		t.Error("InterServicePassword for 'http' has changed")
	}
}
