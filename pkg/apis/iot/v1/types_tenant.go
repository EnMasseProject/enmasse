/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1

import (
	"encoding/json"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type IoTTenant struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   IoTTenantSpec   `json:"spec"`
	Status IoTTenantStatus `json:"status"`
}

type IoTTenantSpec struct {
	Downstream    DownstreamConfig    `json:"downstream"`
	Configuration TenantConfiguration `json:"configuration,omitempty"`
}

type IoTTenantStatus struct {
	Phase   TenantPhaseType `json:"phase"`
	Message string          `json:"message,omitempty"`

	TenantName string `json:"tenantName"`

	Accepted AcceptedStatus `json:"accepted,omitempty"`

	Conditions []TenantCondition `json:"conditions"`
}

type TenantPhaseType string

const (
	TenantPhaseActive      TenantPhaseType = "Active"
	TenantPhaseConfiguring TenantPhaseType = "Configuring"
	TenantPhaseTerminating TenantPhaseType = "Terminating"
	TenantPhaseFailed      TenantPhaseType = "Failed"
)

type TenantConditionType string

const (
	TenantConditionTypeReady                 TenantConditionType = "Ready"
	TenantConditionTypeResourcesCreated      TenantConditionType = "ResourcesCreated"
	TenantConditionTypeResourcesReady        TenantConditionType = "ResourcesReady"
	TenantConditionTypeConfigurationAccepted TenantConditionType = "ConfigurationAccepted"
	TenantConditionTypeTrustAnchorsUnique    TenantConditionType = "TrustAnchorsUnique"
)

type TenantCondition struct {
	Type            TenantConditionType `json:"type"`
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

type IoTTenantList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []IoTTenant `json:"items"`
}
