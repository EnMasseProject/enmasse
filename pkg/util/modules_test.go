/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"testing"
)

var envs = []struct {
	vars   map[string]string
	module string
	result bool
}{
	{map[string]string{}, "iot", true},
	{map[string]string{"CONTROLLER_DISABLE_ALL": "false"}, "iot", true},
	{map[string]string{"CONTROLLER_DISABLE_ALL": "true"}, "iot", false},
	{map[string]string{"CONTROLLER_ENABLE_ALL": "true", "CONTROLLER_DISABLE_ALL": "true"}, "iot", true},

	{map[string]string{"CONTROLLER_ENABLE_IOT": "true", "CONTROLLER_DISABLE_ALL": "true"}, "iot", true},
	{map[string]string{"CONTROLLER_ENABLE_IOT": "false", "CONTROLLER_DISABLE_ALL": "true"}, "iot", false},
}

func TestEnabled(t *testing.T) {

	// CONTROLLER_ENABLE_IOT = false

	// CONTROLLER_ENABLE_IOT = true
	// CONTROLLER_DISABLE_ALL = true

	for _, data := range envs {
		env := MockEnvironmentProvider{environment: data.vars}
		if ret := isModuleEnabled(env, data.module); ret != data.result {
			t.Errorf("Expected module %s to be state = %t, but was %t", data.module, data.result, ret)
		}
	}
}

func TestSimpleExample(t *testing.T) {

	env1 := MockEnvironmentProvider{environment: map[string]string{
		"CONTROLLER_ENABLE_IOT": "false",
	}}

	env2 := MockEnvironmentProvider{environment: map[string]string{
		"CONTROLLER_ENABLE_IOT":  "true",
		"CONTROLLER_DISABLE_ALL": "true",
	}}

	// test env1

	if isModuleEnabled(env1, "iot") != false {
		t.Errorf("Module 'iot' must be disabled in environment #1")
	}
	if isModuleEnabled(env1, "foo") != true {
		t.Errorf("Module 'foo' must be enabled in environment #1")
	}

	// test env2

	if isModuleEnabled(env2, "iot") != true {
		t.Errorf("Module 'iot' must be enabled in environment #2")
	}
	if isModuleEnabled(env2, "foo") != false {
		t.Errorf("Module 'foo' must be disabled in environment #1")
	}

}
