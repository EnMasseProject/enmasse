/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	corev1 "k8s.io/api/core/v1"
)

func FindAdapterSpec(config *iotv1alpha1.IoTConfig, adapterType string) *iotv1alpha1.AdapterSpec {

	for _, a := range config.Spec.Adapters {
		if a.AdapterType == adapterType {
			return &a
		}
	}

	return nil
}

// This sets the default Hono probes
func SetHonoProbes(container *corev1.Container) {
	container.ReadinessProbe = install.ApplyHttpProbe(container.ReadinessProbe, 10, "/readiness", 8088)
	container.LivenessProbe = install.ApplyHttpProbe(container.LivenessProbe, 180, "/liveness", 8088)
}
