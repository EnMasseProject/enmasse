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

func ToConnectionK8Style(connection *AgentConnection) (*consolegraphql.Connection, []*consolegraphql.Link) {

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


	for _, sender := range connection.Senders {
		link := convertAgentLink(sender, "sender", connection)
		links = append(links, link)
	}
	for _, receiver := range connection.Receivers {
		link := convertAgentLink(receiver, "receiver", connection)
		links = append(links, link)
	}

	return con, links
}

func convertAgentLink(l AgentAddressLink, role string, connection *AgentConnection) *consolegraphql.Link {
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
	return link
}

func millisToTime(ms int64) time.Time {
	return  time.Unix(0, ms* int64(time.Millisecond))
}

func InsertOrUpdateMetric(existing []*consolegraphql.Metric, metricName string) ([]*consolegraphql.Metric, *consolegraphql.Metric) {

	for _, m := range existing {
		if m.Name == metricName {
			return existing, m
		}
	}

	m := &consolegraphql.Metric{
	}
	existing = append(existing, m)
	return existing, m

}

func UpdateLinkMetrics(agentcon *AgentConnection, metrics []*consolegraphql.Metric, now time.Time, link *consolegraphql.Link) []*consolegraphql.Metric {

	var agentlinks []AgentAddressLink
	if link.Spec.Role == "sender" {
		agentlinks = agentcon.Senders
	} else {
		agentlinks = agentcon.Receivers
	}

	for _, l := range agentlinks {
		if l.Uuid == link.Name {
			switch agentcon.AddressSpaceType {
			case "standard":
				m, metrics := consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_accepted", "counter")
				m.Update(float64(l.Accepted), now)
				m, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_modified", "counter")
				m.Update(float64(l.Modified), now)
				m, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_presettled", "counter")
				m.Update(float64(l.Presettled), now)
				m, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_unsettled", "counter")
				m.Update(float64(l.Unsettled), now)
				m, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_undelivered", "counter")
				m.Update(float64(l.Undelivered), now)
				m, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_rejected", "counter")
				m.Update(float64(l.Rejected), now)
				m, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_released", "counter")
				m.Update(float64(l.Released), now)
				m, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_deliveries", "counter")
				m.Update(float64(l.Deliveries), now)
				return metrics
			case "brokered":
				m, metrics := consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_deliveries", "counter")
				m.Update(float64(l.Deliveries), now)
				return metrics
			default:
				panic(fmt.Sprintf("unexpected address space type : %s", agentcon.AddressSpaceType))

			}
		}
	}

	return metrics
}
