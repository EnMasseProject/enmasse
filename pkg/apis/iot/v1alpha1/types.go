/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"encoding/json"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	corev1 "k8s.io/api/core/v1"
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

	ServicesConfig ServicesConfig `json:"services"`
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

type ServicesConfig struct {
	DeviceRegistry DeviceRegistryServiceConfig `json:"deviceRegistry,omitempty"`
	Authentication AuthenticationServiceConfig `json:"authentication,omitempty"`
	Tenant         TenantServiceConfig         `json:"tenant,omitempty"`
	Collector      CollectorConfig             `json:"collector,omitempty"`
}

type AdaptersConfig struct {
	HttpAdapterConfig HttpAdapterConfig `json:"http,omitempty"`
	MqttAdapterConfig MqttAdapterConfig `json:"mqtt,omitempty"`
}

type ServiceConfig struct {
	Replicas *int32 `json:"replicas,omitempty"`
}

type ContainerConfig struct {
	Resources *corev1.ResourceRequirements `json:"resources,omitempty"`
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

type CommonAdapterContainers struct {
	Adapter           *ContainerConfig `json:"adapter,omitempty"`
	Proxy             *ContainerConfig `json:"proxy,omitempty"`
	ProxyConfigurator *ContainerConfig `json:"proxyConfigurator,omitempty"`
}

type CollectorConfig struct {
	Container *ContainerConfig `json:"container,omitempty"`
}

type DeviceRegistryServiceConfig struct {
	ServiceConfig `json:",inline"`

	File *FileBasedDeviceRegistry `json:"file,omitempty"`
	Infinispan *InfinispanDeviceRegistry `json:"infinispan,omitempty"`
}

type FileBasedDeviceRegistry struct {
	NumberOfDevicesPerTenant *uint32 `json:"numberOfDevicesPerTenant,omitempty"`

	Container *ContainerConfig `json:"container,omitempty"`
}

type InfinispanDeviceRegistry struct {
	InfinispanServerAddress string `json:"infinispanServerAddress"`

	Container *ContainerConfig `json:"container,omitempty"`
}

type TenantServiceConfig struct {
	ServiceConfig `json:",inline"`

	Container *ContainerConfig `json:"container,omitempty"`
}

type AuthenticationServiceConfig struct {
	ServiceConfig `json:",inline"`

	Container *ContainerConfig `json:"container,omitempty"`
}

type HttpAdapterConfig struct {
	ServiceConfig `json:",inline"`

	Containers CommonAdapterContainers `json:"containers,omitempty"`

	EndpointConfig *AdapterEndpointConfig `json:"endpoint,omitempty"`
}

type MqttAdapterConfig struct {
	ServiceConfig `json:",inline"`

	Containers CommonAdapterContainers `json:"containers,omitempty"`

	EndpointConfig *AdapterEndpointConfig `json:"endpoint,omitempty"`
}

type IoTConfigStatus struct {
	Initialized bool   `json:"initialized"`
	State       string `json:"state"`

	AuthenticationServicePSK *string                  `json:"authenticationServicePSK"`
	Adapters                 map[string]AdapterStatus `json:"adapters,omitempty"`
}

type AdapterStatus struct {
	InterServicePassword string `json:"interServicePassword"`
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
