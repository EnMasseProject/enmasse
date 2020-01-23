/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/accesscontroller"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	v1 "k8s.io/api/core/v1"
	v12 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"strings"
	"testing"
)

type testAccController struct {
}

func newTestAccessController() accesscontroller.AccessController {
	return &testAccController{}
}

func (a *testAccController) CanRead(obj interface{}) (bool, error) {
	return strings.HasPrefix(obj.(*v1.Namespace).Name, "public_"), nil
}

func (a *testAccController) ViewFilter() cache.ObjectFilter {
	return accesscontroller.FilterAdapter(a)
}

func (a *testAccController) GetState() (bool, interface{}) {
	return false, nil
}

func newTestNamespaceResolver(t *testing.T) (*Resolver, context.Context) {
	objectCache, err := cache.CreateObjectCache()
	assert.NoError(t, err, "failed to create object cache")

	resolver := Resolver{}
	resolver.Cache = objectCache

	requestState := &server.RequestState{
		AccessController: accesscontroller.NewAllowAllAccessController(),
	}

	ctx := server.ContextWithRequestState(requestState, context.TODO())
	return &resolver, ctx
}

func TestQueryNamespace(t *testing.T) {
	r, ctx := newTestNamespaceResolver(t)
	namespace := createNamespace("mynamespace")
	err := r.Cache.Add(namespace)
	assert.NoError(t, err)

	objs, err := r.Query().Namespaces(ctx)
	assert.NoError(t, err)

	assert.Equal(t, 1, len(objs), "Unexpected number of namespaces")
	assert.Equal(t, namespace, objs[0], "Unexpected namespace")
}

func TestQueryNamespaceUserView(t *testing.T) {
	r, _ := newTestNamespaceResolver(t)

	requestState := &server.RequestState{
		AccessController: newTestAccessController(),
	}
	ctx := server.ContextWithRequestState(requestState, context.TODO())

	namespace1 := createNamespace("public_mynamespace")
	namespace2 := createNamespace("private_mynamespace")
	err := r.Cache.Add(namespace1, namespace2)
	assert.NoError(t, err)

	objs, err := r.Query().Namespaces(ctx)
	assert.NoError(t, err)

	assert.Equal(t, 1, len(objs), "Unexpected number of namespaces")
	assert.Equal(t, namespace1, objs[0], "Unexpected namespace")
}

func createNamespace(name string) *v1.Namespace {
	namespace := &v1.Namespace{
		TypeMeta: v12.TypeMeta{
			Kind: "Namespace",
		},
		ObjectMeta: v12.ObjectMeta{
			Name: name,
			UID:  types.UID(uuid.New().String()),
		},
		Status: v1.NamespaceStatus{
			Phase: v1.NamespaceActive,
		},
	}
	return namespace
}
