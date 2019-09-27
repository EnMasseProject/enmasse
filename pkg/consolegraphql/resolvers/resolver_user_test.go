/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/enmasseproject/enmasse/pkg/util"
	userv1 "github.com/openshift/api/user/v1"
	"github.com/openshift/client-go/user/clientset/versioned/fake"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"testing"
)

const TEST_IDENTITY = "jbloggs@example.com"
const TEST_FULL_NAME = "Joe Bloggs"

func newTestUserResolver(t *testing.T) (*Resolver, context.Context) {
	resolver := Resolver{}

	users := fake.NewSimpleClientset().UserV1().Users()

	userV1 := userv1.User{
		ObjectMeta: metav1.ObjectMeta{
			Name: "~",
		},
		FullName:   TEST_FULL_NAME,
		Identities: []string{TEST_IDENTITY},
		Groups:     []string{"employees"},
	}
	_, err := users.Create(&userV1)
	if err != nil {
		t.Fatal("failed to create user ~", err)
	}
	requestState := &server.RequestState{
		UserInterface: users,
	}

	ctx := server.ContextWithRequestState(requestState, context.TODO())
	return &resolver, ctx
}

func TestWhoAmI(t *testing.T) {
	r, ctx := newTestUserResolver(t)

	userV1, err := r.Query().Whoami(ctx)
	if err != nil {
		t.Fatal("failed to query whoami", err)
	}

	if util.IsOpenshift() {
		if userV1.Identities[0] != TEST_IDENTITY {
			t.Fatalf("Unexpected current user expected: %s actual: %s", TEST_IDENTITY, userV1.Identities[0])
		}
		if userV1.FullName != TEST_FULL_NAME {
			t.Fatalf("Unexpected current user expected: %s actual: %s", TEST_FULL_NAME, userV1.FullName)
		}
	} else {
		if userV1.Identities[0] != "unknown" {
			t.Fatalf("Unexpected current user expected: %s actual: %s", "unknown", userV1.Identities[0])
		}
		if userV1.FullName != "unknown" {
			t.Fatalf("Unexpected current user expected: %s actual: %s", "unknown", userV1.FullName)
		}
	}
}
