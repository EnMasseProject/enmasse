/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package agent

import (
	"encoding/json"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
)

type AgentConnectionEventType string

const (
	AgentConnectionEventTypeRestart = "restart"
	AgentConnectionEventTypeAdd     = "add"
	AgentConnectionEventTypeDelete  = "delete"
)

type AgentConnectionEvent struct {
	InfraUuid             string
	AddressSpace          string
	AddressSpaceNamespace string
	Type                  AgentConnectionEventType
	Object                *AgentConnection
}

type AgentConnection struct {
	Id                string
	Uuid              string
	Host              string
	Container         string
	Properties        map[string]string
	Encrypted         bool
	SaslMechanism     string `json:"sasl_mechanism"`
	User              string
	CreationTimestamp int64 `json:"creationTimestamp"`
	MessagesIn        int   `json:"messages_in"`
	MessagesOut       int   `json:"messages_out"`
}

func FromAgentConnectionBody(agentConnectionMap map[string]interface{}) (*AgentConnection, error) {
	if props, exists := agentConnectionMap["properties"]; exists {
		if p, ok := props.(map[interface{}]interface{}); ok && len(p) == 0 {
			delete(agentConnectionMap, "properties")
		}
	}
	bytes, e := json.Marshal(agentConnectionMap)
	if e != nil {
		return nil, e
	}

	m := AgentConnection{}
	err := json.Unmarshal(bytes, &m)
	if err == nil {
		return &m, nil
	} else {
		return nil, err
	}
}

func ToK8Style(connection *AgentConnection) *consolegraphql.Connection {

	con := consolegraphql.Connection{
		TypeMeta: v1.TypeMeta{
			Kind: "Connection",
		},
		ObjectMeta: v1.ObjectMeta{
			Name:              connection.Uuid,
			UID:               types.UID(connection.Uuid),
			CreationTimestamp: v1.Unix(connection.CreationTimestamp, 0),
		},

		Spec: consolegraphql.ConnectionSpec{
			Hostname:    connection.Host,
			ContainerId: connection.Container,
			Protocol:    "amqp",
			Properties:  connection.Properties,
		},
	}
	if connection.Encrypted {
		con.Spec.Protocol = "amqps"
	}
	return &con
}
