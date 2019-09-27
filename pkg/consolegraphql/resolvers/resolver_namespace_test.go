/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	v1 "k8s.io/api/core/v1"
	v12 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"testing"
)

func newTestNamespaceResolver(t *testing.T) *Resolver {
	c := &cache.MemdbCache{}
	err := c.Init(
		cache.IndexSpecifier{
			Name:    "id",
			Indexer: &cache.UidIndex{},
		},
		cache.IndexSpecifier{
			Name: "hierarchy",
			Indexer: &cache.HierarchyIndex{
				IndexCreators: map[string]cache.HierarchicalIndexCreator{
					"Namespace": watchers.NamespaceIndexCreator,
				},
			},
		})
	assert.NoError(t, err, "failed to create test resolver")

	resolver := Resolver{}
	resolver.Cache = c
	return &resolver
}

func TestQueryNamespace(t *testing.T) {
	r := newTestNamespaceResolver(t)
	namespace := &v1.Namespace{
		TypeMeta: v12.TypeMeta{
			Kind: "Namespace",
		},
		ObjectMeta: v12.ObjectMeta{
			Name: "mynamespace",
			UID:  types.UID(uuid.New().String()),
		},
		Status: v1.NamespaceStatus{
			Phase: v1.NamespaceActive,
		},
	}
	err := r.Cache.Add(namespace)
	assert.NoError(t, err)

	objs, err := r.Query().Namespaces(context.TODO())
	assert.NoError(t, err)

	expected := 1
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of namespaces")
	assert.Equal(t, namespace, objs[0], "Unexpected namespace")
}
