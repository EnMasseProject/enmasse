/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
)

func (r *ReconcileIoTConfig) processCollector(ctx context.Context, config *iotv1alpha1.IoTConfig) error {
	return r.processDeployment(ctx, "iot-gc", config, r.reconcileCollectorDeployment)
}

func (r *ReconcileIoTConfig) reconcileCollectorDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	var ONE int32 = 1
	deployment.Spec.Replicas = &ONE

	err := install.ApplyContainerWithError(deployment, "collector", func(container *corev1.Container) error {
		if err := install.SetContainerImage(container, "iot-gc", config); err != nil {
			return err
		}

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(128*1024*1024 /* 128Mi */, resource.BinarySI),
			},
		}

		// apply container options

		applyContainerConfig(container, config.Spec.ServicesConfig.Collector.Container)

		// return

		return nil
	})

	if err != nil {
		return err
	}

	return nil
}
