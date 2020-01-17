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

func UpdateLinkMetrics(agentcon *AgentConnection, metrics []*consolegraphql.Metric, now time.Time, link *consolegraphql.Link) []*consolegraphql.Metric {

	var agentlinks []AgentAddressLink
	var rateMetricName string
	if link.Spec.Role == "sender" {
		agentlinks = agentcon.Senders
		rateMetricName = "enmasse_messages_in"
	} else {
		agentlinks = agentcon.Receivers
		rateMetricName = "enmasse_messages_out"
	}

	for _, l := range agentlinks {
		if l.Uuid == link.Name {
			var sm *consolegraphql.SimpleMetric
			sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_deliveries", "counter")
			sm.Update(float64(l.Deliveries), now)

			var rm *consolegraphql.RateCalculatingMetric
			rm, metrics = consolegraphql.FindOrCreateRateCalculatingMetric(metrics, rateMetricName, "gauge")
			rm.Update(float64(l.Deliveries), now)

			switch agentcon.AddressSpaceType {
			case "standard":
				sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_accepted", "counter")
				sm.Update(float64(l.Accepted), now)
				sm, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_modified", "counter")
				sm.Update(float64(l.Modified), now)
				sm, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_presettled", "counter")
				sm.Update(float64(l.Presettled), now)
				sm, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_unsettled", "counter")
				sm.Update(float64(l.Unsettled), now)
				sm, metrics = consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_undelivered", "counter")
				sm.Update(float64(l.Undelivered), now)
				sm, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_rejected", "counter")
				sm.Update(float64(l.Rejected), now)
				sm, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_released", "counter")
				sm.Update(float64(l.Released), now)

				// backlog - agent calculates this field to be the sum of undelivered/unsettled metrics
				backlog := 0
				for _, ld := range l.Links {
					backlog += ld.Backlog
				}
				sm, metrics =  consolegraphql.FindOrCreateSimpleMetric(metrics, "enmasse_messages_backlog", "counter")
				sm.Update(float64(backlog), now)

			case "brokered":
				// No address space specific types
			default:
				panic(fmt.Sprintf("unexpected address space type : %s", agentcon.AddressSpaceType))
			}
			return metrics
		}
	}

	return metrics
}
