/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package resolvers

import (
	"context"
	"github.com/99designs/gqlgen/graphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/watchers"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"testing"
)

func newTestLinkResolver(t *testing.T) *Resolver {
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
	assert.NoError(t, err)

	resolver := Resolver{}
	resolver.Cache = objectCache
	resolver.MetricCache = metricCache
	return &resolver
}

func TestLinkConnection(t *testing.T) {
	r := newTestLinkResolver(t)

	uid := uuid.New().String()
	namespace := "mynamespace"
	addressspace := "myaddressspace"
	addr := &consolegraphql.Connection{
		TypeMeta: metav1.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      uid,
			UID:       types.UID(uid),
			Namespace: namespace,
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: addressspace,
		},
	}
	err := r.Cache.Add(addr)
	assert.NoError(t, err)

	obj := &consolegraphql.LinkSpec{
		Connection:   uid,
		AddressSpace: addressspace,
	}

	ctx := buildResolverContext(namespace)
	con, err := r.LinkSpec_consoleapi_enmasse_io_v1beta1().Connection(ctx, obj)
	assert.NoError(t, err)

	assert.Equal(t, uid, con.ObjectMeta.Name, "unexpected connection uid")
}

func TestLinkMetrics(t *testing.T) {
	r := newTestLinkResolver(t)
	addressspace := "myaddressspace"

	createMetric := func(namespace string, link string, metricName string, metricValue float64) *consolegraphql.Metric {
		return &consolegraphql.Metric{
			Kind:        "Link",
			Namespace:   namespace,
			AddressSpace: addressspace,
			Name:        link,
			MetricName:  metricName,
			MetricType:  "gauge",
			MetricValue: metricValue,
		}
	}

	linkuid := uuid.New().String()
	conuid := uuid.New().String()
	namespace := "mynamespace"
	addr1 := "myaddr"

	err := r.MetricCache.Add(createMetric(namespace, linkuid, "enmasse_messages_backlog", float64(100)))
	assert.NoError(t, err)

	link := &LinkConsoleapiEnmasseIoV1beta1{
		ObjectMeta: &metav1.ObjectMeta{
			Name:      linkuid,
			UID:       types.UID(linkuid),
			Namespace: namespace,
		},
		Spec: &consolegraphql.LinkSpec{
			Connection:   conuid,
			AddressSpace: addressspace,
			Address:      addr1,
			Role:         "sender",
		},
	}
	metrics, err := r.Link_consoleapi_enmasse_io_v1beta1().Metrics(context.TODO(), link)
	assert.NoError(t, err)

	assert.Equal(t, 1, len(metrics), "Unexpected number of metrics")

	backlogMetric := getMetric("enmasse_messages_backlog", metrics)
	assert.NotNil(t, backlogMetric, "Backlog metric is absent")
	assert.Equal(t, float64(100), backlogMetric.MetricValue, "Unexpected baclog metric value")

}

func buildResolverContext(namespace string) context.Context {
	link := &LinkConsoleapiEnmasseIoV1beta1{
		ObjectMeta: &metav1.ObjectMeta{
			Namespace: namespace,
		},
	}
	ctx := graphql.WithResolverContext(context.TODO(), &graphql.ResolverContext{
		Result: &link,
	})
	ctx = graphql.WithResolverContext(ctx, &graphql.ResolverContext{})
	ctx = graphql.WithResolverContext(ctx, &graphql.ResolverContext{})
	return ctx
}
