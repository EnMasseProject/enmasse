/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
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

func TestQueryAddressSpaceMetrics(t *testing.T) {
	r := newTestAddressSpaceResolver(t)
	as := &v1beta1.AddressSpace{
		TypeMeta: metav1.TypeMeta{
			Kind: "AddressSpace",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      "myaddrspace",
			Namespace: "mynamespace",
			UID:       types.UID(uuid.New().String()),
		},
	}
	c1 := &consolegraphql.Connection{
		TypeMeta: metav1.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      "host:1234",
			Namespace: "mynamespace",
			UID:       types.UID(uuid.New().String()),
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: "myaddrspace",
		},
	}
	c2 := &consolegraphql.Connection{
		TypeMeta: metav1.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      "host:1235",
			Namespace: "mynamespace",
			UID:       types.UID(uuid.New().String()),
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: "myaddrspace",
		},
	}
	differentAsCon := &consolegraphql.Connection{
		TypeMeta: metav1.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      "host:1236",
			Namespace: "mynamespace",
			UID:       types.UID(uuid.New().String()),
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: "myaddrspace1",
		},
	}
	err := r.Cache.Add(as, c1, c2, differentAsCon)
	assert.NoError(t, err)

	obj := &AddressSpaceConsoleapiEnmasseIoV1beta1{
		ObjectMeta: &metav1.ObjectMeta{
			Name:      "myaddrspace",
			Namespace: "mynamespace",
		},
	}

	metrics, err := r.AddressSpace_consoleapi_enmasse_io_v1beta1().Metrics(context.TODO(), obj)
	assert.NoError(t, err)

	expected := 2
	actual := len(metrics)
	assert.Equal(t, expected, actual, "Unexpected number of metrics")

	connectionMetric := getMetric("enmasse-connections", metrics)
	assert.NotNil(t, connectionMetric, "Connections metric is absent")

	expectedNumberConnections := float64(2)
	value, _, _ := connectionMetric.Value.GetValue()
	assert.Equal(t, expectedNumberConnections, value, "Unexpected connection metric")

	addressesMetric := getMetric("enmasse-addresses", metrics)
	assert.NotNil(t, addressesMetric, "Addresses metric is absent")
}
