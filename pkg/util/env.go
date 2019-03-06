/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"os"
	"strings"
)

func GetEnvOrDefault(key string, defaultValue string) string {
	value, ok := os.LookupEnv(key)
	if ok {
		return value
	} else {
		return defaultValue
	}
}

func GetBooleanEnvOrDefault(key string, defaultValue bool) bool {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	} else {
		return "true" == strings.ToLower(value)
	}
}

func GetBooleanEnv(key string) bool {
	return GetBooleanEnvOrDefault(key, false)
}
