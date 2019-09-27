/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/google/uuid"
	v12 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"reflect"
	"testing"
)

func newTestAddressResolver(t *testing.T) *Resolver {
	cache := &cache.MemdbCache{}
	err := cache.Init()
	if err != nil {
		t.Fatal("failed to create test resolver")
	}
	cache.RegisterIndexCreator("Address", watchers.AddressIndexCreator)

	resolver := Resolver{}
	resolver.Cache = cache
	return &resolver
}

func TestQueryAddress(t *testing.T) {
	r := newTestAddressResolver(t)
	addr := &v1beta1.Address{
		TypeMeta: v12.TypeMeta{
			Kind: "Address",
		},
		ObjectMeta: v12.ObjectMeta{
			Name: "myaddrspace.myaddr",
			UID:  types.UID(uuid.New().String()),
		},
	}
	err := r.Cache.Add(addr)
	if err != nil {
		t.Fatal(err)
	}

	objs, err := r.Query().Addresses(context.TODO(), nil, nil, nil, nil)
	if err != nil {
		t.Fatal(err)
	}

	expected := 1
	actual := objs.Total
	if actual != expected {
		t.Fatalf("Unexpected number of addresses, expected %d, actual %d", expected, actual)
	}

	if !reflect.DeepEqual(addr.Spec, *objs.Addresses[0].Spec) {
		t.Fatalf("Unexpected address spec, expected %+v actual %+v", addr.Spec, objs.Addresses[0].Spec)
	}
	if !reflect.DeepEqual(addr.ObjectMeta, *objs.Addresses[0].ObjectMeta) {
		t.Fatalf("Unexpected address object meta, expected %+v actual %+v", addr.ObjectMeta, objs.Addresses[0].ObjectMeta)
	}
}
