/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util"
)

func ensureAdapterAuthCredentials(status map[string]iotv1alpha1.AdapterStatus) error {

	for k, v := range status {
		if v.InterServicePassword == "" {
			pwd, err := util.GeneratePassword(64)
			if err != nil {
				return err
			}
			v.InterServicePassword = pwd
			status[k] = v
		}
	}

	return nil
}

// Ensure that we have exactly the require entries
func ensureAdapterStatus(currentStatus map[string]iotv1alpha1.AdapterStatus, names ...string) map[string]iotv1alpha1.AdapterStatus {

	var result = make(map[string]iotv1alpha1.AdapterStatus)

	// prefill

	for _, k := range names {
		result[k] = iotv1alpha1.AdapterStatus{}
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
