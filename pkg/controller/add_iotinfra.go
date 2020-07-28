/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package controller

import (
	"github.com/enmasseproject/enmasse/pkg/controller/iotinfra"
	"github.com/enmasseproject/enmasse/pkg/util"
)

func init() {
	if util.IsModuleEnabled("IOT_INFRASTRUCTURE") {
		AddToManagerFuncs = append(AddToManagerFuncs, iotinfra.Add)
	}
}
