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
	"github.com/stretchr/testify/assert"
	v12 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"reflect"
	"testing"
)

func newTestAddressSpaceSchemaResolver(t *testing.T) *Resolver {
	c := &cache.MemdbCache{}
	err := c.Init(cache.IndexSpecifier{
		Name:    "id",
		Indexer: &cache.UidIndex{},
	},
		cache.IndexSpecifier{
			Name: "hierarchy",
			Indexer: &cache.HierarchyIndex{
				IndexCreators: map[string]cache.HierarchicalIndexCreator{
					"AddressSpaceSchema": watchers.AddressSpaceSchemaIndexCreator,
				},
			},
		})
	assert.NoError(t, err)

	resolver := Resolver{}
	resolver.Cache = c
	return &resolver
}

func TestQueryResolver_AddressSpaceSchema(t *testing.T) {
	r := newTestAddressSpaceSchemaResolver(t)
	addrSpaceSchema := &v1beta1.AddressSpaceSchema{
		TypeMeta: v12.TypeMeta{
			Kind: "AddressSpaceSchema",
		},
		ObjectMeta: v12.ObjectMeta{
			Name: "brokered",
			UID:  types.UID(uuid.New().String()),
		},
	}
	err := r.Cache.Add(addrSpaceSchema)
	if err != nil {
		t.Fatal(err)
	}

	objs, err := r.Query().AddressSpaceSchema(context.TODO())
	if err != nil {
		t.Fatal(err)
	}


	if !reflect.DeepEqual(addrSpaceSchema.Spec, objs[0].Spec) {
		t.Fatalf("Unexpected addressSpaceSchema spec, expected %+v actual %+v", addrSpaceSchema.Spec, objs[0].Spec)
	}
	if !reflect.DeepEqual(addrSpaceSchema.ObjectMeta, objs[0].ObjectMeta) {
		t.Fatalf("Unexpected addressSpaceSchema object meta, expected %+v actual %+v", addrSpaceSchema.ObjectMeta, objs[0].ObjectMeta)
	}
}
