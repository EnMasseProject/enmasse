/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"crypto/tls"
	"time"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
)

/**
 * Manages clients for messaging infrastructure.
 */
type ClientManager interface {
	// Retrieve a client handle for communicating with messaging infrastructure. Client is thread
	// safe and shared with multiple threads.
	GetClient(infra *v1beta2.MessagingInfrastructure) InfraClient

	// Remove client from manager. This will take care to call client.Shutdown() to cleanup client resources.
	DeleteClient(infra *v1beta2.MessagingInfrastructure) error
}

/**
 * Represents a Kubernetes host and corresponding pod IP
 */
type Host struct {
	Hostname string
	Ip       string
}

/**
 * A client for performing changes and querying infrastructure.
 */
type InfraClient interface {
	// Start any internal state management processes
	Start()
	// Synchronize all resources for infrastructure for the provided routers and brokers
	SyncAll(routers []Host, brokers []Host, tlsConfig *tls.Config) ([]ConnectorStatus, error)
	// Stop and cleanup client resources
	Shutdown() error
	// Schedule durable address for tenant
	ScheduleAddress(address *v1beta2.MessagingAddress, scheduler Scheduler) error
	// Synchronize address
	SyncAddress(address *v1beta2.MessagingAddress) error
	// Delete address
	DeleteAddress(address *v1beta2.MessagingAddress) error
	// Allocate endpoint ports
	AllocatePorts(endpoint *v1beta2.MessagingEndpoint, protocols []v1beta2.MessagingEndpointProtocol) error
	// Free endpoint ports
	FreePorts(endpoint *v1beta2.MessagingEndpoint)
	// Synchronize endpoint
	SyncEndpoint(endpoint *v1beta2.MessagingEndpoint) error
	// Synchronize endpoint
	DeleteEndpoint(endpoint *v1beta2.MessagingEndpoint) error
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
