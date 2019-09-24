/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package controller

import (
	deployer "github.com/enmasseproject/enmasse/pkg/controller/address_space_controller_deployer"
	"github.com/enmasseproject/enmasse/pkg/util"
)

func init() {

	// add ourselves to the list of controllers

	if util.IsModuleEnabled("ADDRESS_SPACE_CONTROLLER_DEPLOYER") {
		AddToManagerFuncs = append(AddToManagerFuncs, deployer.Add)
	}
}
