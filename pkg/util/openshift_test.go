/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/apimachinery/pkg/version"
	"os"
	"testing"
)

func TestHasApiWithEnvTrue(t *testing.T) {

	// reset cache before testing
	apis = make(map[string]bool, 0)

	name := "ENMASSE_HAS_API_FOO.BAR_V0ALPHA0_BAZ"

	if err := os.Setenv(name, "true"); err != nil {
		t.Error(err)
		return
	}

	if !HasApi(schema.GroupVersionKind{
		Group:   "foo.bar",
		Version: "v0alpha0",
		Kind:    "Baz",
	}) {
		t.Error("Result should be 'true'")
	}

	if err := os.Unsetenv(name); err != nil {
		t.Error(err)
		return
	}

}

func TestHasApiWithEnvFalse(t *testing.T) {

	// reset cache before testing
	apis = make(map[string]bool, 0)

	name := "ENMASSE_HAS_API_FOO.BAR_V0ALPHA0_BAZ"

	if err := os.Setenv(name, "false"); err != nil {
		t.Error(err)
		return
	}

	if HasApi(schema.GroupVersionKind{
		Group:   "foo.bar",
		Version: "v0alpha0",
		Kind:    "Baz",
	}) {
		t.Error("Result should be 'false'")
	}

	if err := os.Unsetenv(name); err != nil {
		t.Error(err)
		return
	}

}

func TestIsOpenShift4(t *testing.T) {

	var data = []struct {
		major  string
		minor  string
		isFour bool
	}{
		{"1", "9+", false},
		{"1", "9", false},
		{"1", "18+", true},
		{"1", "19", true},
	}

	for _, d := range data {
		v := version.Info{
			Major: d.major,
			Minor: d.minor,
		}
		r := evalIsOpenShift4(&v)
		if r != d.isFour {
			t.Errorf("Failed result - version: %v, expectedResult: %v (was: %v)", v, d.isFour, r)
		}
	}

}
