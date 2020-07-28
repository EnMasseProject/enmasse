/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iottenant

import (
	"context"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
)

func (r *ReconcileIoTTenant) findIoTProjectsByPredicate(predicate func(project *iotv1alpha1.IoTTenant) bool) ([]iotv1alpha1.IoTTenant, error) {

	var result []iotv1alpha1.IoTTenant

	list := &iotv1alpha1.IoTTenantList{}

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
