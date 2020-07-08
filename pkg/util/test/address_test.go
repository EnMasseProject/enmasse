/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package test

import (
	"testing"

	"github.com/enmasseproject/enmasse/pkg/util"

	"github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestBuildName1(t *testing.T) {

	data := []struct {
		namespace string
		name      string
		prefix    string
		output    string
	}{
		{"foo", "bar", "telemetry", "telemetry/foo.bar"},
	}

	for _, entry := range data {
		actual := util.AddressName(&v1alpha1.IoTProject{ObjectMeta: v1.ObjectMeta{
			Namespace: entry.namespace,
			Name:      entry.name,
		}}, entry.prefix)

		if actual != entry.output {
			t.Errorf("Address name was incorrect - wanted: %s, got: %s", entry.output, actual)
		}
	}

}

func TestEncodeName(t *testing.T) {

	data := []struct {
		name   string
		output string
	}{
		{"foo", "foo"},
		{"foo#bar", "foobar-c0bcd0e4-eba6-3f8e-8ce8-842b01ee4bcd"},
		{"foo.bar", "foobar-f650eb27-6a02-3b32-8e0b-30d0aaa4a225"},
		{".", "4f5067c8-a900-337c-a5a8-5658d18db8b9"},
		{"..", "542b76a4-0d9a-3ca2-93bd-4ba89f64d117"},
	}

	for _, entry := range data {
		actual := util.EncodeAddressSpaceAsMetaName(entry.name)

		if actual != entry.output {
			t.Errorf("Encoding was not correct - wanted: %s, got: %s", entry.output, actual)
		}
	}
}
