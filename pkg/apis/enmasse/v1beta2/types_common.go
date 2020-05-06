/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1beta2

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type NamespaceSelector struct {
	metav1.LabelSelector
	// A list of namespaces this selector should match.
	MatchNames []string `json:"matchNames,omitempty"`
}

type MessagingInfraReference struct {
	// Name of referenced MessagingInfra.
	Name string `json:"name"`
	// Namespace of referenced MessagingInfra.
	Namespace string `json:"namespace,omitempty"`
}
