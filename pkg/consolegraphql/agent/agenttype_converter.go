/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package agent

import (
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"time"
)

type MetricCreator = func (proto *consolegraphql.Metric) (*consolegraphql.Metric, error)

func ToConnectionK8Style(connection *AgentConnection, metricCreator MetricCreator) (*consolegraphql.Connection, []*consolegraphql.Link, []*consolegraphql.Metric) {

	links := make([]*consolegraphql.Link, 0)

	con := &consolegraphql.Connection{
		TypeMeta: metav1.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:              connection.Uuid,
			Namespace:         connection.AddressSpaceNamespace,
			UID:               types.UID(connection.Uuid),
			CreationTimestamp: metav1.Unix(connection.CreationTimestamp, 0),
		},

		Spec: consolegraphql.ConnectionSpec{
			AddressSpace: connection.AddressSpace,
			Hostname:     connection.Host,
			ContainerId:  connection.Container,
			Protocol:     "amqp",
			Encrypted:    connection.Encrypted,
			Properties:   connection.Properties,
		},
	}
	if connection.Encrypted {
		con.Spec.Protocol = "amqps"
	}

	metrics := make([]*consolegraphql.Metric, 0)

	lastUpdated := time.Now()
	if connection.LastUpdated > 0 {
		lastUpdated = millisToTime(connection.LastUpdated)
	}

	messagesInMetric, err := metricCreator(&consolegraphql.Metric{
		Kind:         "Connection",
		Namespace:    connection.AddressSpaceNamespace,
		AddressSpace: connection.AddressSpace,
		Name:         connection.Uuid,
		Value: consolegraphql.NewRateCalculatingMetricValue("enmasse_messages_in", "gauge", ""),
	})
	if err != nil {
		panic(err)
	}
	messagesInMetric.Value.SetValue(float64(connection.MessagesIn), lastUpdated)

	messagesOutMetric, err := metricCreator(&consolegraphql.Metric{
		Kind:         "Connection",
		Namespace:    connection.AddressSpaceNamespace,
		AddressSpace: connection.AddressSpace,
		Name:         connection.Uuid,
		Value: consolegraphql.NewRateCalculatingMetricValue("enmasse_messages_out", "gauge", ""),
	})
	messagesOutMetric.Value.SetValue(float64(connection.MessagesOut), lastUpdated)

	metrics = append(metrics, messagesInMetric, messagesOutMetric)


	for _, sender := range connection.Senders {
		link, sender_metrics := convertAgentLink(sender, "sender", connection, metricCreator)
		links = append(links, link)
		metrics = append(metrics, sender_metrics...)
	}
	for _, receiver := range connection.Receivers {
		link, receiver_metrics := convertAgentLink(receiver, "receiver", connection, metricCreator)
		links = append(links, link)
		metrics = append(metrics, receiver_metrics...)
	}

	return con, links, metrics
}

func convertAgentLink(l AgentAddressLink, role string, connection *AgentConnection, metricCreator MetricCreator) (*consolegraphql.Link, []*consolegraphql.Metric) {
	link := &consolegraphql.Link{
		TypeMeta: metav1.TypeMeta{
			Kind: "Link",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      l.Uuid,
			Namespace: connection.AddressSpaceNamespace,
			UID:       types.UID(l.Uuid),
		},
		Spec: consolegraphql.LinkSpec{
			Connection:   connection.Uuid,
			AddressSpace: connection.AddressSpace,
			Address:      l.Address,
			Role:         role,
		},
	}

	createMetric := func(name string, value int) *consolegraphql.Metric {
		return &consolegraphql.Metric{
			Kind:         "Link",
			Namespace:    connection.AddressSpaceNamespace,
			AddressSpace: connection.AddressSpace,
			Name:         l.Uuid,
			ConnectionName: &connection.Uuid,

			Value: consolegraphql.NewSimpleMetricValue(name, "counter", float64(value), "", millisToTime(connection.LastUpdated)),
		}
	}

	metrics := make([]*consolegraphql.Metric, 0)
	switch connection.AddressSpaceType {
	case "standard":
		metrics = append(metrics,
			createMetric("enmasse_accepted", l.Accepted),
			createMetric("enmasse_modified", l.Modified),
			createMetric("enmasse_presettled", l.Presettled),
			createMetric("enmasse_unsettled", l.Unsettled),
			createMetric("enmasse_undelivered", l.Undelivered),
			createMetric("enmasse_rejected", l.Rejected),
			createMetric("enmasse_released", l.Released),
			createMetric("enmasse_deliveries", l.Deliveries),
		)
	case "brokered":
		metrics = append(metrics,
			createMetric("enmasse_deliveries", l.Deliveries),
		)
	default:
		panic(fmt.Sprintf("unexpected address space type : %s", connection.AddressSpaceType))

	}

	return link, metrics
}

func millisToTime(ms int64) time.Time {
	return  time.Unix(0, ms* int64(time.Millisecond))
}
