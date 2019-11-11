/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package controller

import (
	"github.com/enmasseproject/enmasse/pkg/controller/upgrader"
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
	upgrader, err := upgrader.New(m)
	if err != nil {
		return err
	}
	return upgrader.Upgrade()
}
