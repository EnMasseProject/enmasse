/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1beta2

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

/*
 * Be careful with the comments in this file. The prefix "+" indicates that this is being processed
 * by the client generator. The location, empty lines, and other comments in this file may confuse
 * the generator, and produce a non-version version.
 */

// ** AddressPlan

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +genclient:noStatus

type AddressPlan struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   AddressPlanSpec   `json:"spec"`
	Status AddressPlanStatus `json:"status,omitempty"`
}

type AddressPlanSpec struct {
	AddressType      string             `json:"addressType"`
	DisplayName      string             `json:"displayName,omitempty"`
	LongDescription  string             `json:"longDescription,omitempty"`
	ShortDescription string             `json:"shortDescription,omitempty"`
	DisplayOrder     int                `json:"displayOrder,omitempty"`
	Resources        map[string]float64 `json:"resources,omitempty"`
}

type AddressPlanPhase string

const (
	AddressPlanPending     AddressPlanPhase = "Pending"
	AddressPlanConfiguring AddressPlanPhase = "Configuring"
	AddressPlanActive      AddressPlanPhase = "Active"
	AddressPlanFailed      AddressPlanPhase = "Failed"
)

type AddressPlanStatus struct {
	Phase   AddressPlanPhase `json:"phase,omitempty"`
	Message string           `json:"message,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AddressPlanList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []AddressPlan `json:"items"`
}

// ** AddressSpacePlan

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +genclient:noStatus

type AddressSpacePlan struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   AddressSpacePlanSpec   `json:"spec"`
	Status AddressSpacePlanStatus `json:"status,omitempty"`
}

type AddressSpacePlanSpec struct {
	AddressPlans     []string           `json:"addressPlans"`
	AddressSpaceType string             `json:"addressSpaceType"`
	DisplayName      string             `json:"displayName,omitempty"`
	LongDescription  string             `json:"longDescription,omitempty"`
	ShortDescription string             `json:"shortDescription,omitempty"`
	InfraConfigRef   string             `json:"infraConfigRef,omitempty"`
	DisplayOrder     int                `json:"displayOrder,omitempty"`
	ResourceLimits   map[string]float64 `json:"resourceLimits,omitempty"`
}

type AddressSpacePlanPhase string

const (
	AddressSpacePlanPending     AddressSpacePlanPhase = "Pending"
	AddressSpacePlanConfiguring AddressSpacePlanPhase = "Configuring"
	AddressSpacePlanActive      AddressSpacePlanPhase = "Active"
	AddressSpacePlanFailed      AddressSpacePlanPhase = "Failed"
)

type AddressSpacePlanStatus struct {
	Phase   AddressSpacePlanPhase `json:"phase,omitempty"`
	Message string                `json:"message,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AddressSpacePlanList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []AddressSpacePlan `json:"items"`
}
