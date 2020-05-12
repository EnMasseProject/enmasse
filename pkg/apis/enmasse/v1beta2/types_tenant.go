/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1beta2

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
// +kubebuilder:resource:shortName=msgt;msgtenant;msgtenants,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The current phase."
// +kubebuilder:printcolumn:name="Message",type="string",JSONPath=".status.message",priority=1,description="Message describing the reason for the current Phase."
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
type MessagingTenant struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingTenantSpec   `json:"spec,omitempty"`
	Status            MessagingTenantStatus `json:"status,omitempty"`
}

type MessagingTenantSpec struct {
	// Reference to a specific MessagingInfra to use (must be available for this tenant).
	MessagingInfraRef *MessagingInfraReference `json:"messagingInfraRef,omitempty"`
}

type MessagingTenantStatus struct {
	// +kubebuilder:printcolumn
	Phase   MessagingTenantPhase `json:"phase,omitempty"`
	Message string               `json:"message,omitempty"`
	// MessagingInfra this tenant is bound to.
	MessagingInfraRef *MessagingInfraReference   `json:"messagingInfraRef,omitempty"`
	Conditions        []MessagingTenantCondition `json:"conditions,omitempty"`
}

type MessagingTenantCondition struct {
	Type               MessagingTenantConditionType `json:"type"`
	Status             corev1.ConditionStatus       `json:"status"`
	LastTransitionTime metav1.Time                  `json:"lastTransitionTime,omitempty"`
	Reason             string                       `json:"reason,omitempty"`
	Message            string                       `json:"message,omitempty"`
}

type MessagingTenantConditionType string

const (
	MessagingTenantBound     MessagingTenantConditionType = "Bound"
	MessagingTenantCaCreated MessagingTenantConditionType = "CaCreated"
	MessagingTenantReady     MessagingTenantConditionType = "Ready"
)

type MessagingTenantPhase string

const (
	MessagingTenantConfiguring MessagingTenantPhase = "Configuring"
	MessagingTenantActive      MessagingTenantPhase = "Active"
	MessagingTenantTerminating MessagingTenantPhase = "Terminating"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingTenantList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []MessagingTenant `json:"items"`
}
