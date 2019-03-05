/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package controller

import (
	"github.com/enmasseproject/enmasse/pkg/controller/iotconfig"
	"github.com/enmasseproject/enmasse/pkg/util"
)

func init() {
	if util.IsModuleEnabled("IOT_CONFIG") {
		AddToManagerFuncs = append(AddToManagerFuncs, iotconfig.Add)
	}
}
