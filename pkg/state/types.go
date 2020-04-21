/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
)

/**
 * Manages clients for messaging infrastructure.
 */
type ClientManager interface {
	// Retrieve a client handle for communicating with messaging infrastructure. Client is thread
	// safe and shared with multiple threads.
	GetClient(infra *v1beta2.MessagingInfra) InfraClient

	// Remove client from manager. This will take care to call client.Shutdown() to cleanup client resources.
	DeleteClient(infra *v1beta2.MessagingInfra) error
}

/**
 * A client for performing changes and querying infrastructure.
 */
type InfraClient interface {
	// Synchronize connectors between router and broker hosts
	SyncConnectors(routers []string, brokers []string) ([]ConnectorStatus, error)
	// Stop and cleanup client resources
	Shutdown() error
}

type ConnectorStatus struct {
	Router    string
	Broker    string
	Connected bool
	Message   string
}
