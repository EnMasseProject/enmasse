/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"strings"
)

// implementation

func isBooleanEnv(environment EnvironmentProvider, name string, defaultValue bool) bool {
	val := getBooleanEnv(environment, name)
	if val == nil {
		return defaultValue
	} else {
		return *val
	}
}

func getBooleanEnv(environment EnvironmentProvider, name string) *bool {
	val, ok := environment.LookupEnv(name)
	if !ok {
		return nil
	} else {
		b := strings.ToLower(val) == "true"
		return &b
	}
}

// check if a controller module is enabled, or not
func IsModuleEnabled(module string) bool {
	return isModuleEnabled(&OSEnvironmentProvider{}, module)
}

func isModuleEnabled(environment EnvironmentProvider, module string) bool {

	module = strings.ToUpper(module)

	// check is "everything" is enabled ...
	if isBooleanEnv(environment, "CONTROLLER_ENABLE_ALL", false) {
		// ... it is
		return true
	}

	val := getBooleanEnv(environment, "CONTROLLER_ENABLE_"+module)
	// if val is set ...
	if val != nil {
		// ... it is authoritative
		return *val
	}

	// no implicit enable or disable, now check if everything is disabled

	return !isBooleanEnv(environment, "CONTROLLER_DISABLE_ALL", false)
}
