/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"encoding/json"
	"testing"
)

const testExpected1 = `{"downstream":{"addresses":{"telemetry":{},"event":{},"command":{}}},"configuration":{"enabled":true,"ext":{"bar":"baz","bar2":2}}}`

// Test to decode and re-encode the generic config section
func TestSerializeConfiguration(t *testing.T) {
	spec := &IoTProjectSpec{}
	if err := json.Unmarshal([]byte(`
{
	"configuration": {
		"enabled": true,
		"ext": {
			"bar": "baz",
			"bar2": 2
        }
	}
}
`), spec); err != nil {
		t.Fatalf("Failed to decode: %v", err)
		return
	}

	data, err := json.Marshal(spec)
	if err != nil {
		t.Fatalf("Failed to encode: %v", err)
		return
	}

	if string(data) != testExpected1 {
		t.Error("Expected JSON to be: ", testExpected1, " but was ", string(data))
	}
}
