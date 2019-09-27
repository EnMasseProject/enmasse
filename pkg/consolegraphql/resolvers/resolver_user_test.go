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
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"testing"
)

const testIdentity = "jbloggs@example.com"
const testFullName = "Joe Bloggs"

func newTestUserResolver(t *testing.T) (*Resolver, context.Context) {
	resolver := Resolver{}

	users := fake.NewSimpleClientset().UserV1().Users()

	userV1 := userv1.User{
		ObjectMeta: metav1.ObjectMeta{
			Name: "~",
		},
		FullName:   testFullName,
		Identities: []string{testIdentity},
		Groups:     []string{"employees"},
	}
	_, err := users.Create(&userV1)
	assert.NoError(t, err, "failed to create user ~")

	requestState := &server.RequestState{
		UserInterface: users,
	}

	ctx := server.ContextWithRequestState(requestState, context.TODO())
	return &resolver, ctx
}

func TestWhoAmI(t *testing.T) {
	r, ctx := newTestUserResolver(t)

	userV1, err := r.Query().Whoami(ctx)
	assert.NoError(t, err, "failed to query whoami")

	if util.IsOpenshift() {
		assert.Equal(t, testIdentity, userV1.Identities[0], "Unexpected current user")
		assert.Equal(t, testFullName, userV1.FullName, "Unexpected current user")
	} else {
		assert.Equal(t, "unknown", userV1.Identities[0], "Unexpected current user")
		assert.Equal(t, "unknown", userV1.FullName, "Unexpected current user")
	}
}
