/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package agent

import (
	"encoding/json"
	"testing"
)

const rec = `
{
  "id": "6625",
  "addressSpace": "standard",
  "addressSpaceNamespace": "enmasse-infra",
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
      "accepted": 0,
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

func TestFromAgentConnection(t *testing.T) {
	target := make(map[string]interface{})
	err := json.Unmarshal([]byte(rec), &target)

	if err != nil {
		t.Fatal("Failed to convert agent json", err)
	}

	connection, e := FromAgentConnectionBody(target)
	if e != nil {
		t.Fatal("Failed to convert known good; ", e)
	}

	expected := "931018af-750c-5217-911b-6c42a99d5cdc"
	actual := connection.Id
	if connection.Uuid != expected {
		t.Fatalf("Unexpected connection id, expected %s, actual %s", expected, actual)
	}
}
