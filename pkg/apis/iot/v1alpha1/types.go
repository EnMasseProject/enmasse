/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"encoding/json"

	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
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
	IsReady            bool                        `json:"isReady"`
	DownstreamEndpoint *ExternalDownstreamStrategy `json:"downstreamEndpoint,omitempty"`

	// FIXME: add conditions
}

type DownstreamStrategy struct {
	ExternalDownstreamStrategy *ExternalDownstreamStrategy `json:"externalStrategy,omitempty"`
	ManagedDownstreamStrategy  *ManagedDownstreamStrategy  `json:"managedStrategy,omitempty"`
}

type ManagedDownstreamStrategy struct {
	AddressSpaceName string `json:"addressSpaceName"`
}

type ExternalDownstreamStrategy struct {
	Host string `json:"host"`
	Port uint16 `json:"port"`

	Credentials `json:",inline"`

	TLS         bool   `json:"tls"`
	Certificate []byte `json:"certificate,omitempty"`
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

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type IoTConfig struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   IoTConfigSpec   `json:"spec,omitempty"`
	Status IoTConfigStatus `json:"status,omitempty"`
}

type IoTConfigSpec struct {
	EnableDefaultRoutes *bool `json:"enableDefaultRoutes,omitempty"`

	ImageOverrides map[string]v1beta1.ImageOverride `json:"imageOverrides,omitempty"`

	InterServiceCertificates *InterServiceCertificates `json:"interServiceCertificates,omitempty"`

	AdaptersConfig AdaptersConfig `json:"adapters"`
}

type InterServiceCertificates struct {
	SecretCertificatesStrategy *SecretCertificatesStrategy `json:"secretCertificatesStrategy,omitempty"`
	ServiceCAStrategy          *ServiceCAStrategy          `json:"serviceCAStrategy,omitempty"`
}

type ServiceCAStrategy struct {
}

type SecretCertificatesStrategy struct {
	CASecretName string `json:"caSecretName"`

	ServiceSecretNames map[string]string `json:"serviceSecretNames"`
}

type AdaptersConfig struct {
	HttpAdapterConfig HttpAdapterConfig `json:"http,omitempty"`
	MqttAdapterConfig MqttAdapterConfig `json:"mqtt,omitempty"`
}

type ServiceConfig struct {
	Replicas *int32 `json:"replicas,omitempty"`
}

type AdapterEndpointConfig struct {
	EnableDefaultRoute     *bool                   `json:"enableDefaultRoute,omitempty"`
	SecretNameStrategy     *SecretNameStrategy     `json:"secretNameStrategy,omitempty"`
	KeyCertificateStrategy *KeyCertificateStrategy `json:"keyCertificateStrategy,omitempty"`
}

type KeyCertificateStrategy struct {
	Key         []byte `json:"key,omitempty"`
	Certificate []byte `json:"certificate,omitempty"`
}

type SecretNameStrategy struct {
	TlsSecretName string `json:"secretName,omitempty"`
}

type HttpAdapterConfig struct {
	ServiceConfig  `json:",inline"`
	EndpointConfig *AdapterEndpointConfig `json:"endpoint,omitempty"`
}

type MqttAdapterConfig struct {
	ServiceConfig  `json:",inline"`
	EndpointConfig *AdapterEndpointConfig `json:"endpoint,omitempty"`
}

type IoTConfigStatus struct {
	Initialized bool   `json:"initialized"`
	State       string `json:"state"`

	AuthenticationServicePSK *string `json:"authenticationServicePSK"`
}

const (
	ConfigStateWrongName = "WrongName"
	ConfigStateRunning   = "Running"
	ConfigStateFailed    = "Failed"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type IoTConfigList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []IoTConfig `json:"items"`
}
