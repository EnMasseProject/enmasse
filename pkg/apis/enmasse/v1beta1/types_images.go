/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1beta1

import (
	corev1 "k8s.io/api/core/v1"
)

type ImageOverride struct {
	Name       string            `json:"name"`
	PullPolicy corev1.PullPolicy `json:"pullPolicy,omitempty"`
}

type ImageOverridesProvider interface {
	GetImageOverrides() map[string]ImageOverride
}
