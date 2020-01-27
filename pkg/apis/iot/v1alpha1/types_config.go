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
	Tracing        TracingConfig  `json:"tracing"`
}

//region Tracing

type TracingConfig struct {
	Strategy TracingStrategy `json:"strategy,omitempty"`
}

type TracingStrategy struct {
	Sidecar   *SidecarTracingStrategy   `json:"sidecar,omitempty"`
	DaemonSet *DaemonSetTracingStrategy `json:"daemonSet,omitempty"`
}

type SidecarTracingStrategy struct {
}

type DaemonSetTracingStrategy struct {
}

//endregion

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
}

type AdaptersConfig struct {
	DefaultOptions *AdapterOptions `json:"defaults,omitempty"`

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

	Options *AdapterOptions `json:"options,omitempty"`
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

// Deprecated: no longer used
type CollectorConfig struct {
	Container *ContainerConfig `json:"container,omitempty"`
}

type DeviceRegistryServiceConfig struct {
	ServiceConfig `json:",inline"`

	File       *FileBasedDeviceRegistry  `json:"file,omitempty"`
	Infinispan *InfinispanDeviceRegistry `json:"infinispan,omitempty"`
	JDBC       *JdbcDeviceRegistry       `json:"jdbc,omitempty"`
}

type JdbcServer struct {
	External *ExternalJdbcServer `json:"external,omitempty"`
}

type ExtensionImage struct {
	Container corev1.Container `json:"container"`
}

type ExternalJdbcServer struct {
	JdbcConnectionInformation `json:",inline"`

	Devices           ExternalJdbcDevices           `json:"devices,omitempty"`
	DeviceInformation ExternalJdbcDeviceInformation `json:"deviceInformation,omitempty"`

	Extensions []ExtensionImage `json:"extensions,omitempty"`
}

type JdbcConnectionInformation struct {
	URL         string `json:"url"`
	DriverClass string `json:"driverClass"`
	Username    string `json:"username,omitempty"`
	Password    string `json:"password,omitempty"`

	MaximumPoolSize uint32 `json:"maximumPoolSize,omitempty"`
}

type ExternalJdbcService struct {
	JdbcConnectionInformation `json:",inline"`

	TableName string `json:"tableName,omitempty"`
}

type ExternalJdbcDevices struct {
	ExternalJdbcService `json:",inline"`

	Mode string `json:"mode,omitempty"`
}

type ExternalJdbcDeviceInformation struct {
	ExternalJdbcService `json:",inline"`
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

	CacheNames        *ExternalCacheNames `json:"cacheNames,omitempty"`
	DeletionChunkSize uint32              `json:"deletionChunkSize"`
}

type InfinispanRegistryManagement struct {
	AuthTokenCacheExpiration string `json:"authTokenCacheExpiration,omitempty"`
}

type JdbcRegistryManagement struct {
	AuthTokenCacheExpiration string `json:"authTokenCacheExpiration,omitempty"`
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

type CommonDeviceRegistry struct {
	// Disable configuration temporarily (use for development only)
	Disabled bool `json:"disabled,omitempty"`
}

type FileBasedDeviceRegistry struct {
	CommonDeviceRegistry `json:",inline"`

	NumberOfDevicesPerTenant *uint32 `json:"numberOfDevicesPerTenant,omitempty"`
	CommonServiceConfig      `json:",inline"`
	SecurityContext          *corev1.PodSecurityContext `json:"securityContext,omitempty"`
}

type InfinispanDeviceRegistry struct {
	CommonDeviceRegistry `json:",inline"`
	CommonServiceConfig  `json:",inline"`

	Server     InfinispanServer             `json:"server"`
	Management InfinispanRegistryManagement `json:"management"`
}

type JdbcDeviceRegistry struct {
	CommonDeviceRegistry `json:",inline"`
	CommonServiceConfig  `json:",inline"`

	Server     JdbcServer             `json:"server"`
	Management JdbcRegistryManagement `json:"management"`
}

// The adapter options should focus on functional configuration applicable to all adapters
type AdapterOptions struct {
	TenantIdleTimeout string `json:"tenantIdleTimeout,omitempty"`
	MaxPayloadSize    uint32 `json:"maxPayloadSize,omitempty"`
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

//region Status

type IoTConfigStatus struct {
	Phase       ConfigPhaseType `json:"phase"`
	PhaseReason string          `json:"phaseReason,omitempty"`

	AuthenticationServicePSK *string                  `json:"authenticationServicePSK"`
	Adapters                 map[string]AdapterStatus `json:"adapters,omitempty"`
	Services                 map[string]ServiceStatus `json:"services,omitempty"`

	Conditions []ConfigCondition `json:"conditions"`
}

type ConfigPhaseType string

const (
	ConfigPhaseActive      ConfigPhaseType = "Active"
	ConfigPhaseConfiguring ConfigPhaseType = "Configuring"
	ConfigPhaseTerminating ConfigPhaseType = "Terminating"
	ConfigPhaseFailed      ConfigPhaseType = "Failed"
)

type ConfigConditionType string

const (
	ConfigConditionTypeReady ConfigConditionType = "Ready"
)

type ConfigCondition struct {
	Type            ConfigConditionType `json:"type"`
	CommonCondition `json:",inline"`
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

//endregion

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type IoTConfigList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []IoTConfig `json:"items"`
}
