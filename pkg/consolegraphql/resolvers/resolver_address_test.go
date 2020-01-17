/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"testing"
	"time"
)

func newTestAddressResolver(t *testing.T) *Resolver {
	objectCache := &cache.MemdbCache{}
	err := objectCache.Init(
		cache.IndexSpecifier{
			Name:    "id",
			Indexer: &cache.UidIndex{},
		},
		cache.IndexSpecifier{
			Name: "hierarchy",
			Indexer: &cache.HierarchyIndex{
				IndexCreators: map[string]cache.HierarchicalIndexCreator{
					"Address": watchers.AddressIndexCreator,
					"Link":    watchers.ConnectionLinkIndexCreator,
				},
			},
		}, cache.IndexSpecifier{
			Name:         "addressLinkHierarchy",
			AllowMissing: true,
			Indexer: &cache.HierarchyIndex{
				IndexCreators: map[string]cache.HierarchicalIndexCreator{
					"Link": watchers.AddressLinkIndexCreator,
				},
			},
		})
	assert.NoError(t, err)

	resolver := Resolver{}
	resolver.Cache = objectCache
	return &resolver
}

func TestQueryAddress(t *testing.T) {
	r := newTestAddressResolver(t)
	namespace := "mynamespace"
	addr := createAddress(namespace, "myaddrspace.myaddr")
	err := r.Cache.Add(addr)
	assert.NoError(t, err)

	objs, err := r.Query().Addresses(context.TODO(), nil, nil, nil, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of addresses")

	assert.Equal(t, addr.Spec, objs.Addresses[0].Spec, "Unexpected address spec")
	assert.Equal(t, addr.ObjectMeta, objs.Addresses[0].ObjectMeta, "Unexpected address object meta")
}

func TestQueryAddressFilter(t *testing.T) {
	r := newTestAddressResolver(t)
	namespace := "mynamespace"
	addr1 := createAddress(namespace, "myaddrspace.myaddr1")
	addr2 := createAddress(namespace, "myaddrspace.myaddr2")
	err := r.Cache.Add(addr1, addr2)
	assert.NoError(t, err)

	filter := fmt.Sprintf("`$.ObjectMeta.Name` = '%s'", addr1.ObjectMeta.Name)
	objs, err := r.Query().Addresses(context.TODO(), nil, nil, &filter, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of addresses")

	assert.Equal(t, addr1.Spec, objs.Addresses[0].Spec, "Unexpected address spec")
	assert.Equal(t, addr1.ObjectMeta, objs.Addresses[0].ObjectMeta, "Unexpected address object meta")
}

func TestQueryAddressOrder(t *testing.T) {
	r := newTestAddressResolver(t)
	namespace := "mynamespace"
	addr1 := createAddress(namespace, "myaddrspace.myaddr1")
	addr2 := createAddress(namespace, "myaddrspace.myaddr2")
	err := r.Cache.Add(addr1, addr2)
	assert.NoError(t, err)

	orderby := "`$.ObjectMeta.Name` DESC"
	objs, err := r.Query().Addresses(context.TODO(), nil, nil, nil, &orderby)
	assert.NoError(t, err)

	expected := 2
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of addresses")

	assert.Equal(t, addr2.Spec, objs.Addresses[0].Spec, "Unexpected address spec")
	assert.Equal(t, addr2.ObjectMeta, objs.Addresses[0].ObjectMeta, "Unexpected address object meta")
}

func TestQueryAddressPagination(t *testing.T) {
	r := newTestAddressResolver(t)
	namespace := "mynamespace"
	addr1 := createAddress(namespace, "myaddrspace.myaddr1")
	addr2 := createAddress(namespace, "myaddrspace.myaddr2")
	addr3 := createAddress(namespace, "myaddrspace.myaddr3")
	addr4 := createAddress(namespace, "myaddrspace.myaddr4")
	err := r.Cache.Add(addr1, addr2, addr3, addr4)
	assert.NoError(t, err)

	objs, err := r.Query().Addresses(context.TODO(), nil, nil, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of addresses")

	one := 1
	two := 2
	objs, err = r.Query().Addresses(context.TODO(), nil, &one, nil,  nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of addresses")
	assert.Equal(t, 3, len(objs.Addresses), "Unexpected number of addresses in page")
	assert.Equal(t, addr2.ObjectMeta, objs.Addresses[0].ObjectMeta, "Unexpected addresses object meta")

	objs, err = r.Query().Addresses(context.TODO(), &one, &two, nil,  nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of addresses")
	assert.Equal(t, 1, len(objs.Addresses), "Unexpected number of address in page")
	assert.Equal(t, addr3.ObjectMeta, objs.Addresses[0].ObjectMeta, "Unexpected addresses object meta")
}

func TestQueryAddressLinks(t *testing.T) {
	r := newTestAddressResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	addr1 := "myaddr"
	addr2 := "myaddr1"
	addressName := addressspace + "." + addr1

	addr := createAddress(namespace, addressName)

	err := r.Cache.Add(
		addr,
		createAddressLink(namespace, addressspace, addr1, "sender"),
		createAddressLink(namespace, addressspace, addr1, "sender"),
		createAddressLink(namespace, addressspace, addr1, "sender"),
		createAddressLink(namespace, addressspace, addr2, "sender"))
	assert.NoError(t, err)

	con := &consolegraphql.AddressHolder{
		Address: v1beta1.Address{
			ObjectMeta: metav1.ObjectMeta{
				Name:      addressName,
				UID:       types.UID(addr.UID),
				Namespace: namespace,
			},
			Spec: v1beta1.AddressSpec{
				AddressSpace: addressspace,
			},
		},
	}

	objs, err := r.Address_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, nil, nil, nil)
	assert.NoError(t, err)

	expected := 3
	actual := objs.Total
	assert.Equalf(t, expected, actual, "Unexpected number of links for address %s", addr1)
}

func TestQueryAddressLinkFilter(t *testing.T) {
	r := newTestAddressResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	addr1 := "myaddr"
	addressName := addressspace + "." + addr1

	addr := createAddress(namespace, addressName)

	link1 := createAddressLink(namespace, addressspace, addr1, "sender")
	link2 := createAddressLink(namespace, addressspace, addr1, "sender")
	err := r.Cache.Add(addr, link1, link2, )
	assert.NoError(t, err)

	con := &consolegraphql.AddressHolder{
		Address: v1beta1.Address{
			ObjectMeta: metav1.ObjectMeta{
				Name:      addressName,
				UID:       types.UID(addr.UID),
				Namespace: namespace,
			},
			Spec: v1beta1.AddressSpec{
				AddressSpace: addressspace,
			},
		},
	}

	filter := fmt.Sprintf("`$.ObjectMeta.Name` = '%s'", link1.ObjectMeta.Name)
	objs, err := r.Address_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, nil, &filter, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equalf(t, expected, actual, "Unexpected number of links for address %s", addr1)
	assert.Equal(t, link1.Spec, objs.Links[0].Spec, "Unexpected link spec")
	assert.Equal(t, link1.ObjectMeta, objs.Links[0].ObjectMeta, "Unexpected link object meta")
}

func TestQueryAddressLinkOrder(t *testing.T) {
	r := newTestAddressResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	addr1 := "myaddr"
	addressName := addressspace + "." + addr1

	addr := createAddress(namespace, addressName)

	link1 := createAddressLink(namespace, addressspace, addr1, "sender")
	link2 := createAddressLink(namespace, addressspace, addr1, "receiver")
	err := r.Cache.Add(addr, link1, link2, )
	assert.NoError(t, err)

	con := &consolegraphql.AddressHolder{
		Address: v1beta1.Address{
			ObjectMeta: metav1.ObjectMeta{
				Name:      addressName,
				UID:       types.UID(addr.UID),
				Namespace: namespace,
			},
			Spec: v1beta1.AddressSpec{
				AddressSpace: addressspace,
			},
		},
	}

	orderby := "`$.Spec.Role`"
	objs, err := r.Address_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, nil, nil, &orderby)
	assert.NoError(t, err)

	expected := 2
	actual := objs.Total
	assert.Equalf(t, expected, actual, "Unexpected number of links for address %s", addr1)
	assert.Equal(t, link2.Spec, objs.Links[0].Spec, "Unexpected link spec")
	assert.Equal(t, link2.ObjectMeta, objs.Links[0].ObjectMeta, "Unexpected link object meta")
}

func TestQueryAddressLinkPaginated(t *testing.T) {
	r := newTestAddressResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	addr1 := "myaddr"
	addressName := "myaddressspace." + addr1

	addr := createAddress(namespace, addressName)

	link1 := createAddressLink(namespace, addressspace, addr1, "sender")
	link2 := createAddressLink(namespace, addressspace, addr1, "sender")
	link3 := createAddressLink(namespace, addressspace, addr1, "sender")
	link4 := createAddressLink(namespace, addressspace, addr1, "sender")
	err := r.Cache.Add(addr, link1, link2, link3, link4)
	assert.NoError(t, err)

	con := &consolegraphql.AddressHolder{
		Address: v1beta1.Address{
			ObjectMeta: metav1.ObjectMeta{
				Name:      addressName,
				UID:       types.UID(addr.UID),
				Namespace: namespace,
			},
			Spec: v1beta1.AddressSpec{
				AddressSpace: addressspace,
			},
		},
	}

	objs, err := r.Address_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, nil, nil, nil)
	assert.NoError(t, err)
	assert.Equalf(t, 4, objs.Total, "Unexpected number of links for address %s", addr1)

	one := 1
	two := 2
	objs, err = r.Address_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, &one, nil,  nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of links")
	assert.Equal(t, 3, len(objs.Links), "Unexpected number of links in page")

	objs, err = r.Address_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, &one, &two, nil,  nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of links")
	assert.Equal(t, 1, len(objs.Links), "Unexpected number of links in page")

}

func TestQueryAddressMetrics(t *testing.T) {
	r := newTestAddressResolver(t)
	namespace := "mynamespace"
	addressName := "myaddressspace.myaddr"

	createMetric := func(namespace string, metricName string, metricValue float64) *consolegraphql.Metric {
		metric := consolegraphql.NewSimpleMetric(metricName, "gauge")
		metric.Update(metricValue, time.Now())
		return (*consolegraphql.Metric)(metric)
	}

	addr := createAddress(namespace, addressName,
		createMetric(namespace, "enmasse_messages_stored", float64(100)),
		createMetric(namespace, "enmasse_messages_in", float64(10)),
		createMetric(namespace, "enmasse_messages_out", float64(20)),
		createMetric(namespace, "enmasse_senders", float64(2)),
		createMetric(namespace, "enmasse_receivers", float64(1)),
	)

	err := r.Cache.Add(addr)
	assert.NoError(t, err)

	objs, err := r.Query().Addresses(context.TODO(), nil, nil, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 1, objs.Total, "Unexpected number of addresses")

	metrics := objs.Addresses[0].Metrics

	expected := 5
	actual := len(metrics)
	assert.Equal(t, expected, actual, "Unexpected number of metrics")

	sendersMetric := getMetric("enmasse_senders", metrics)
	assert.NotNil(t, sendersMetric, "Senders metric is absent")
	value := sendersMetric.Value
	assert.Equal(t, float64(2), value, "Unexpected senders metric value")
	receiversMetric := getMetric("enmasse_receivers", metrics)
	assert.NotNil(t, receiversMetric, "Receivers metric is absent")
	value = receiversMetric.Value
	assert.Equal(t, float64(1), value, "Unexpected receivers metric value")
	storedMetric := getMetric("enmasse_messages_stored", metrics)
	assert.NotNil(t, storedMetric, "Stored metric is absent")
	value = storedMetric.Value
	assert.Equal(t, float64(100), value, "Unexpected stored metric value")
}

func createAddressLink(namespace string, addressspace string, addr string, role string) *consolegraphql.Link {
	linkuid := uuid.New().String()
	return &consolegraphql.Link{
		TypeMeta: metav1.TypeMeta{
			Kind: "Link",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      linkuid,
			UID:       types.UID(linkuid),
			Namespace: namespace,
		},
		Spec: consolegraphql.LinkSpec{
			AddressSpace: addressspace,
			Address:      addr,
			Role:         role,
		},
	}
}
