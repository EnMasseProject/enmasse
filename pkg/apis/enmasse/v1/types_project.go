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
// +kubebuilder:resource:shortName=msgp;msgproject;msgprojects,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The current phase."
// +kubebuilder:printcolumn:name="Message",type="string",JSONPath=".status.message",priority=1,description="Message describing the reason for the current Phase."
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
type MessagingProject struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingProjectSpec   `json:"spec,omitempty"`
	Status            MessagingProjectStatus `json:"status,omitempty"`
}

type MessagingProjectSpec struct {
	// Reference to a specific MessagingInfra to use (must be available for this project).
	MessagingInfrastructureRef *ObjectReference `json:"messagingInfrastructureRef,omitempty"`
	// Reference to a specific MessagingPlan to use (must be available for this project).
	MessagingPlanRef *ObjectReference `json:"messagingPlanRef,omitempty"`
	// The desired capabilities common to all addresses for this project.
	Capabilities []MessagingCapability `json:"capabilities,omitempty"`
}

type MessagingProjectStatus struct {
	// +kubebuilder:printcolumn
	Phase   MessagingProjectPhase `json:"phase,omitempty"`
	Message string                `json:"message,omitempty"`
	// MessagingInfra this project is bound to.
	MessagingInfrastructureRef ObjectReference `json:"messagingInfrastructureRef,omitempty"`
	// Applied plan name.
	MessagingPlanRef *ObjectReference `json:"messagingPlanRef,omitempty"`
	// Applied plan configuration.
	AppliedMessagingPlan *MessagingPlanSpec `json:"appliedMessagingPlan,omitempty"`
	// Current project conditions.
	Conditions []MessagingProjectCondition `json:"conditions,omitempty"`
	// The actual capabilities common to all addresses for this project.
	Capabilities []MessagingCapability `json:"capabilities,omitempty"`
	// For transactional projects, the broker addresses should be scheduled todo
	Broker *MessagingAddressBroker `json:"broker,omitempty"`
}

type MessagingProjectCondition struct {
	Type               MessagingProjectConditionType `json:"type"`
	Status             corev1.ConditionStatus        `json:"status"`
	LastTransitionTime metav1.Time                   `json:"lastTransitionTime,omitempty"`
	Reason             string                        `json:"reason,omitempty"`
	Message            string                        `json:"message,omitempty"`
}

type MessagingProjectConditionType string

const (
	MessagingProjectBound       MessagingProjectConditionType = "Bound"
	MessagingProjectCaCreated   MessagingProjectConditionType = "CaCreated"
	MessagingProjectScheduled   MessagingProjectConditionType = "Scheduled"
	MessagingProjectPlanApplied MessagingProjectConditionType = "PlanApplied"
	MessagingProjectCreated     MessagingProjectConditionType = "Created"
	MessagingProjectReady       MessagingProjectConditionType = "Ready"
)

type MessagingProjectPhase string

const (
	MessagingProjectConfiguring MessagingProjectPhase = "Configuring"
	MessagingProjectActive      MessagingProjectPhase = "Active"
	MessagingProjectTerminating MessagingProjectPhase = "Terminating"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingProjectList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []MessagingProject `json:"items"`
}
