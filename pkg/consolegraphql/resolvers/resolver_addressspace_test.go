/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/stretchr/testify/assert"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"testing"
)

func newTestAddressSpaceResolver(t *testing.T) *Resolver {
	objectCache, err := cache.CreateObjectCache()
	assert.NoError(t, err)

	resolver := Resolver{}
	resolver.Cache = objectCache
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

func TestQueryAddressSpaceConnections(t *testing.T) {
	r := newTestAddressSpaceResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	con1 := createConnection("host:1234", namespace, addressspace)
	// Different addresspace, should not be found
	con2 := createConnection("host:1234", namespace, "myaddressspace1")
	err := r.Cache.Add(con1, con2)
	assert.NoError(t, err)

	ash := &consolegraphql.AddressSpaceHolder{
		AddressSpace: v1beta1.AddressSpace{
			ObjectMeta: v1.ObjectMeta{
				Name:                       addressspace,
				Namespace:                  namespace,
			},
		},
	}
	objs, err := r.AddressSpace_consoleapi_enmasse_io_v1beta1().Connections(context.TODO(), ash, nil, nil, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 1, objs.Total, "Unexpected number of connections associated with the addressspace")
}

func TestQueryAddressSpaceConnectionFilter(t *testing.T) {
	r := newTestAddressSpaceResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	con1 := createConnection("host:1234", namespace, addressspace)
	con2 := createConnection("host:1235", namespace, addressspace)
	err := r.Cache.Add(con1, con2)
	assert.NoError(t, err)

	ash := &consolegraphql.AddressSpaceHolder{
		AddressSpace: v1beta1.AddressSpace{
			ObjectMeta: v1.ObjectMeta{
				Name:                       addressspace,
				Namespace:                  namespace,
			},
		},
	}

	filter := fmt.Sprintf("`$.ObjectMeta.Name` = '%s'", con2.ObjectMeta.Name)
	objs, err := r.AddressSpace_consoleapi_enmasse_io_v1beta1().Connections(context.TODO(), ash, nil, nil, &filter, nil)
	assert.NoError(t, err)
	assert.Equal(t, 1, objs.Total, "Unexpected number of filtered connections associated with the addressspace")
	assert.Equal(t, con2, objs.Connections[0], "Unexpected connection")
}

func TestQueryAddressSpaceConnectionOrder(t *testing.T) {
	r := newTestAddressSpaceResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	con1 := createConnection("host:1234", namespace, addressspace)
	con2 := createConnection("host:1235", namespace, addressspace)
	err := r.Cache.Add(con1, con2)
	assert.NoError(t, err)

	ash := &consolegraphql.AddressSpaceHolder{
		AddressSpace: v1beta1.AddressSpace{
			ObjectMeta: v1.ObjectMeta{
				Name:                       addressspace,
				Namespace:                  namespace,
			},
		},
	}

	orderby := "`$.ObjectMeta.Name` DESC"
	objs, err := r.AddressSpace_consoleapi_enmasse_io_v1beta1().Connections(context.TODO(), ash, nil, nil, nil, &orderby)
	assert.NoError(t, err)
	assert.Equal(t, 2, objs.Total, "Unexpected number of connections associated with the addressspace")
	assert.Equal(t, con2, objs.Connections[0], "Unexpected connection")
}

func TestQueryAddressSpaceAddress(t *testing.T) {
	r := newTestAddressSpaceResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	addr := createAddress(namespace, fmt.Sprintf("%s.addr1", addressspace))
	err := r.Cache.Add(addr)
	assert.NoError(t, err)

	ash := &consolegraphql.AddressSpaceHolder{
		AddressSpace: v1beta1.AddressSpace{
			ObjectMeta: v1.ObjectMeta{
				Name:                       addressspace,
				Namespace:                  namespace,
			},
		},
	}
	objs, err := r.AddressSpace_consoleapi_enmasse_io_v1beta1().Addresses(context.TODO(), ash, nil, nil, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 1, objs.Total, "Unexpected number of address associated with the addressspace")
}

func TestQueryAddressSpaceAddressFilter(t *testing.T) {
	r := newTestAddressSpaceResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	addr1 := createAddress(namespace, fmt.Sprintf("%s.addr1", addressspace))
	addr2 := createAddress(namespace, fmt.Sprintf("%s.addr2", addressspace))
	err := r.Cache.Add(addr1, addr2)
	assert.NoError(t, err)

	ash := &consolegraphql.AddressSpaceHolder{
		AddressSpace: v1beta1.AddressSpace{
			ObjectMeta: v1.ObjectMeta{
				Name:                       addressspace,
				Namespace:                  namespace,
			},
		},
	}
	filter := fmt.Sprintf("`$.ObjectMeta.Name` = '%s'", addr2.ObjectMeta.Name)
	objs, err := r.AddressSpace_consoleapi_enmasse_io_v1beta1().Addresses(context.TODO(), ash, nil, nil, &filter, nil)
	assert.NoError(t, err)
	assert.Equal(t, 1, objs.Total, "Unexpected number of address associated with the addressspace")
	assert.Equal(t, 1, objs.Total, "Unexpected number of filtered addresses associated with the addressspace")
	assert.Equal(t, addr2, objs.Addresses[0], "Unexpected connection")

}
