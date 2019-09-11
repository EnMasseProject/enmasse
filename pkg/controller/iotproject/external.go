/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

func (r *ReconcileIoTProject) reconcileExternal(ctx context.Context, request *reconcile.Request, project *iotv1alpha1.IoTProject) (reconcile.Result, error) {

	// we simply copy over the external information
	// so everything is expected to be ready

	project.Status.
		GetProjectCondition(iotv1alpha1.ProjectConditionTypeResourcesCreated).
		SetStatusOk()
	project.Status.
		GetProjectCondition(iotv1alpha1.ProjectConditionTypeResourcesReady).
		SetStatusOk()
	project.Status.
		GetProjectCondition(iotv1alpha1.ProjectConditionTypeReady).
		SetStatusOk()

	project.Status.DownstreamEndpoint = project.Spec.DownstreamStrategy.ExternalDownstreamStrategy.DeepCopy()
	project.Status.IsReady = true

	return reconcile.Result{}, nil

}
