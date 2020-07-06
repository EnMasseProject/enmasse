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
	Downstream    DownstreamConfig    `json:"downstream"`
	Configuration TenantConfiguration `json:"configuration,omitempty"`
}

type IoTProjectStatus struct {
	Phase   ProjectPhaseType `json:"phase"`
	Message string           `json:"message,omitempty"`

	TenantName                    string `json:"tenantName"`
	MessagingInfrastructurePrefix string `json:"prefix"` // FIXME: this will be gone in the future

	Accepted AcceptedStatus `json:"accepted,omitempty"`

	Managed *ManagedStatus `json:"managed,omitempty"`

	Conditions []ProjectCondition `json:"conditions"`
}

type ProjectPhaseType string

const (
	ProjectPhaseActive      ProjectPhaseType = "Active"
	ProjectPhaseConfiguring ProjectPhaseType = "Configuring"
	ProjectPhaseTerminating ProjectPhaseType = "Terminating"
	ProjectPhaseFailed      ProjectPhaseType = "Failed"
)

type ProjectConditionType string

const (
	ProjectConditionTypeReady                 ProjectConditionType = "Ready"
	ProjectConditionTypeResourcesCreated      ProjectConditionType = "ResourcesCreated"
	ProjectConditionTypeResourcesReady        ProjectConditionType = "ResourcesReady"
	ProjectConditionTypeConfigurationAccepted ProjectConditionType = "ConfigurationAccepted"
	ProjectConditionTypeTrustAnchorsUnique    ProjectConditionType = "TrustAnchorsUnique"
)

type ProjectCondition struct {
	Type            ProjectConditionType `json:"type"`
	CommonCondition `json:",inline"`
}

// region Common

type Credentials struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type ConnectionInformation struct {
	Host string `json:"host"`
	Port uint16 `json:"port"`

	Credentials `json:",inline"`

	TLS         bool   `json:"tls"`
	Certificate []byte `json:"certificate,omitempty"`
}

// endregion

// region Configuration

type TenantConfiguration struct {
	Enabled *bool `json:"enabled,omitempty"`

	MinimumMessageSize uint64 `json:"minimumMessageSize,omitempty"`

	Adapters map[string]AdapterConfiguration `json:"adapters,omitempty"`

	ResourceLimits *ResourceLimits `json:"resourceLimits,omitempty"`

	Tracing *TracingConfiguration `json:"tracing,omitempty"`

	Defaults   json.RawMessage `json:"defaults,omitempty"`
	Extensions json.RawMessage `json:"ext,omitempty"`

	TrustAnchors []TrustAnchor `json:"trustAnchors,omitempty"`
}

type AdapterConfiguration struct {
	Enabled *bool `json:"enabled,omitempty"`

	Extensions json.RawMessage `json:"ext,omitempty"`
}

type TracingConfiguration struct {
	SamplingMode          string            `json:"samplingMode,omitempty"`
	SamplingModePerAuthId map[string]string `json:"samplingModePerAuthId,omitempty"`
}

type ResourceLimits struct {
	MaximumConnections       *uint32 `json:"maximumConnections,omitempty"`
	MaximumTimeToLiveSeconds *uint32 `json:"maximumTimeToLive,omitempty"`
}

type TrustAnchor struct {
	Enabled     *bool  `json:"enabled,omitempty"`
	Certificate string `json:"certificate"`
}

// endregion

// region Strategy

type DownstreamConfig struct {
	Addresses AddressesConfig `json:"addresses"`
}

type AddressesConfig struct {
	Telemetry AddressConfig `json:"telemetry"`
	Event     AddressConfig `json:"event"`
	Command   AddressConfig `json:"command"`
}

type AddressConfig struct {
	Plan string `json:"plan,omitempty"`
}

// endregion

// region Managed Status

type ManagedStatus struct {
	PasswordTime metav1.Time `json:"passwordTime,omitempty"`
	AddressSpace string      `json:"addressSpace,omitempty"`
}

// endregion

// region Accepted Status

type AcceptedStatus struct {
	Configuration AcceptedConfiguration `json:"configuration,omitempty"`
}

// The configuration, accepted by the operator, in the format the
// Hono Tenant API requires. This means that field names and data structures
// may seem a bit odd from a Kubernetes point of view.
type AcceptedConfiguration struct {
	Enabled *bool `json:"enabled,omitempty"`

	MinimumMessageSize uint64 `json:"minimum-message-size,omitempty"`

	// if the array has no entry, then it must be omitted (`omitempty` is required here)
	Adapters []AcceptedAdapterConfiguration `json:"adapters,omitempty"`
	Tracing  *AcceptedTracingConfiguration  `json:"tracing,omitempty"`

	ResourceLimits *AcceptedResourceLimits `json:"resource-limits,omitempty"`
	TrustAnchors   []AcceptedTrustAnchor   `json:"trusted-ca,omitempty"`

	Defaults   json.RawMessage `json:"defaults,omitempty"`
	Extensions json.RawMessage `json:"ext,omitempty"`
}

type AcceptedAdapterConfiguration struct {
	Type string `json:"type"`

	AdapterConfiguration `json:",inline"`
}

type AcceptedTracingConfiguration struct {
	SamplingMode          string            `json:"sampling-mode,omitempty"`
	SamplingModePerAuthId map[string]string `json:"sampling-mode-per-auth-id,omitempty"`
}

type AcceptedResourceLimits struct {
	MaximumConnections       int64 `json:"max-connections"`
	MaximumTimeToLiveSeconds int64 `json:"max-ttl"`
}

type AcceptedTrustAnchor struct {
	SubjectDN string `json:"subject-dn,omitempty"`

	PublicKey []byte `json:"public-key,omitempty"`
	Algorithm string `json:"algorithm,omitempty"`

	Enabled *bool `json:"enabled,omitempty"`

	NotBefore metav1.Time `json:"not-before"`
	NotAfter  metav1.Time `json:"not-after"`

	AutoProvisioningEnabled *bool `json:"auto-provisioning-enabled,omitempty"`
}

// endregion

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type IoTProjectList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []IoTProject `json:"items"`
}
