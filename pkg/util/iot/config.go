/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iot

import "github.com/enmasseproject/enmasse/pkg/util"

func GetIoTInfrastructureName() (string, error) {
	return util.GetEnvOrError("IOT_CONFIG_NAME")
}
