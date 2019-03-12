/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"

	"github.com/enmasseproject/enmasse/pkg/util/recon"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
)

func (r *ReconcileIoTConfig) processOperator(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, "iot-operator", config, r.reconcileOperatorDeployment)
	})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileOperatorDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	deployment.Spec.Replicas = nil
	deployment.Spec.Template.Spec.ServiceAccountName = "iot-operator"

	err := install.ApplyContainerWithError(deployment, "operator", func(container *corev1.Container) error {
		if err := install.SetContainerImage(container, "enmasse-controller-manager", config); err != nil {
			return err
		}

		container.Env = []corev1.EnvVar{
			{Name: "CONTROLLER_ENABLE_IOT_PROJECT", Value: "true"},
			{Name: "CONTROLLER_DISABLE_ALL", Value: "true"},

			{Name: "POD_NAME", ValueFrom: &corev1.EnvVarSource{FieldRef: &corev1.ObjectFieldSelector{FieldPath: "metadata.name"}}},
			{Name: "OPERATOR_NAME", Value: "iot-operator"},
		}

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(128*1024*1024 /* 128Mi */, resource.BinarySI),
			},
		}

		return nil
	})

	if err != nil {
		return err
	}

	return nil
}
