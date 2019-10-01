/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1beta2

import (
	"encoding/json"
	"fmt"
	"reflect"
	"testing"
)

func TestSerializeAddressPlan(t *testing.T) {
	ap := AddressPlan{}
	bytes, err := json.Marshal(ap)
	if err != nil {
		t.Fatalf("Failed to serialize")
		return
	}

	fmt.Println(string(bytes))
}

func TestDeserializeAddressPlan(t *testing.T) {
	data := []byte(`
{
        "spec": {
        "addressType": "queue",
        "displayName": "Brokered Queue",
        "displayOrder": 0,
        "longDescription": "Creates a queue on a broker.",
        "resources": {
            "foos": 1
        },
        "shortDescription": "Creates a queue on a broker."
    }
}`)
	ap := AddressPlan{}
	err := json.Unmarshal(data, &ap)

	if err != nil {
		t.Fatal("Failed to decode:", err)
	}

	if val, ok := ap.Spec.Resources["foo"]; ok {
		if val != 1 {
			t.Error("foo resources unequal")
		}
	}
}

func TestDeepEqualsAddressPlan(t *testing.T) {
	data := []byte(`
{
        "spec": {
        "addressType": "queue",
        "displayName": "My queue",
        "displayOrder": 0,
        "longDescription": "My long description",
        "shortDescription": "My short description",
        "resources": {
            "broker": 0
        }
    }
}`)

	ap1 := &AddressPlan{}
	err := json.Unmarshal(data, ap1)

	if err != nil {
		t.Fatal("Failed to decode:", err)
	}

	ap2 := ap1.DeepCopyObject()
	ap3 := ap1.DeepCopy()

	if !reflect.DeepEqual(ap1, ap2) {
		t.Error("a1 equals a2 failed")
	}
	if !reflect.DeepEqual(ap2, ap3) {
		t.Error("a2 equals a3 failed")
	}
	if !reflect.DeepEqual(ap1, ap3) {
		t.Error("a1 equals a3 failed")
	}
}
func TestSerializeAddressSpacePlan(t *testing.T) {
	asp := AddressSpacePlan{}
	bytes, err := json.Marshal(asp)
	if err != nil {
		t.Fatalf("Failed to serialize")
		return
	}

	fmt.Println(string(bytes))
}

func TestDeserializeAddressSpacePlan(t *testing.T) {
	data := []byte(`
{
        "spec": {
        "addressPlans": [
            "foo-plan"
        ],
        "addressSpaceType": "mytype",
        "displayName": "My space plan",
        "displayOrder": 0,
        "infraConfigRef": "my-infra-config",
        "longDescription": "My long description",
        "shortDescription": "My short description",
        "resourceLimits": {
            "foo": 500
        }
    }
}`)
	asp := AddressSpacePlan{}
	err := json.Unmarshal(data, &asp)

	if err != nil {
		t.Fatal("Failed to decode:", err)
	}

	if val, ok := asp.Spec.ResourceLimits["foo"]; ok {
		if val != 500 {
			t.Error("foo resource limits unequal")
		}
	}
}

func TestDeepEqualsAddressSpacePlan(t *testing.T) {
	data := []byte(`
{
        "spec": {
        "addressPlans": [
            "foo-plan"
        ],
        "addressSpaceType": "mytype",
        "displayName": "My space plan",
        "displayOrder": 0,
        "infraConfigRef": "my-infra-config",
        "longDescription": "My long description",
        "shortDescription": "My short description",
        "resourceLimits": {
            "foo": 500
        }
    }
}`)

	asp1 := &AddressSpacePlan{}
	err := json.Unmarshal(data, asp1)

	if err != nil {
		t.Fatal("Failed to decode:", err)
	}

	asp2 := asp1.DeepCopyObject()
	asp3 := asp1.DeepCopy()

	if !reflect.DeepEqual(asp1, asp2) {
		t.Error("a1 equals a2 failed")
	}
	if !reflect.DeepEqual(asp2, asp3) {
		t.Error("a2 equals a3 failed")
	}
	if !reflect.DeepEqual(asp1, asp3) {
		t.Error("a1 equals a3 failed")
	}
}
