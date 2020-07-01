/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

func (r *ReconcileIoTProject) findIoTProjectsByPredicate(predicate func(project *iotv1alpha1.IoTProject) bool) ([]iotv1alpha1.IoTProject, error) {

	var result []iotv1alpha1.IoTProject

	list := &iotv1alpha1.IoTProjectList{}

	err := r.client.List(context.TODO(), list)
	if err != nil {
		return nil, err
	}

	for _, item := range list.Items {
		if predicate(&item) {
			result = append(result, item)
		}
	}

	return result, nil
}
