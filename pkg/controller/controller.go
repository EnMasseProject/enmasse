/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package controller

import (
	"context"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/controller/upgrader"
	"github.com/enmasseproject/enmasse/pkg/state"
	"github.com/enmasseproject/enmasse/pkg/util"

	"sigs.k8s.io/controller-runtime/pkg/manager"
)

var AddToManagerFuncs []func(manager.Manager) error

func AddToManager(m manager.Manager) error {
	for _, f := range AddToManagerFuncs {
		if err := f(m); err != nil {
			return err
		}
	}
	return nil
}

func CheckUpgrade(m manager.Manager) error {
	if util.IsModuleEnabled("UPGRADER") && !util.IsModuleEnabled("MESSAGING_INFRA") {
		upgrader, err := upgrader.New(m)
		if err != nil {
			return err
		}
		return upgrader.Upgrade()
	} else {
		return nil
	}
}

func InitializeStateManager(ctx context.Context, m manager.Manager) error {
	stateManager := state.GetStateManager()
	client := m.GetAPIReader()

	if util.IsModuleEnabled("MESSAGING_INFRA") {

		infras := v1beta2.MessagingInfraList{}
		err := client.List(ctx, &infras)
		if err != nil {
			return err
		}

		for _, infra := range infras.Items {
			stateManager.GetOrCreateInfra(&infra)
		}

	}

	if util.IsModuleEnabled("MESSAGING_TENANT") {
		tenants := v1beta2.MessagingTenantList{}
		err := client.List(ctx, &tenants)
		if err != nil {
			return err
		}
		for _, tenant := range tenants.Items {
			stateManager.GetOrCreateTenant(&tenant)
		}
	}
	return nil
}
