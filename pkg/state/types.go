/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

type StateManager interface {
	GetOrCreateInfra(name string, namespace string) InfraState
	GetOrCreateTenant(name string, namespace string) TenantState
	DeleteInfra(name string, namespace string) error
	DeleteTenant(name string, namespace string) error
}

type InfraState interface {
	UpdateRouters(hosts []string)
	UpdateBrokers(hosts []string)
	GetStatus() (InfraStatus, error)
	Sync() error
	Shutdown() error
}

type InfraStatus struct {
	Connectors []ConnectorStatus
}

type ConnectorStatus struct {
	Router    string
	Broker    string
	Connected bool
	Message   string
}

type TenantState interface {
	// TODO: Implement
	EnsureAddress()
	// TODO: Implement
	EnsureEndpoint()
}
