/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
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

func GetDurationEnvOrDefault(key string, defaultValue time.Duration) time.Duration {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	} else {
		duration, err := time.ParseDuration(value)
		if err != nil {
			return defaultValue
		} else {
			return duration
		}
	}
}

func GetUintEnvOrDefault(key string, base int, bitSize int, defaultValue uint64) uint64 {
	// Validate default value agrees with bitSize
	_, err := strconv.ParseUint(strconv.FormatUint(defaultValue, 10), base, bitSize)
	if err != nil {
		panic(fmt.Errorf("defaultValue %d would overflow %d bits", defaultValue, bitSize))
	}

	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	} else {
		uintValue, err := strconv.ParseUint(value, base, bitSize)
		if err != nil {
			return defaultValue
		} else {
			return uintValue
		}
	}
}

func GetEnvOrError(name string) (string, error) {
	result := os.Getenv(name)
	if result != "" {
		return result, nil
	} else {
		return "", fmt.Errorf("'%s' is not set", name)
	}
}

func GetBooleanEnv(key string) bool {
	return GetBooleanEnvOrDefault(key, false)
}

type EnvironmentProvider interface {
	LookupEnv(string) (string, bool)
	Get(string) string
}

// OS environment provider

type OSEnvironmentProvider struct {
}

func (p OSEnvironmentProvider) LookupEnv(name string) (string, bool) {
	return os.LookupEnv(name)
}

func (p OSEnvironmentProvider) Get(name string) string {
	return os.Getenv(name)
}

var _ EnvironmentProvider = OSEnvironmentProvider{}

// mock environment provider

type MockEnvironmentProvider struct {
	Environment map[string]string
}

func (p MockEnvironmentProvider) LookupEnv(name string) (string, bool) {
	val, ok := p.Environment[name]
	return val, ok
}

func (p MockEnvironmentProvider) Get(name string) string {
	return p.Environment[name]
}

var _ EnvironmentProvider = MockEnvironmentProvider{}
