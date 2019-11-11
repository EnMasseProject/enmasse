/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package controller

import (
	address_space_controller "github.com/enmasseproject/enmasse/pkg/controller/address_space_controller"
	"github.com/enmasseproject/enmasse/pkg/util"
)

func init() {

	// add ourselves to the list of controllers

	if util.IsModuleEnabled("ADDRESS_SPACE_CONTROLLER") {
		AddToManagerFuncs = append(AddToManagerFuncs, address_space_controller.Add)
	}
}
