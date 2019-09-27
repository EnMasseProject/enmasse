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
	v1 "k8s.io/api/core/v1"
	v12 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"reflect"
	"testing"
)

func newTestNamespaceResolver(t *testing.T) *Resolver {
	cache := &cache.MemdbCache{}
	err := cache.Init()
	if err != nil {
		t.Fatal("failed to create test resolver")
	}
	cache.RegisterIndexCreator("Namespace", watchers.NamespaceIndexCreator)

	resolver := Resolver{}
	resolver.Cache = cache
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
	if err != nil {
		t.Fatal(err)
	}

	objs, err := r.Query().Namespaces(context.TODO())
	if err != nil {
		t.Fatal(err)
	}

	expected := 1
	actual := len(objs)
	if actual != expected {
		t.Fatalf("Unexpected number of namespaces expected %d, actual %d", expected, actual)
	}
	if !reflect.DeepEqual(namespace, objs[0]) {
		t.Fatalf("Unexpected namespace expected %+v actual %+v", namespace, objs[0])
	}
}
