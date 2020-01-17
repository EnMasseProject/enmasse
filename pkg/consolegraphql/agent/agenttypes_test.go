/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package agent

import (
	"encoding/json"
	"github.com/stretchr/testify/assert"
	"testing"
)

const connection = `
{
  "id": "6625",
  "addressSpace": "standard",
  "addressSpaceNamespace": "enmasse-infra",
  "addressSpaceType": "standard",
  "uuid": "931018af-750c-5217-911b-6c42a99d5cdc",
  "host": "172.17.0.1:54682",
  "container": "af63b689-b747-7d44-97ed-2a85bbbdd5fb",
  "properties": {},
  "encrypted": true,
  "sasl_mechanism": "PLAIN",
  "user": "56dab160-b578-47e2-9cfa-f3cecb999796",
  "messages_in": 0,
  "messages_out": 0,
  "outcomes": {
    "ingress": {
      "accepted": 1,
      "released": 0,
      "rejected": 0,
      "modified": 0,
      "unsettled": 0,
      "presettled": 0,
      "undelivered": 0,
      "links": []
    },
    "egress": {
      "accepted": 0,
      "released": 0,
      "rejected": 0,
      "modified": 0,
      "unsettled": 0,
      "presettled": 0,
      "undelivered": 2,
      "links": [
        {
          "identity": "13236",
          "name": "517496d8-d604-9e4d-8477-8f3173600e55",
          "operStatus": "up",
          "adminStatus": "enabled",
          "deliveryCount": 0,
          "capacity": 250,
          "backlog": 0,
          "routerName": "qdrouterd-4f5fcdf-0",
          "clientName": "af63b689-b747-7d44-97ed-2a85bbbdd5fb",
          "acceptedCount": 0,
          "releasedCount": 0,
          "rejectedCount": 0,
          "modifiedCount": 0,
          "unsettledCount": 0,
          "presettledCount": 0,
          "undeliveredCount": 0,
          "lastUpdated": 1573662606300
        }
      ]
    }
  },
  "senders": [],
  "receivers": [
    {
      "address": "myqueue1",
      "name": "517496d8-d604-9e4d-8477-8f3173600e55",
      "uuid": "39c662ff-9872-53b6-a776-dc2319f3c63d",
      "accepted": 0,
      "released": 0,
      "rejected": 0,
      "modified": 0,
      "unsettled": 0,
      "presettled": 0,
      "undelivered": 0,
      "links": [
        {
          "identity": "13236",
          "name": "517496d8-d604-9e4d-8477-8f3173600e55",
          "operStatus": "up",
          "adminStatus": "enabled",
          "deliveryCount": 0,
          "capacity": 250,
          "backlog": 0,
          "acceptedCount": 0,
          "releasedCount": 0,
          "rejectedCount": 0,
          "modifiedCount": 0,
          "unsettledCount": 0,
          "presettledCount": 0,
          "undeliveredCount": 0,
          "lastUpdated": 1573662606300
        }
      ],
      "deliveries": 0
    }
  ]
}
`

const address = `{
  "address": "myqueue1",
  "addressSpace": "standard",
  "addressSpaceNamespace": "enmasse-infra",
  "forwarders": [],
  "plan": "standard-small-queue",
  "type": "queue",
  "status": {
    "brokerStatuses": [
      {
        "clusterId": "broker-4f5fcdf-f0pv",
        "containerId": "broker-4f5fcdf-f0pv-0",
        "state": "Active"
      }
    ],
    "forwarders": [],
    "isReady": true,
    "messages": [],
    "phase": "Active",
    "planStatus": {
      "name": "standard-small-queue",
      "partitions": 1,
      "resources": {
        "broker": 0.01,
        "router": 0.001
      }
    }
  },
  "name": "standard.myqueue1",
  "allocated_to": [
    {
      "clusterId": "broker-4f5fcdf-f0pv",
      "containerId": "broker-4f5fcdf-f0pv-0",
      "state": "Active"
    }
  ],
  "depth": 350,
  "dlq_depth": 0,
  "shards": [
    {
      "name": "broker-4f5fcdf-f0pv-0",
      "type": "queue",
      "id": 34,
      "address": "myqueue1",
      "filter": 0,
      "rate": 0,
      "durable": true,
      "paused": false,
      "temporary": false,
      "purgeOnNoConsumers": false,
      "maxConsumers": -1,
      "autoCreated": false,
      "user": 0,
      "routingType": "ANYCAST",
      "deliverDeliver": false,
      "exclusive": false,
      "lastValue": false,
      "scheduledCount": 0,
      "messages": 350,
      "consumers": 1,
      "enqueued": 350,
      "delivering": 0,
      "acknowledged": 1,
      "killed": 0
    }
  ],
  "senders": 1,
  "receivers": 0,
  "propagated": 100,
  "messages_in": 350,
  "messages_out": 700,
  "outcomes": {
    "ingress": {
      "accepted": 50,
      "released": 0,
      "rejected": 0,
      "modified": 0,
      "unsettled": 0,
      "presettled": 0,
      "undelivered": 0,
      "links": [
        {
          "identity": "13675",
          "name": "ca95b223-b4e1-d043-b938-6c4ffb99e62a",
          "operStatus": "up",
          "adminStatus": "enabled",
          "deliveryCount": 50,
          "capacity": 250,
          "backlog": 0,
          "routerName": "qdrouterd-4f5fcdf-0",
          "clientName": "d4d8175a-e02f-3a49-a570-3154cfcbda23",
          "acceptedCount": 50,
          "releasedCount": 0,
          "rejectedCount": 0,
          "modifiedCount": 0,
          "unsettledCount": 0,
          "presettledCount": 0,
          "undeliveredCount": 0,
          "lastUpdated": 1576245349726
        }
      ]
    },
    "egress": {
      "accepted": 0,
      "released": 0,
      "rejected": 0,
      "modified": 0,
      "unsettled": 0,
      "presettled": 0,
      "undelivered": 0,
      "links": []
    }
  },
  "waypoint": true
}
`

