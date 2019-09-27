/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/google/uuid"
	v12 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"reflect"
	"testing"
)

func newTestConnectionResolver(t *testing.T) *Resolver {
	cache := &cache.MemdbCache{}
	err := cache.Init()
	if err != nil {
		t.Fatal("failed to create test resolver")
	}
	cache.RegisterIndexCreator("Connection", watchers.ConnectionIndexCreator)

	resolver := Resolver{}
	resolver.Cache = cache
	return &resolver
}

func TestQueryConnection(t *testing.T) {
	r := newTestConnectionResolver(t)
	uid := uuid.New().String()
	addr := &consolegraphql.Connection{
		TypeMeta: v12.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: v12.ObjectMeta{
			Name: uid,
			UID:  types.UID(uid),
		},
	}
	err := r.Cache.Add(addr)
	if err != nil {
		t.Fatal(err)
	}

	objs, err := r.Query().Connections(context.TODO(), nil, nil, nil, nil)
	if err != nil {
		t.Fatal(err)
	}

	expected := 1
	actual := objs.Total
	if actual != expected {
		t.Fatalf("Unexpected number of addresses, expected %d, actual %d", expected, actual)
	}

	if !reflect.DeepEqual(addr.Spec, *objs.Connections[0].Spec) {
		t.Fatalf("Unexpected connection spec, expected %+v actual %+v", addr.Spec, objs.Connections[0].Spec)
	}
	if !reflect.DeepEqual(addr.ObjectMeta, *objs.Connections[0].ObjectMeta) {
		t.Fatalf("Unexpected address object meta, expected %+v actual %+v", addr.ObjectMeta, objs.Connections[0].ObjectMeta)
	}
}
