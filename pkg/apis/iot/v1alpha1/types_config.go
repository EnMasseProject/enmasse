/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

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

	JavaDefaults JavaContainerDefaults `json:"java,omitempty"`

	ServicesConfig ServicesConfig `json:"services"`
	AdaptersConfig AdaptersConfig `json:"adapters"`
}

type JavaContainerDefaults struct {
	RequireNativeTls *bool `json:"requireNativeTls,omitempty"`
}

type JavaContainerOptions struct {
	RequireNativeTls *bool `json:"requireNativeTls,omitempty"`
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
	Operator       OperatorConfig              `json:"operator,omitempty"`
}

type AdaptersConfig struct {
	HttpAdapterConfig    HttpAdapterConfig    `json:"http,omitempty"`
	MqttAdapterConfig    MqttAdapterConfig    `json:"mqtt,omitempty"`
	SigfoxAdapterConfig  SigfoxAdapterConfig  `json:"sigfox,omitempty"`
	LoraWanAdapterConfig LoraWanAdapterConfig `json:"lorawan,omitempty"`
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

type AdapterConfig struct {
	Enabled *bool `json:"enabled,omitempty"`
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

type OperatorConfig struct {
	ServiceConfig `json:",inline"`

	Container *ContainerConfig `json:"container,omitempty"`
}

type DeviceRegistryServiceConfig struct {
	ServiceConfig `json:",inline"`

	File       *FileBasedDeviceRegistry  `json:"file,omitempty"`
	Infinispan *InfinispanDeviceRegistry `json:"infinispan,omitempty"`
}

type InfinispanServer struct {
	External *ExternalInfinispanServer `json:"external,omitempty"`
}

type ExternalInfinispanServer struct {
	Host string `json:"host"`
	Port uint16 `json:"port"`

	Credentials `json:",inline"`

	SaslServerName string `json:"saslServerName,omitempty"`
	SaslRealm      string `json:"saslRealm,omitempty"`

	CacheNames *ExternalCacheNames `json:"cacheNames,omitempty"`
}

type ExternalCacheNames struct {
	Devices            string `json:"devices,omitempty"`
	DeviceStates       string `json:"deviceStates,omitempty"`
	AdapterCredentials string `json:"adapterCredentials,omitempty"`
}

// Common options for a single container Java service
type CommonServiceConfig struct {
	Container *ContainerConfig      `json:"container,omitempty"`
	Java      *JavaContainerOptions `json:"java,omitempty"`
}

type TenantServiceConfig struct {
	ServiceConfig       `json:",inline"`
	CommonServiceConfig `json:",inline"`
}

type AuthenticationServiceConfig struct {
	ServiceConfig       `json:",inline"`
	CommonServiceConfig `json:",inline"`
}

type FileBasedDeviceRegistry struct {
	NumberOfDevicesPerTenant *uint32 `json:"numberOfDevicesPerTenant,omitempty"`
	CommonServiceConfig      `json:",inline"`
}

type InfinispanDeviceRegistry struct {
	Server              InfinispanServer `json:"server"`
	CommonServiceConfig `json:",inline"`
}

// Common options for a standard 3-pod protocol adapter
type CommonAdapterConfig struct {
	ServiceConfig `json:",inline"`
	AdapterConfig `json:",inline"`

	Containers CommonAdapterContainers `json:"containers,omitempty"`
	Java       *JavaContainerOptions   `json:"java,omitempty"`

	EndpointConfig *AdapterEndpointConfig `json:"endpoint,omitempty"`
}

type HttpAdapterConfig struct {
	CommonAdapterConfig `json:",inline"`
}

type SigfoxAdapterConfig struct {
	CommonAdapterConfig `json:",inline"`
}

type LoraWanAdapterConfig struct {
	CommonAdapterConfig `json:",inline"`
}

type MqttAdapterConfig struct {
	CommonAdapterConfig `json:",inline"`
}

type IoTConfigStatus struct {
	Initialized bool   `json:"initialized"`
	State       string `json:"state"`

	AuthenticationServicePSK *string                  `json:"authenticationServicePSK"`
	Adapters                 map[string]AdapterStatus `json:"adapters,omitempty"`
	Services                 map[string]ServiceStatus `json:"services,omitempty"`
}

type CommonStatus struct {
	Endpoint EndpointStatus `json:"endpoint,omitempty"`
}

type AdapterStatus struct {
	CommonStatus `json:",inline"`

	InterServicePassword string `json:"interServicePassword,omitempty"`
	Enabled              bool   `json:"enabled"`
}

type ServiceStatus struct {
	CommonStatus `json:",inline"`
}

type EndpointStatus struct {
	URI string `json:"uri,omitempty"`
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
