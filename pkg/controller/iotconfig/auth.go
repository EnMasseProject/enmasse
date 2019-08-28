/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util"
)

func initConfigStatus(config *iotv1alpha1.IoTConfig) error {

	status := config.Status.Adapters

	for k, v := range status {
		v.Enabled = findAdapter(k).IsEnabled(config)
		if v.Enabled && v.InterServicePassword == "" {
			pwd, err := util.GeneratePassword(64)
			if err != nil {
				return err
			}
			v.InterServicePassword = pwd
		}
		if !v.Enabled {
			v.InterServicePassword = ""
		}
		status[k] = v
	}

	return nil
}

// Ensure that we have exactly the require entries
func ensureAdapterStatus(currentStatus map[string]iotv1alpha1.AdapterStatus) map[string]iotv1alpha1.AdapterStatus {

	var result = make(map[string]iotv1alpha1.AdapterStatus)

	// prefill

	for _, k := range adapters {
		result[k.Name] = iotv1alpha1.AdapterStatus{}
	}

	if currentStatus == nil {
		return result
	}

	// overwrite with existing

	for k, v := range currentStatus {
		// only overwrite if it is existing
		if _, ok := result[k]; ok {
			result[k] = v
		}
	}

	return result

}
