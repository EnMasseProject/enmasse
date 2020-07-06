/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

/*
 * Be careful with the comments in this file. The prefix "+" indicates that this is being processed
 * by the client generator. The location, empty lines, and other comments in this file may confuse
 * the generator, and produce a non-version version.
 */

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +kubebuilder:resource:shortName=msgap;msgaddressplan;msgaddressplans,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The current phase."
// +kubebuilder:printcolumn:name="Message",type="string",JSONPath=".status.message",priority=1,description="Message describing the reason for the current Phase."
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
type MessagingAddressPlan struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingAddressPlanSpec   `json:"spec,omitempty"`
	Status            MessagingAddressPlanStatus `json:"status,omitempty"`
}

type MessagingAddressPlanSpec struct {
	// A selector defining which namespaces this plan should serve. Default is all namespaces.
	NamespaceSelector *NamespaceSelector `json:"namespaceSelector,omitempty"`
}

type MessagingAddressPlanStatus struct {
	// +kubebuilder:printcolumn
	Phase      MessagingAddressPlanPhase       `json:"phase,omitempty"`
	Message    string                          `json:"message,omitempty"`
	Conditions []MessagingAddressPlanCondition `json:"conditions,omitempty"`
}

type MessagingAddressPlanCondition struct {
	Type               MessagingAddressPlanConditionType `json:"type"`
	Status             corev1.ConditionStatus            `json:"status"`
	LastTransitionTime metav1.Time                       `json:"lastTransitionTime,omitempty"`
	Reason             string                            `json:"reason,omitempty"`
	Message            string                            `json:"message,omitempty"`
}

type MessagingAddressPlanConditionType string

const (
	MessagingAddressPlanReady MessagingAddressPlanConditionType = "Ready"
)

type MessagingAddressPlanPhase string

const (
	MessagingAddressPlanConfiguring MessagingAddressPlanPhase = "Configuring"
	MessagingAddressPlanActive      MessagingAddressPlanPhase = "Active"
	MessagingAddressPlanTerminating MessagingAddressPlanPhase = "Terminating"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingAddressPlanList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []MessagingAddressPlan `json:"items"`
}
