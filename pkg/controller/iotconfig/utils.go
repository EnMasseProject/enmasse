/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"

	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
)

// This sets the default Hono probes
func SetHonoProbes(container *corev1.Container) {
	container.ReadinessProbe = install.ApplyHttpProbe(container.ReadinessProbe, 10, "/readiness", 8088)
	container.LivenessProbe = install.ApplyHttpProbe(container.LivenessProbe, 180, "/liveness", 8088)
}

func applyDefaultDeploymentConfig(deployment *appsv1.Deployment, serviceConfig iotv1alpha1.ServiceConfig) {
	deployment.Spec.Replicas = serviceConfig.Replicas
}

func applyContainerConfig(container *corev1.Container, config *iotv1alpha1.ContainerConfig) {

	if config == nil {
		return
	}

	if config.Resources != nil {
		container.Resources = *config.Resources
	}

}

func (r *ReconcileIoTConfig) cleanupSecrets(ctx context.Context, config *iotv1alpha1.IoTConfig, adapterName string) error {

	// we need to use an unstructured list, as "SecretList" doesn't work
	// due to kubernetes-sigs/controller-runtime#362

	ul := unstructured.UnstructuredList{}
	ul.SetKind("SecretList")
	ul.SetAPIVersion("")

	n, err := install.BulkDeleteByLabelMap(
		ctx, r.client, &ul,
		config.GetNamespace(), install.CreateDefaultLabels(nil, "iot", adapterName+"-tls"),
		install.IsOwnedByPredicate(config, true),
	)

	if err == nil {
		log.Info("cleaned up adapter secrets", "adapter", adapterName, "secretsDeleted", n)
	}

	return err
}

func deviceRegistryImplementation(config *iotv1alpha1.IoTConfig) DeviceRegistryImplementation {

	var file = config.Spec.ServicesConfig.DeviceRegistry.File
	var infinispan = config.Spec.ServicesConfig.DeviceRegistry.Infinispan

	if infinispan != nil && file == nil {
		return DeviceRegistryInfinispan
	} else if infinispan == nil && file != nil {
		return DeviceRegistryFileBased
	} else {
		return DeviceRegistryIllegal
	}
}
