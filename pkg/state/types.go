/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
)

type StateKey struct {
	Name      string
	Namespace string
}

type StateManager interface {
	GetOrCreateInfra(infra *v1beta2.MessagingInfra) InfraState
	GetOrCreateTenant(tenant *v1beta2.MessagingTenant) TenantState
	BindTenantToInfra(tenant *v1beta2.MessagingTenant) error
	DeleteInfra(infra *v1beta2.MessagingInfra) error
	DeleteTenant(tenant *v1beta2.MessagingTenant) error
}

type InfraState interface {
	UpdateRouters(hosts []string)
	UpdateBrokers(hosts []string)
	GetSelector() *v1beta2.Selector
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
	// EnsureAddress()
	// TODO: Implement
	// EnsureEndpoint()
	BindInfra(key StateKey, state InfraState)
	GetInfra() InfraState
	GetStatus() TenantStatus
	Shutdown() error
}

type TenantStatus struct {
	Bound          bool
	InfraName      string
	InfraNamespace string
}
