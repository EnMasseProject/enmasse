/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/stretchr/testify/assert"
	"testing"
)

func newTestAddressSpaceResolver(t *testing.T) *Resolver {
	c := &cache.MemdbCache{}
	err := c.Init(cache.IndexSpecifier{
		Name:    "id",
		Indexer: &cache.UidIndex{},
	},
		cache.IndexSpecifier{
			Name: "hierarchy",
			Indexer: &cache.HierarchyIndex{
				IndexCreators: map[string]cache.HierarchicalIndexCreator{
					"AddressSpace": watchers.AddressSpaceIndexCreator,
					"Connection":   watchers.ConnectionIndexCreator,
				},
			},
		})
	assert.NoError(t, err)

	resolver := Resolver{}
	resolver.Cache = c
	return &resolver
}

func TestQueryAddressSpace(t *testing.T) {
	r := newTestAddressSpaceResolver(t)
	as := createAddressSpace("mynamespace", "myaddressspace")
	err := r.Cache.Add(as)
	assert.NoError(t, err)

	objs, err := r.Query().AddressSpaces(context.TODO(), nil, nil, nil, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of address spaces")

	assert.Equal(t, as.Spec, objs.AddressSpaces[0].Spec, "Unexpected address space spec")
	assert.Equal(t, as.ObjectMeta, objs.AddressSpaces[0].ObjectMeta, "Unexpected address space object meta")
}

func TestQueryAddressSpaceFilter(t *testing.T) {
	r := newTestAddressSpaceResolver(t)
	as1 := createAddressSpace("mynamespace1", "myaddressspace")
	as2 := createAddressSpace("mynamespace2", "myaddressspace")
	err := r.Cache.Add(as1, as2)
	assert.NoError(t, err)

	filter := fmt.Sprintf("`$.ObjectMeta.Name` = '%s'", as1.ObjectMeta.Name)
	objs, err := r.Query().AddressSpaces(context.TODO(), nil, nil, &filter, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of address spaces")

	assert.Equal(t, as1.ObjectMeta, objs.AddressSpaces[0].ObjectMeta, "Unexpected address space object meta")
}

func TestQueryAddressSpaceOrder(t *testing.T) {
	r := newTestAddressSpaceResolver(t)
	as1 := createAddressSpace("mynamespace1", "myaddressspace")
	as2 := createAddressSpace("mynamespace2", "myaddressspace")
	err := r.Cache.Add(as1, as2)
	assert.NoError(t, err)

	orderby := "`$.ObjectMeta.Name` DESC"
	objs, err := r.Query().AddressSpaces(context.TODO(), nil, nil, nil,  &orderby)
	assert.NoError(t, err)

	expected := 2
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of address spaces")

	assert.Equal(t, as2.ObjectMeta, objs.AddressSpaces[0].ObjectMeta, "Unexpected address space object meta")
	assert.Equal(t, as1.ObjectMeta, objs.AddressSpaces[1].ObjectMeta, "Unexpected address space object meta")
}

func TestQueryAddressSpacePagination(t *testing.T) {
	r := newTestAddressSpaceResolver(t)
	as1 := createAddressSpace("mynamespace1", "myaddressspace")
	as2 := createAddressSpace("mynamespace2", "myaddressspace")
	as3 := createAddressSpace("mynamespace3", "myaddressspace")
	as4 := createAddressSpace("mynamespace4", "myaddressspace")
	err := r.Cache.Add(as1, as2, as3, as4)
	assert.NoError(t, err)

	objs, err := r.Query().AddressSpaces(context.TODO(), nil, nil, nil,  nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of address spaces")

	one := 1
	two := 2
	objs, err = r.Query().AddressSpaces(context.TODO(), nil, &one, nil,  nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of address spaces")
	assert.Equal(t, 3, len(objs.AddressSpaces), "Unexpected number of address spaces in page")
	assert.Equal(t, as2.ObjectMeta, objs.AddressSpaces[0].ObjectMeta, "Unexpected address space object meta")

	objs, err = r.Query().AddressSpaces(context.TODO(), &one, &two, nil,  nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of address spaces")
	assert.Equal(t, 1, len(objs.AddressSpaces), "Unexpected number of address spaces in page")
	assert.Equal(t, as3.ObjectMeta, objs.AddressSpaces[0].ObjectMeta, "Unexpected address space object meta")
}

