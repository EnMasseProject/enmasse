/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"encoding/json"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type IoTProject struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   IoTProjectSpec   `json:"spec"`
	Status IoTProjectStatus `json:"status"`
}

type IoTProjectSpec struct {
	DownstreamStrategy DownstreamStrategy `json:"downstreamStrategy"`

	Configuration json.RawMessage `json:"configuration,omitempty"`
}

type IoTProjectStatus struct {
	Phase       ProjectPhaseType `json:"phase"`
	PhaseReason string           `json:"phaseReason,omitempty"`

	TenantName         string                 `json:"tenantName"`
	DownstreamEndpoint *ConnectionInformation `json:"downstreamEndpoint,omitempty"`

	Managed *ManagedStatus `json:"managed,omitempty"`

	Conditions []ProjectCondition `json:"conditions"`
}

type ProjectPhaseType string

const (
	ProjectPhaseReady       ProjectPhaseType = "Ready"
	ProjectPhaseConfiguring ProjectPhaseType = "Configuring"
	ProjectPhaseTerminating ProjectPhaseType = "Terminating"
	ProjectPhaseFailed      ProjectPhaseType = "Failed"
)

type ProjectConditionType string

const (
	ProjectConditionTypeReady            ProjectConditionType = "Ready"
	ProjectConditionTypeResourcesCreated ProjectConditionType = "ResourcesCreated"
	ProjectConditionTypeResourcesReady   ProjectConditionType = "ResourcesReady"
)

type ProjectCondition struct {
	Type            ProjectConditionType `json:"type"`
	CommonCondition `json:",inline"`
}

type DownstreamStrategy struct {
	ExternalDownstreamStrategy *ExternalDownstreamStrategy `json:"externalStrategy,omitempty"`
	ProvidedDownstreamStrategy *ProvidedDownstreamStrategy `json:"providedStrategy,omitempty"`
	ManagedDownstreamStrategy  *ManagedDownstreamStrategy  `json:"managedStrategy,omitempty"`
}

type ProvidedDownstreamStrategy struct {
	Namespace        string `json:"namespace"`
	AddressSpaceName string `json:"addressSpaceName"`

	Credentials `json:",inline"`

	EndpointMode *EndpointMode `json:"endpointMode,omitempty"`
	EndpointName string        `json:"endpointName,omitempty"`
	PortName     string        `json:"portName,omitempty"`
	TLS          *bool         `json:"tls,omitempty"`
}

type ManagedDownstreamStrategy struct {
	AddressSpace AddressSpaceConfig `json:"addressSpace"`
	Addresses    AddressesConfig    `json:"addresses"`
}

type AddressSpaceConfig struct {
	Name string `json:"name"`
	Plan string `json:"plan"`
	Type string `json:"type,omitempty"`
}

type AddressesConfig struct {
	Telemetry AddressConfig `json:"telemetry"`
	Event     AddressConfig `json:"event"`
	Command   AddressConfig `json:"command"`
}

type AddressConfig struct {
	Plan string `json:"plan"`
	Type string `json:"type,omitempty"`
}

type ConnectionInformation struct {
	Host string `json:"host"`
	Port uint16 `json:"port"`

	Credentials `json:",inline"`

	TLS         bool   `json:"tls"`
	Certificate []byte `json:"certificate,omitempty"`
}

type ExternalDownstreamStrategy struct {
	ConnectionInformation `json:",inline"`
}

type ManagedStatus struct {
	PasswordTime metav1.Time `json:"passwordTime,omitempty"`
	AddressSpace string      `json:"addressSpace,omitempty"`
}

type Credentials struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type IoTProjectList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []IoTProject `json:"items"`
}
