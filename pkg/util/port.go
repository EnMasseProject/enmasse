/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"fmt"
	v1 "k8s.io/api/core/v1"
)

func GetPortForService(servicePorts []v1.ServicePort, name string) (*int32, error) {
	for _, v := range servicePorts {
		if v.Name == name {
			return &v.Port, nil
		}
	}
	return nil, fmt.Errorf("failed to find service port with name '%s'", name)
}
