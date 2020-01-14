/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package keycloak

import (
	gocloak "github.com/Nerzal/gocloak/v3"
	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/stretchr/testify/assert"

	"reflect"
	"testing"
)

func TestAnnotationDecoding(t *testing.T) {
	annotationList := []string{"{\"key1\":\"value1\"}", "{\"key2\":\"value2\"}"}
	annotations, err := annotationsFromList(annotationList)

	assert.Nil(t, err)

	value, ok := annotations["key1"]
	if assert.True(t, ok) {
		assert.Equal(t, "value1", value)
	}

	value, ok = annotations["key2"]
	if assert.True(t, ok) {
		assert.Equal(t, "value2", value)
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
	assert.Contains(t, mapping, "send_addr1")
	assert.Contains(t, mapping, "send_addr2")
	assert.Contains(t, mapping, "send_addr3%2F%2A")
	assert.Contains(t, mapping, "recv_addr2")
	assert.Contains(t, mapping, "manage")
	assert.Contains(t, mapping, "recv_addr1")
	assert.NotContains(t, mapping, "send_unknown")
}

func TestUserRepToMessagingUser(t *testing.T) {
	testAttributes := make(map[string][]string, 0)
	testAttributes[ATTR_RESOURCE_NAME] = []string{"myspace.test"}
	testAttributes[ATTR_RESOURCE_NAMESPACE] = []string{"ns"}
	testAttributes[ATTR_AUTHENTICATION_TYPE] = []string{"password"}
	testAttributes[ATTR_ANNOTATIONS] = []string{"{\"enmasse.io/key1\":\"value1\"}", "{\"iot.enmasse.io/key2\":\"value2\"}"}
	testAttributes[ATTR_OWNER_REFERENCES] = []string{"{\"apiVersion\":\"v1\",\"kind\":\"ConfigMap\"}"}

	user := &gocloak.User{
		ID:         "1234",
		Username:   "test",
		Attributes: testAttributes,
	}

	groups := []*gocloak.UserGroup{
		&gocloak.UserGroup{
			ID:   "1",
			Name: "send_%2A",
		},
		&gocloak.UserGroup{
			ID:   "2",
			Name: "manage",
		},
	}

	messagingUser, err := userRepToMessagingUser(user, groups)
	assert.Nil(t, err)

	assert.Equal(t, "myspace.test", messagingUser.Name)
	assert.Equal(t, "ns", messagingUser.Namespace)
	assert.Equal(t, "test", messagingUser.Spec.Username)
	assert.Equal(t, userv1beta1.Password, messagingUser.Spec.Authentication.Type)
	assert.Equal(t, 2, len(messagingUser.Spec.Authorization))

	assertAuthorizationOperation(t, userv1beta1.Manage, []string{}, messagingUser.Spec.Authorization)
	assertAuthorizationOperation(t, userv1beta1.Send, []string{"*"}, messagingUser.Spec.Authorization)
	assertAnnotation(t, "enmasse.io/key1", "value1", messagingUser)
	assertAnnotation(t, "iot.enmasse.io/key2", "value2", messagingUser)

	assert.Equal(t, 1, len(messagingUser.ObjectMeta.OwnerReferences))
	assert.Equal(t, "v1", messagingUser.ObjectMeta.OwnerReferences[0].APIVersion)
	assert.Equal(t, "ConfigMap", messagingUser.ObjectMeta.OwnerReferences[0].Kind)
}

func assertAnnotation(t *testing.T, expectedKey string, expectedValue string, user *userv1beta1.MessagingUser) {
	value, ok := user.ObjectMeta.Annotations[expectedKey]
	if assert.Truef(t, ok, "Unable to find annotation %s", expectedKey) {
		assert.Equalf(t, expectedValue, value, "Annotation %s contains unexpected value %s (expected %s)", expectedKey, value, expectedValue)
	}
}

func assertAuthorizationOperation(t *testing.T, operation userv1beta1.AuthorizationOperation, addresses []string, rules []userv1beta1.AuthorizationSpec) {
	foundOperation := false
	for _, rule := range rules {
		for _, op := range rule.Operations {
			foundOperation = true
			if operation == op {
				if reflect.DeepEqual(addresses, rule.Addresses) {
					return
				}
			}
		}
	}

	if assert.Truef(t, foundOperation, "Unable to find operation %s in authorization rules", operation) {
		t.Errorf("Unable to find matching addresses for operation %s", operation)
	}
}

func TestMessagingUserToUserRep(t *testing.T) {
	user := &userv1beta1.MessagingUser{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "myspace.test",
			Namespace: "ns",
		},
		Spec: userv1beta1.MessagingUserSpec{
			Username: "test",
			Authentication: userv1beta1.AuthenticationSpec{
				Type: userv1beta1.Password,
			},
			Authorization: []userv1beta1.AuthorizationSpec{
				userv1beta1.AuthorizationSpec{
					Operations: []userv1beta1.AuthorizationOperation{userv1beta1.Manage},
					Addresses:  make([]string, 0),
				},
				userv1beta1.AuthorizationSpec{
					Operations: []userv1beta1.AuthorizationOperation{userv1beta1.Recv},
					Addresses:  []string{"queue1", "anycast/*"},
				},
			},
		},
	}

	rep, err := createUserRepresentation(user)
	assert.Nil(t, err)

	assert.Equal(t, "test", rep.Username)

	// This is really the only critical attribute to check
	value, ok := rep.Attributes[ATTR_FROM_CRD]
	if assert.Truef(t, ok, "Missing attribute %s", ATTR_FROM_CRD) {
		assert.Equalf(t, "true", value[0], "Unexpected value of attribute %s", ATTR_FROM_CRD)
	}
}
