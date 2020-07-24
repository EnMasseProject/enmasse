/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"reflect"
	"testing"

	"github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
)

func TestMerge(t *testing.T) {

	input := []struct {
		first  *v1.AdapterOptions
		second *v1.AdapterOptions
		result v1.AdapterOptions
	}{
		{
			first:  nil,
			second: nil,
			result: v1.AdapterOptions{},
		},
		{
			first: nil,
			second: &v1.AdapterOptions{
				TenantIdleTimeout: "1m",
			},
			result: v1.AdapterOptions{
				TenantIdleTimeout: "1m",
			},
		},
		{
			first: &v1.AdapterOptions{
				TenantIdleTimeout: "1m",
			},
			second: nil,
			result: v1.AdapterOptions{
				TenantIdleTimeout: "1m",
			},
		},
		{
			first: &v1.AdapterOptions{
				TenantIdleTimeout: "1m",
			},
			second: &v1.AdapterOptions{
				TenantIdleTimeout: "2m",
			},
			result: v1.AdapterOptions{
				TenantIdleTimeout: "2m",
			},
		},
	}

	for _, i := range input {
		r := mergeAdapterOptions(i.first, i.second)
		if !reflect.DeepEqual(i.result, r) {
			t.Errorf("Failed to merge - expected: %v, actual: %v", i.result, r)
		}
	}

}
