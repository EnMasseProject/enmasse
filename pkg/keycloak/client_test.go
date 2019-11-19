/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package keycloak

import (
	//"fmt"
	"testing"
	// gocloak "github.com/Nerzal/gocloak/v3"
	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
)

func TestAnnotationDecoding(t *testing.T) {
	annotationList := []string{"{\"key1\":\"value1\"}", "{\"key2\":\"value2\"}"}
	annotations, err := annotationsFromList(annotationList)

	if err != nil {
		t.Fatalf("decoding annotations: (%v)", err)
	}

	value, ok := annotations["key1"]
	if !ok {
		t.Fatal("missing entry key1")
	}
	if value != "value1" {
		t.Fatal("wrong value for key1: " + value)
	}

	value, ok = annotations["key2"]
	if !ok {
		t.Fatal("missing entry key2")
	}
	if value != "value2" {
		t.Fatal("wrong value for key2: " + value)
	}
}

func TestGroupEncoding(t *testing.T) {
	testSpec := []userv1beta1.AuthorizationSpec{
		userv1beta1.AuthorizationSpec{
			Addresses:  []string{"addr1", "addr2", "addr3/*"},
			Operations: []userv1beta1.AuthorizationOperation{userv1beta1.Send},
		},
		userv1beta1.AuthorizationSpec{
			Addresses:  []string{"addr2"},
			Operations: []userv1beta1.AuthorizationOperation{userv1beta1.Recv},
		},
		userv1beta1.AuthorizationSpec{
			Addresses:  []string{"addr1"},
			Operations: []userv1beta1.AuthorizationOperation{userv1beta1.Manage, userv1beta1.Recv},
		},
	}

	mapping := createDesiredGroups(testSpec)
	// fmt.Println(mapping)
	assertGroup(t, "send_addr1", mapping)
	assertGroup(t, "send_addr2", mapping)
	assertGroup(t, "send_addr3%2F%2A", mapping)
	assertGroup(t, "recv_addr2", mapping)
	assertGroup(t, "manage", mapping)
	assertGroup(t, "recv_addr1", mapping)
	assertNoGroup(t, "send_unknown", mapping)
}

func assertGroup(t *testing.T, expected string, mapping map[string]bool) {
	if _, ok := mapping[expected]; !ok {
		t.Error("Expected group " + expected + " not found")
	}
}

func assertNoGroup(t *testing.T, unexpected string, mapping map[string]bool) {
	if _, ok := mapping[unexpected]; ok {
		t.Error("Unexpected group " + unexpected + " found")
	}
}

func TestUserRepToMessagingUser(t *testing.T) {
}
