/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1beta1

import (
	"encoding/json"
	"fmt"
	"reflect"
	"testing"
)

func TestSerialize(t *testing.T) {
	addressSpace := AddressSpace{}
	bytes, err := json.Marshal(addressSpace)
	if err != nil {
		t.Fatalf("Failed to serialize")
		t.Fail()
		return
	}

	fmt.Println(string(bytes))
}

func TestDeserialize(t *testing.T) {
	data := []byte(`
{
	"spec": {
		"type": "type1",
		"authenticationService": {
			"details":{
				"foo1": "bar1"
			}
		}
	}
}
	`)
	as := AddressSpace{}
	err := json.Unmarshal(data, &as)

	if err != nil {
		t.Fatal("Failed to decode:", err)
	}
}

func TestDeepEquals(t *testing.T) {

	data := []byte(`
{
	"spec": {
		"type": "type1",
		"authenticationService": {
			"details":{
				"foo1": "bar1"
			}
		}
	}
}
	`)
	addressSpace1 := &AddressSpace{}
	err := json.Unmarshal(data, addressSpace1)

	if err != nil {
		t.Fatal("Failed to decode:", err)
	}

	addressSpace2 := addressSpace1.DeepCopyObject()
	addressSpace3 := addressSpace1.DeepCopy()

	if !reflect.DeepEqual(addressSpace1, addressSpace2) {
		t.Error("a1 equals a2 failed")
	}
	if !reflect.DeepEqual(addressSpace2, addressSpace3) {
		t.Error("a2 equals a3 failed")
	}
	if !reflect.DeepEqual(addressSpace1, addressSpace3) {
		t.Error("a1 equals a3 failed")
	}

}
