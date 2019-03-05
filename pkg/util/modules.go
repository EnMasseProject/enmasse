/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"os"
	"strings"
)

type environmentProvider interface {
	lookupEnv(string) (string, bool)
}

// OS environment provider

type OSEnvironmentProvider struct {
}

func (p OSEnvironmentProvider) lookupEnv(name string) (string, bool) {
	return os.LookupEnv(name)
}

var _ environmentProvider = OSEnvironmentProvider{}

// mock environment provider

type MockEnvironmentProvider struct {
	environment map[string]string
}

func (p MockEnvironmentProvider) lookupEnv(name string) (string, bool) {
	val, ok := p.environment[name]
	return val, ok
}

var _ environmentProvider = MockEnvironmentProvider{}

// implementation

func isBooleanEnv(environment environmentProvider, name string, defaultValue bool) bool {
	val := getBooleanEnv(environment, name)
	if val == nil {
		return defaultValue
	} else {
		return *val
	}
}

func getBooleanEnv(environment environmentProvider, name string) *bool {
	val, ok := environment.lookupEnv(name)
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

func isModuleEnabled(environment environmentProvider, module string) bool {

	// check is "everything" is enabled ...
	if isBooleanEnv(environment, "CONTROLLER_ENABLE_ALL", false) {
		// ... it is
		return true
	}

	val := getBooleanEnv(environment, "CONTROLLER_ENABLE_"+strings.ToUpper(module))
	// if val is set ...
	if val != nil {
		// ... it is authorative
		return *val
	}

	// no implicit enable or disable, now check if everything is disabled

	return !isBooleanEnv(environment, "CONTROLLER_DISABLE_ALL", false)
}
