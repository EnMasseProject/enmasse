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
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"testing"
)

func newTestConnectionResolver(t *testing.T) *Resolver {
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
					"Connection": watchers.ConnectionIndexCreator,
					"Link":       watchers.ConnectionLinkIndexCreator,
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

func TestQueryConnection(t *testing.T) {
	r := newTestConnectionResolver(t)
	uid := uuid.New().String()
	con := &consolegraphql.Connection{
		TypeMeta: metav1.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name: uid,
			UID:  types.UID(uid),
		},
	}
	err := r.Cache.Add(con)
	assert.NoError(t, err)

	objs, err := r.Query().Connections(context.TODO(), nil, nil, nil, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of connections")

	assert.Equal(t, con.Spec, *objs.Connections[0].Spec, "Unexpected connection spec")
	assert.Equal(t, con.ObjectMeta, *objs.Connections[0].ObjectMeta, "Unexpected connection object meta")
}

func TestQueryConnectionLinks(t *testing.T) {
	r := newTestConnectionResolver(t)
	con1 := uuid.New().String()
	con2 := uuid.New().String()
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	createLink := func(con string) *consolegraphql.Link {
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
				Connection:   con,
				AddressSpace: addressspace,
			},
		}
	}

	err := r.Cache.Add(createLink(con1), createLink(con2))
	assert.NoError(t, err)

	con := &ConnectionConsoleapiEnmasseIoV1beta1{
		ObjectMeta: &metav1.ObjectMeta{
			Name:      con1,
			UID:       types.UID(con1),
			Namespace: namespace,
		},
		Spec: &consolegraphql.ConnectionSpec{
			AddressSpace: addressspace,
		},
	}
	objs, err := r.Connection_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, nil, nil, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of links")
}

func TestQueryConnectionMetrics(t *testing.T) {
	r := newTestConnectionResolver(t)
	con1 := uuid.New().String()
	con2 := uuid.New().String()
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	createLink := func(con string, role string) *consolegraphql.Link {
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
				Connection:   con,
				AddressSpace: addressspace,
				Role:         role,
			},
		}
	}

	createMetric := func(namespace string, con string, metricName string, metricValue float64) *consolegraphql.Metric {
		return &consolegraphql.Metric{
			Kind:         "Connection",
			Namespace:    namespace,
			AddressSpace: addressspace,
			Name:         con,
			MetricName:   metricName,
			MetricType:   "gauge",
			MetricValue:  metricValue,
		}
	}

	err := r.Cache.Add(createLink(con1, "sender"), createLink(con1, "sender"), createLink(con1, "receiver"), createLink(con2, "receiver"))
	assert.NoError(t, err)

	err = r.MetricCache.Add(createMetric(namespace, con1, "enmasse_messages_in", float64(10)), createMetric(namespace, con1, "enmasse_messages_out", float64(20)))
	assert.NoError(t, err)

	con := &ConnectionConsoleapiEnmasseIoV1beta1{
		ObjectMeta: &metav1.ObjectMeta{
			Name:      con1,
			UID:       types.UID(con1),
			Namespace: namespace,
		},
		Spec: &consolegraphql.ConnectionSpec{
			AddressSpace: addressspace,
		},
	}
	objs, err := r.Connection_consoleapi_enmasse_io_v1beta1().Metrics(context.TODO(), con)
	assert.NoError(t, err)

	expected := 4
	actual := len(objs)
	assert.Equal(t, expected, actual, "Unexpected number of metrics")

	sendersMetric := getMetric("enmasse_senders", objs)
	assert.NotNil(t, sendersMetric, "Senders metric is absent")
	assert.Equal(t, float64(2), sendersMetric.MetricValue, "Unexpected senders metric value")
	receiversMetric := getMetric("enmasse_receivers", objs)
	assert.NotNil(t, receiversMetric, "Receivers metric is absent")
	assert.Equal(t, float64(1), receiversMetric.MetricValue, "Unexpected receivers metric value")
	messagesInMetric := getMetric("enmasse_messages_in", objs)
	assert.NotNil(t, messagesInMetric, "Messages In metric is absent")
	assert.Equal(t, float64(10), messagesInMetric.MetricValue, "Unexpected messages in metric value")
	messagesOutMetric := getMetric("enmasse_messages_out", objs)
	assert.NotNil(t, messagesOutMetric, "Messages In metric is absent")
	assert.Equal(t, float64(20), messagesOutMetric.MetricValue, "Unexpected messages out metric value")
}
