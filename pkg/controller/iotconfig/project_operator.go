/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"

	"github.com/enmasseproject/enmasse/pkg/util/images"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
)

func (r *ReconcileIoTConfig) processProjectOperator(ctx context.Context, config *iotv1alpha1.IoTConfig) error {
	return r.processDeployment(ctx, "iot-operator", config, false, r.reconcileProjectOperator)
}

func (r *ReconcileIoTConfig) reconcileProjectOperator(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	applyDefaultDeploymentConfig(deployment, config.Spec.ServicesConfig.Operator.ServiceConfig, nil)

	deployment.Spec.Template.Spec.ServiceAccountName = "iot-operator"

	err := install.ApplyDeploymentContainerWithError(deployment, "operator", func(container *corev1.Container) error {
		if err := install.SetContainerImage(container, "controller-manager", config); err != nil {
			return err
		}

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(128*1024*1024 /* 128Mi */, resource.BinarySI),
			},
		}

		// apply container options

		applyContainerConfig(container, config.Spec.ServicesConfig.Operator.Container)

		iotTenantCleanerImage, err := images.GetImage("iot-tenant-cleaner")
		if err != nil {
			return err
		}

		// setup env vars

		container.Env = []corev1.EnvVar{
			{Name: "POD_NAME", ValueFrom: &corev1.EnvVarSource{
				FieldRef: &corev1.ObjectFieldSelector{FieldPath: "metadata.name"},
			}},
			{Name: "K8S_NAMESPACE", ValueFrom: &corev1.EnvVarSource{
				FieldRef: &corev1.ObjectFieldSelector{FieldPath: "metadata.namespace"},
			}},
			{Name: "IOT_CONFIG_NAME", Value: config.Name},

			{Name: "OPERATOR_NAME", Value: "iot-operator"},

			{Name: "CONTROLLER_DISABLE_ALL", Value: "true"},
			{Name: "CONTROLLER_ENABLE_IOT_PROJECT", Value: "true"},

			{Name: "IMAGE_PULL_POLICY", Value: string(images.GetDefaultPullPolicy())},
			{Name: "IOT_TENANT_CLEANER_IMAGE", Value: iotTenantCleanerImage},
		}

		// return

		return nil
	})

	if err != nil {
		return err
	}

	return nil
}
