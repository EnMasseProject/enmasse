/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type NamespaceSelector struct {
	// matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels
	// map is equivalent to an element of matchExpressions, whose key field is "key", the
	// operator is "In", and the values array contains only "value". The requirements are ANDed.
	// +optional
	MatchLabels map[string]string `json:"matchLabels,omitempty" protobuf:"bytes,1,rep,name=matchLabels"`
	// matchExpressions is a list of label selector requirements. The requirements are ANDed.
	// +optional
	MatchExpressions []metav1.LabelSelectorRequirement `json:"matchExpressions,omitempty" protobuf:"bytes,2,rep,name=matchExpressions"`
	// A list of namespaces this selector should match.
	// +optional
	MatchNames []string `json:"matchNames,omitempty"`
}

type MessagingCapability string

const (
	MessagingCapabilityTransactional MessagingCapability = "transactional"
)

type ObjectReference struct {
	// Name of referenced object.
	Name string `json:"name"`
	// Namespace of referenced object.
	Namespace string `json:"namespace,omitempty"`
}

type Selectable interface {
	metav1.Object
	GetSelector() *NamespaceSelector
}