func TestFromAgentConnection(t *testing.T) {
	target := make(map[string]interface{})
	err := json.Unmarshal([]byte(connection), &target)
	assert.NoError(t, err)

	connection, err := FromAgentConnectionBody(target)
	assert.NoError(t, err)

	expected := &AgentConnection{
		Id:                    "6625",
		Uuid:                  "931018af-750c-5217-911b-6c42a99d5cdc",
		Host:                  "172.17.0.1:54682",
		Container:             "af63b689-b747-7d44-97ed-2a85bbbdd5fb",
		AddressSpace:          "standard",
		AddressSpaceNamespace: "enmasse-infra",
		AddressSpaceType:      "standard",
		Properties:            map[string]string{},
		Encrypted:             true,
		SaslMechanism:         "PLAIN",
		User:                  "56dab160-b578-47e2-9cfa-f3cecb999796",
		CreationTimestamp:     0,
		MessagesIn:            0,
		MessagesOut:           0,
		Outcomes: map[string]AgentOutcome{
			"egress": {
				Accepted:    0,
				Released:    0,
				Rejected:    0,
				Modified:    0,
				Unsettled:   0,
				Presettled:  0,
				Undelivered: 2,
				Links: []AgentLink{
					{
						Identity:         "13236",
						Name:             "517496d8-d604-9e4d-8477-8f3173600e55",
						OperStatus:       "up",
						AdminStatus:      "enabled",
						DeliveryCount:    0,
						Capacity:         250,
						Backlog:          0,
						RouterName:       "qdrouterd-4f5fcdf-0",
						ClientName:       "af63b689-b747-7d44-97ed-2a85bbbdd5fb",
						AcceptedCount:    0,
						ReleasedCount:    0,
						ModifiedCount:    0,
						UnsettledCount:   0,
						PresettledCount:  0,
						UndeliveredCount: 0,
						LastUpdated:      1573662606300,
					},
				},
			},
			"ingress": {
				Accepted:    1,
				Released:    0,
				Rejected:    0,
				Modified:    0,
				Unsettled:   0,
				Presettled:  0,
				Undelivered: 0,
				Links:       []AgentLink{},
			},
		},
		Senders: []AgentAddressLink{},
		Receivers: []AgentAddressLink{
			{
				Address:     "myqueue1",
				Name:        "517496d8-d604-9e4d-8477-8f3173600e55",
				Uuid:        "39c662ff-9872-53b6-a776-dc2319f3c63d",
				Accepted:    0,
				Released:    0,
				Rejected:    0,
				Modified:    0,
				Unsettled:   0,
				Presettled:  0,
				Undelivered: 0,
				Links: []AgentLink{
					{
						Identity:         "13236",
						Name:             "517496d8-d604-9e4d-8477-8f3173600e55",
						OperStatus:       "up",
						AdminStatus:      "enabled",
						DeliveryCount:    0,
						Capacity:         250,
						Backlog:          0,
						RouterName:       "",
						ClientName:       "",
						AcceptedCount:    0,
						ReleasedCount:    0,
						ModifiedCount:    0,
						UnsettledCount:   0,
						PresettledCount:  0,
						UndeliveredCount: 0,
						LastUpdated:      1573662606300,
					},
				},
			},
		},
	}

	assert.Equal(t, expected, connection)
}

func TestFromAgentAddress(t *testing.T) {
	target := make(map[string]interface{})
	err := json.Unmarshal([]byte(address), &target)
	assert.NoError(t, err)

	address, err := FromAgentAddressBody(target)
	assert.NoError(t, err)

	expected := &AgentAddress{
		Name:                  "standard.myqueue1",
		Address:               "myqueue1",
		Depth:                 350,
		MessagesIn:            350,
		MessagesOut:           700,
		Senders:               1,
		Receivers:             0,
		AddressSpace:          "standard",
		AddressSpaceNamespace: "enmasse-infra",
		Shards: []AgentAddressShards {
			{"broker-4f5fcdf-f0pv-0", 1, 350, 1, 0},
		},
	}

	assert.Equal(t, expected, address)
}
