/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"crypto/tls"
	"time"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/state/common"
)

/**
 * Manages clients for messaging infrastructure.
 */
type ClientManager interface {
	// Retrieve a client handle for communicating with messaging infrastructure. Client is thread
	// safe and shared with multiple threads.
	GetClient(infra *v1.MessagingInfrastructure) InfraClient

	// Remove client from manager. This will take care to call client.Shutdown() to cleanup client resources.
	DeleteClient(infra *v1.MessagingInfrastructure) error
}

/**
 * A client for performing changes and querying infrastructure.
 */
type InfraClient interface {
	// Start any internal state management processes
	Start()
	// Synchronize all resources for infrastructure for the provided routers and brokers
	SyncAll(routers []common.Host, brokers []common.Host, tlsConfig *tls.Config) ([]ConnectorStatus, error)
	// Stop and cleanup client resources
	Shutdown() error
	// Check if broker is in use by any address or project and delete it
	DeleteBroker(host string) error
	// Schedule project
	ScheduleProject(project *v1.MessagingProject) error
	// Schedule durable address for project
	ScheduleAddress(address *v1.MessagingAddress) error
	// Synchronize address
	SyncAddress(address *v1.MessagingAddress) error
	// Delete address
	DeleteAddress(address *v1.MessagingAddress) error
	// Synchronize project
	SyncProject(project *v1.MessagingProject) error
	// Delete project
	DeleteProject(project *v1.MessagingProject) error
	// Allocate endpoint ports
	AllocatePorts(endpoint *v1.MessagingEndpoint, protocols []v1.MessagingEndpointProtocol) error
	// Free endpoint ports
	FreePorts(endpoint *v1.MessagingEndpoint)
	// Synchronize endpoint
	SyncEndpoint(endpoint *v1.MessagingEndpoint) error
	// Synchronize endpoint
	DeleteEndpoint(endpoint *v1.MessagingEndpoint) error
}

/**
 * Interface for retrieving current time.
 */
type Clock interface {
	Now() time.Time
}

type ConnectorStatus struct {
	Router    string
	Broker    string
	Connected bool
	Message   string
}

type EndpointStatus struct {
	Port string
}
