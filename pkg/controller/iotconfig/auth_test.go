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

	as := make(map[string]iotv1alpha1.AdapterStatus)

	as["foo"] = iotv1alpha1.AdapterStatus{}
	as["bar"] = iotv1alpha1.AdapterStatus{}
	as["boo"] = iotv1alpha1.AdapterStatus{
		InterServicePassword: "foobar",
	}

	as = ensureAdapterStatus(as, "foo", "baz", "boo")

	err := ensureAdapterAuthCredentials(as)
	if err != nil {
		t.Fatal("ensureAdapterAuthCredentials failed: ", err)
		return
	}

	if len(as) != 3 {
		t.Fatalf("Length must be 3, but was %d", len(as))
		return
	}

	if as["foo"].InterServicePassword == "" {
		t.Error("InterServicePassword for 'foo' is not set")
	}
	if as["baz"].InterServicePassword == "" {
		t.Error("InterServicePassword for 'baz' is not set")
	}
	if as["boo"].InterServicePassword != "foobar" {
		t.Error("InterServicePassword for 'boo' has changed")
	}
}
