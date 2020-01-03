/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"testing"
	time2 "time"
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

	metricCache := &cache.MemdbCache{}
	err = metricCache.Init(
		cache.IndexSpecifier{
			Name:    "id",
			Indexer: cache.MetricIndex(),
		},
	)

	resolver := Resolver{}
	resolver.Cache = objectCache
	resolver.MetricCache = metricCache
	return &resolver
}

func TestQueryAddress(t *testing.T) {
	r := newTestAddressResolver(t)
	addr := &v1beta1.Address{
		TypeMeta: metav1.TypeMeta{
			Kind: "Address",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name: "myaddrspace.myaddr",
			UID:  types.UID(uuid.New().String()),
		},
	}
	err := r.Cache.Add(addr)
	assert.NoError(t, err)

	objs, err := r.Query().Addresses(context.TODO(), nil, nil, nil, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of addresses")

	assert.Equal(t, addr.Spec, *objs.Addresses[0].Spec, "Unexpected address spec")
	assert.Equal(t, addr.ObjectMeta, *objs.Addresses[0].ObjectMeta, "Unexpected address object meta")
}

func TestQueryAddressLinks(t *testing.T) {
	r := newTestAddressResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	addruid := types.UID(uuid.New().String())
	addr1 := "myaddr"
	addr2 := "myaddr1"
	addressName := "myaddressspace." + addr1

	addr := &v1beta1.Address{
		TypeMeta: metav1.TypeMeta{
			Kind: "Address",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      addressName,
			UID:       addruid,
			Namespace: namespace,
		},
	}

	createLink := func(addr string) *consolegraphql.Link {
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
			},
		}
	}

	err := r.Cache.Add(addr, createLink(addr1), createLink(addr1), createLink(addr1), createLink(addr2))
	assert.NoError(t, err)

	con := &AddressConsoleapiEnmasseIoV1beta1{
		ObjectMeta: &metav1.ObjectMeta{
			Name:      addressName,
			UID:       types.UID(addruid),
			Namespace: namespace,
		},
		Spec: &v1beta1.AddressSpec{
			AddressSpace: addressspace,
		},
	}

	objs, err := r.Address_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, nil, nil, nil)
	assert.NoError(t, err)

	expected := 3
	actual := objs.Total
	assert.Equalf(t, expected, actual, "Unexpected number of links for address %s", addr1)
}

func TestQueryAddressMetrics(t *testing.T) {
	r := newTestAddressResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	addr1 := "myaddr"
	addr2 := "myaddr1"
	addressName := "myaddressspace." + addr1

	createLink := func(addr string, role string) *consolegraphql.Link {
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

	createMetric := func(namespace string, addr1 string, metricName string, metricValue float64) *consolegraphql.Metric {
		return &consolegraphql.Metric{
			Kind:         "Address",
			Namespace:    namespace,
			AddressSpace: addressspace,
			Name:         addr1,
			Value:        consolegraphql.NewSimpleMetricValue(metricName, "gauge", float64(metricValue), "", time2.Now()),
		}
	}

	addruid := types.UID(uuid.New().String())
	addr := &v1beta1.Address{
		TypeMeta: metav1.TypeMeta{
			Kind: "Address",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      addressName,
			UID:       addruid,
			Namespace: namespace,
		},
	}

	err := r.MetricCache.Add(createMetric(namespace, addr1, "enmasse_messages_stored", float64(100)),
		createMetric(namespace, addr1, "enmasse_messages_in", float64(10)),
		createMetric(namespace, addr1, "enmasse_messages_out", float64(20)),
	)
	assert.NoError(t, err)

	err = r.Cache.Add(addr, createLink(addr1, "sender"), createLink(addr1, "sender"), createLink(addr1, "receiver"), createLink(addr2, "receiver"))
	assert.NoError(t, err)

	con := &AddressConsoleapiEnmasseIoV1beta1{
		ObjectMeta: &metav1.ObjectMeta{
			Name:      addressName,
			UID:       types.UID(addruid),
			Namespace: namespace,
		},
		Spec: &v1beta1.AddressSpec{
			AddressSpace: addressspace,
		},
	}
	objs, err := r.Address_consoleapi_enmasse_io_v1beta1().Metrics(context.TODO(), con)
	assert.NoError(t, err)

	expected := 5
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of metrics")

	sendersMetric := getMetric("enmasse_senders", objs)
	assert.NotNil(t, sendersMetric, "Senders metric is absent")
	value, _, _ := sendersMetric.Value.GetValue()
	assert.Equal(t, float64(2), value, "Unexpected senders metric value")
	receiversMetric := getMetric("enmasse_receivers", objs)
	assert.NotNil(t, receiversMetric, "Receivers metric is absent")
	value, _, _ = receiversMetric.Value.GetValue()
	assert.Equal(t, float64(1), value, "Unexpected receivers metric value")
	storedMetric := getMetric("enmasse_messages_stored", objs)
	assert.NotNil(t, storedMetric, "Stored metric is absent")
	value, _, _ = storedMetric.Value.GetValue()
	assert.Equal(t, float64(100), value, "Unexpected stored metric value")
}
