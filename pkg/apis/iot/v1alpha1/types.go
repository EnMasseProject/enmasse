/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type CommonCondition struct {
	Status             v1.ConditionStatus `json:"status"`
	LastTransitionTime metav1.Time        `json:"lastTransitionTime"`
	Reason             string             `json:"reason,omitempty"`
	Message            string             `json:"message,omitempty"`
}
