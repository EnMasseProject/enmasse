/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

/*
 * Be careful with the comments in this file. The prefix "+" indicates that this is being processed
 * by the client generator. The location, empty lines, and other comments in this file may confuse
 * the generator, and produce a non-version version.
 */

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +kubebuilder:resource:shortName=msgplan;msgplans,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The current phase."
// +kubebuilder:printcolumn:name="Message",type="string",JSONPath=".status.message",priority=1,description="Message describing the reason for the current Phase."
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
type MessagingPlan struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingPlanSpec   `json:"spec,omitempty"`
	Status            MessagingPlanStatus `json:"status,omitempty"`
}

type MessagingPlanSpec struct {
	// A selector defining which namespaces this plan should serve. Default is all namespaces.
	NamespaceSelector *NamespaceSelector `json:"namespaceSelector,omitempty"`
	// Resources specified for this plan.
	Resources  *MessagingPlanSpecResources `json:"resources,omitempty"`
}

type MessagingPlanSpecResources struct {
	// Requests map[MessagingPlanResource]resource.Quantity

	// Requested plan limits
	Limits map[MessagingPlanResource]resource.Quantity `json:"limits,omitempty"`
}

type MessagingPlanResource string

const (
	MessagingResourceLimitsConnections MessagingPlanResource = "limits.connections"
)

type MessagingPlanStatus struct {
	// +kubebuilder:printcolumn
	Phase      MessagingPlanPhase       `json:"phase,omitempty"`
	Message    string                   `json:"message,omitempty"`
	Conditions []MessagingPlanCondition `json:"conditions,omitempty"`
}

type MessagingPlanCondition struct {
	Type               MessagingPlanConditionType `json:"type"`
	Status             corev1.ConditionStatus     `json:"status"`
	LastTransitionTime metav1.Time                `json:"lastTransitionTime,omitempty"`
	Reason             string                     `json:"reason,omitempty"`
	Message            string                     `json:"message,omitempty"`
}

type MessagingPlanConditionType string

const (
	MessagingPlanReady MessagingPlanConditionType = "Ready"
)

type MessagingPlanPhase string

const (
	MessagingPlanConfiguring MessagingPlanPhase = "Configuring"
	MessagingPlanActive      MessagingPlanPhase = "Active"
	MessagingPlanTerminating MessagingPlanPhase = "Terminating"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingPlanList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []MessagingPlan `json:"items"`
}
