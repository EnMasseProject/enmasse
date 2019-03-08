/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1beta1

import (
	corev1 "k8s.io/api/core/v1"
)

type ImageOverride struct {
	Match ImageMatch `json:"match"`

	Registry     string            `json:"registry ,omitempty"`
	Organization string            `json:"organization,omitempty"`
	Name         string            `json:"name,omitempty"`
	Tag          string            `json:"tag,omitempty"`
	PullPolicy   corev1.PullPolicy `json:"pullPolicy,omitempty"`
}

type ImageMatch struct {
	ByNameMatch *ByNameMatch `json:"byName,omitempty"`
}

type ByNameMatch struct {
	Organization string `json:"organization,omitempty"`
	Name         string `json:"name,omitempty"`
}

type ImageOverridesProvider interface {
	GetImageOverrides() []ImageOverride
}
