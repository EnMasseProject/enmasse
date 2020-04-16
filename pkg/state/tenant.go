/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package state

import (
// v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
)

type tenant struct {
	infraKey   StateKey
	infraState InfraState
	// endpoints []v1beta2.MessagingEndpoint
	// addresses []
}

func (t *tenant) BindInfra(key StateKey, infra InfraState) {
	if t.infraState == nil {
		t.infraKey = key
		t.infraState = infra
	}
}

func (t *tenant) GetInfra() InfraState {
	return t.infraState
}

func (t *tenant) GetStatus() TenantStatus {
	return TenantStatus{
		Bound:          t.infraState != nil,
		InfraName:      t.infraKey.Name,
		InfraNamespace: t.infraKey.Namespace,
	}
}

func (t *tenant) Shutdown() error {
	// TODO: Delete all address, endpoint and connector configuration?
	return nil
}
