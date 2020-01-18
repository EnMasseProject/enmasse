/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"testing"
	"time"
)

func newTestConnectionResolver(t *testing.T) *Resolver {
	objectCache, err := cache.CreateObjectCache()
	assert.NoError(t, err)

	resolver := Resolver{}
	resolver.Cache = objectCache
	return &resolver
}

func TestQueryConnection(t *testing.T) {
	r := newTestConnectionResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	con := createConnection("host:1234", namespace, addressspace)
	err := r.Cache.Add(con)
	assert.NoError(t, err)

	objs, err := r.Query().Connections(context.TODO(), nil, nil, nil, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of connections")

	assert.Equal(t, con.Spec, objs.Connections[0].Spec, "Unexpected connection spec")
	assert.Equal(t, con.ObjectMeta, objs.Connections[0].ObjectMeta, "Unexpected connection object meta")
}

func TestQueryConnectionFilter(t *testing.T) {
	r := newTestConnectionResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	con1 := createConnection("host:1234", namespace, addressspace)
	con2 := createConnection("host:1235", namespace, addressspace)
	err := r.Cache.Add(con1, con2)
	assert.NoError(t, err)

	filter := fmt.Sprintf("`$.ObjectMeta.Name` = '%s'", con1.ObjectMeta.Name)
	objs, err := r.Query().Connections(context.TODO(), nil, nil, &filter, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of connections")

	assert.Equal(t, con1.Spec, objs.Connections[0].Spec, "Unexpected connection spec")
	assert.Equal(t, con1.ObjectMeta, objs.Connections[0].ObjectMeta, "Unexpected connection object meta")
}

func TestQueryConnectionOrder(t *testing.T) {
	r := newTestConnectionResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	con1 := createConnection("host:1234", namespace, addressspace)
	con2 := createConnection("host:1235", namespace, addressspace)
	err := r.Cache.Add(con1, con2)
	assert.NoError(t, err)

	orderby := "`$.ObjectMeta.Name` DESC"
	objs, err := r.Query().Connections(context.TODO(), nil, nil, nil, &orderby)
	assert.NoError(t, err)

	expected := 2
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of connections")

	assert.Equal(t, con2.Spec, objs.Connections[0].Spec, "Unexpected connection spec")
	assert.Equal(t, con2.ObjectMeta, objs.Connections[0].ObjectMeta, "Unexpected connection object meta")
}

func TestQueryConnectionPagination(t *testing.T) {
	r := newTestConnectionResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	con1 := createConnection("host:1234", namespace, addressspace)
	con2 := createConnection("host:1235", namespace, addressspace)
	con3 := createConnection("host:1236", namespace, addressspace)
	con4 := createConnection("host:1237", namespace, addressspace)
	err := r.Cache.Add(con1, con2, con3, con4)
	assert.NoError(t, err)

	objs, err := r.Query().Connections(context.TODO(), nil, nil, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of connections")

	one := 1
	two := 2
	objs, err = r.Query().Connections(context.TODO(), nil, &one, nil,  nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of address spaces")
	assert.Equal(t, 3, len(objs.Connections), "Unexpected number of addresses in page")
	assert.Equal(t, con2.ObjectMeta, objs.Connections[0].ObjectMeta, "Unexpected addresses object meta")

	objs, err = r.Query().Connections(context.TODO(), &one, &two, nil,  nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of address spaces")
	assert.Equal(t, 1, len(objs.Connections), "Unexpected number of address spaces in page")
	assert.Equal(t, con3.ObjectMeta, objs.Connections[0].ObjectMeta, "Unexpected addresses object meta")
}

func TestQueryConnectionLinks(t *testing.T) {
	r := newTestConnectionResolver(t)
	con1 := uuid.New().String()
	con2 := uuid.New().String()
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	err := r.Cache.Add(createConnectionLink(namespace, addressspace, con1, "sender"), createConnectionLink(namespace, addressspace, con2, "sender"))
	assert.NoError(t, err)

	con := &consolegraphql.Connection{
		ObjectMeta: metav1.ObjectMeta{
			Name:      con1,
			UID:       types.UID(con1),
			Namespace: namespace,
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: addressspace,
		},
	}
	objs, err := r.Connection_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, nil, nil, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of links")
}

func TestQueryConnectionLinkFilter(t *testing.T) {
	r := newTestConnectionResolver(t)
	conuuid := uuid.New().String()
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	link1 := createConnectionLink(namespace, addressspace, conuuid, "sender")
	link2 := createConnectionLink(namespace, addressspace, conuuid, "sender")
	err := r.Cache.Add(link1, link2)
	assert.NoError(t, err)

	con := &consolegraphql.Connection{
		ObjectMeta: metav1.ObjectMeta{
			Name:      conuuid,
			UID:       types.UID(conuuid),
			Namespace: namespace,
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: addressspace,
		},
	}

	filter := fmt.Sprintf("`$.ObjectMeta.Name` = '%s'", link1.ObjectMeta.Name)
	objs, err := r.Connection_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, nil, &filter, nil)
	assert.NoError(t, err)

	expected := 1
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of links")
	assert.Equal(t, link1.Spec, objs.Links[0].Spec, "Unexpected link spec")
	assert.Equal(t, link1.ObjectMeta, objs.Links[0].ObjectMeta, "Unexpected link object meta")
}

func TestQueryConnectionLinkOrder(t *testing.T) {
	r := newTestConnectionResolver(t)
	conuuid := uuid.New().String()
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	link1 := createConnectionLink(namespace, addressspace, conuuid, "sender")
	link2 := createConnectionLink(namespace, addressspace, conuuid, "receiver")
	err := r.Cache.Add(link1, link2)
	assert.NoError(t, err)

	con := &consolegraphql.Connection{
		ObjectMeta: metav1.ObjectMeta{
			Name:      conuuid,
			UID:       types.UID(conuuid),
			Namespace: namespace,
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: addressspace,
		},
	}

	orderby := "`$.Spec.Role`"
	objs, err := r.Connection_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, nil, nil, &orderby)
	assert.NoError(t, err)

	expected := 2
	actual := objs.Total
	assert.Equal(t, expected, actual, "Unexpected number of links")
	assert.Equal(t, link2.Spec, objs.Links[0].Spec, "Unexpected link spec")
	assert.Equal(t, link2.ObjectMeta, objs.Links[0].ObjectMeta, "Unexpected link object meta")
}

func TestQueryConnectionLinkPaged(t *testing.T) {
	r := newTestConnectionResolver(t)
	conuuid := uuid.New().String()
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	link1 := createConnectionLink(namespace, addressspace, conuuid, "sender")
	link2 := createConnectionLink(namespace, addressspace, conuuid, "receiver")
	link3 := createConnectionLink(namespace, addressspace, conuuid, "receiver")
	link4 := createConnectionLink(namespace, addressspace, conuuid, "receiver")
	err := r.Cache.Add(link1, link2, link3, link4)
	assert.NoError(t, err)

	con := &consolegraphql.Connection{
		ObjectMeta: metav1.ObjectMeta{
			Name:      conuuid,
			UID:       types.UID(conuuid),
			Namespace: namespace,
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: addressspace,
		},
	}

	objs, err := r.Connection_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, nil, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of links")

	one := 1
	two := 2
	objs, err = r.Connection_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, &one, nil,  nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of links")
	assert.Equal(t, 3, len(objs.Links), "Unexpected number of links in page")

	objs, err = r.Connection_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, &one, &two, nil,  nil)
	assert.NoError(t, err)
	assert.Equal(t, 4, objs.Total, "Unexpected number of links")
	assert.Equal(t, 1, len(objs.Links), "Unexpected number of links in page")
}


func TestQueryConnectionMetrics(t *testing.T) {
	r := newTestConnectionResolver(t)
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	createMetric := func(namespace string, metricName string, metricValue float64) *consolegraphql.Metric {
		metric := consolegraphql.NewSimpleMetric(metricName, "gauge")
		metric.Update(metricValue, time.Now())
		return (*consolegraphql.Metric)(metric)
	}

	con := createConnection("host:1234", namespace, addressspace,
		createMetric(namespace, "enmasse_messages_in", float64(10)),
		createMetric(namespace, "enmasse_messages_out", float64(20)),
		createMetric(namespace, "enmasse_senders", float64(2)),
		createMetric(namespace, "enmasse_receivers", float64(1)))

	err := r.Cache.Add(con)
	assert.NoError(t, err)

	objs, err := r.Query().Connections(context.TODO(), nil, nil, nil, nil)
	assert.NoError(t, err)

	assert.Equal(t, 1, objs.Total, "Unexpected number of connections")

	metrics := objs.Connections[0].Metrics
	expected := 4
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
	messagesInMetric := getMetric("enmasse_messages_in", metrics)
	assert.NotNil(t, messagesInMetric, "Messages In metric is absent")
	value = messagesInMetric.Value
	assert.Equal(t, float64(10), value, "Unexpected messages in metric value")
	messagesOutMetric := getMetric("enmasse_messages_out", metrics)
	assert.NotNil(t, messagesOutMetric, "Messages In metric is absent")
	value = messagesOutMetric.Value
	assert.Equal(t, float64(20), value, "Unexpected messages out metric value")
}

func TestQueryConnectionLinkMetric(t *testing.T) {
	r := newTestConnectionResolver(t)
	conuuid := uuid.New().String()
	namespace := "mynamespace"
	addressspace := "myaddressspace"

	createMetric := func(namespace string, addr1 string, metricName string, metricValue float64) *consolegraphql.Metric {
		metric := consolegraphql.NewSimpleMetric(metricName, "gauge")
		metric.Update(metricValue, time.Now())
		return (*consolegraphql.Metric)(metric)
	}


	link := createConnectionLink(namespace, addressspace, conuuid, "sender",
		createMetric(namespace, "", "enmasse_messages_backlog", float64(100)))
	err := r.Cache.Add(link)
	assert.NoError(t, err)

	con := &consolegraphql.Connection{
		ObjectMeta: metav1.ObjectMeta{
			Name:      conuuid,
			UID:       types.UID(conuuid),
			Namespace: namespace,
		},
		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: addressspace,
		},
	}

	objs, err := r.Connection_consoleapi_enmasse_io_v1beta1().Links(context.TODO(), con, nil, nil, nil, nil)
	assert.NoError(t, err)
	assert.Equal(t, 1, objs.Total, "Unexpected number of links")

	metrics := objs.Links[0].Metrics
	assert.Equal(t, 1, len(metrics), "Unexpected number of metrics")

	backlogMetric := getMetric("enmasse_messages_backlog", metrics)
	assert.NotNil(t, backlogMetric, "Backlog metric is absent")
	value := backlogMetric.Value
	assert.Equal(t, float64(100), value, "Unexpected backlog metric value")

}

func createConnectionLink(namespace string, addressspace string, con string, role string, metrics ...*consolegraphql.Metric) *consolegraphql.Link {
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
		Metrics: metrics,
	}
}
