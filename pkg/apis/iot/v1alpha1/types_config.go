/*
 * Copyright 2018-2020, EnMasse authors.
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
	EnableDefaultRoutes     *bool  `json:"enableDefaultRoutes,omitempty"`
	MessagingInfrastructure string `json:"messagingInfrastructureRef,omitempty"`

	ImageOverrides map[string]v1beta1.ImageOverride `json:"imageOverrides,omitempty"`

	InterServiceCertificates *InterServiceCertificates `json:"interServiceCertificates,omitempty"`

	JavaDefaults JavaContainerDefaults `json:"java,omitempty"`
	TlsDefaults  TlsOptions            `json:"tls,omitempty"`

	Mesh           MeshConfig       `json:"mesh"`
	ServicesConfig ServicesConfig   `json:"services"`
	AdaptersConfig AdaptersConfig   `json:"adapters"`
	Tracing        TracingConfig    `json:"tracing"`
	Monitoring     MonitoringConfig `json:"monitoring"`
	Logging        LoggingConfig    `json:"logging"`
}

// region Logging

type LogLevel string

const (
	LogLevelTrace   LogLevel = "trace"
	LogLevelDebug   LogLevel = "debug"
	LogLevelInfo    LogLevel = "info"
	LogLevelWarning LogLevel = "warn"
	LogLevelError   LogLevel = "error"
)

type LoggingConfig struct {
	Level    LogLevel            `json:"level,omitempty"`
	Loggers  map[string]LogLevel `json:"loggers,omitempty"`
	Defaults LoggingDefaults     `json:"defaults,omitempty"`
}

type LoggingDefaults struct {
	Logback string `json:"logback,omitempty"`
}

type CommonLoggingConfig struct {
	Level   LogLevel            `json:"level,omitempty"`
	Loggers map[string]LogLevel `json:"loggers,omitempty"`
}

type LogbackConfig struct {
	CommonLoggingConfig `json:",inline"`
	Logback             string `json:"logback,omitempty"`
}

// endregion

//region Mesh

type MeshConfig struct {
	ServiceConfig   `json:",inline"`
	ContainerConfig `json:",inline"`

	Tls TlsOptions `json:"tls,omitempty"`
}

//endregion

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

//region Monitoring

type MonitoringConfig struct {
	Labels map[string]string `json:"labels,omitempty"`
}

//endregion

type ExtensionImage struct {
	Container corev1.Container `json:"container"`
}

type JavaContainerDefaults struct {
	RequireNativeTls *bool `json:"requireNativeTls,omitempty"`
}

type TlsOptions struct {
	Versions []string `json:"versions,omitempty"`
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
	Authentication   AuthenticationServiceConfig   `json:"authentication,omitempty"`
	DeviceConnection DeviceConnectionServiceConfig `json:"deviceConnection,omitempty"`
	DeviceRegistry   DeviceRegistryServiceConfig   `json:"deviceRegistry,omitempty"`
	Tenant           TenantServiceConfig           `json:"tenant,omitempty"`
}

type AdaptersConfig struct {
	DefaultOptions *AdapterOptions `json:"defaults,omitempty"`

	AmqpAdapterConfig    AmqpAdapterConfig    `json:"amqp,omitempty"`
	HttpAdapterConfig    HttpAdapterConfig    `json:"http,omitempty"`
	MqttAdapterConfig    MqttAdapterConfig    `json:"mqtt,omitempty"`
	SigfoxAdapterConfig  SigfoxAdapterConfig  `json:"sigfox,omitempty"`
	LoraWanAdapterConfig LoraWanAdapterConfig `json:"lorawan,omitempty"`
}

type ServiceConfig struct {
	Replicas *int32           `json:"replicas,omitempty"`
	Affinity *corev1.Affinity `json:"affinity,omitempty"`
}

type ContainerConfig struct {
	Resources *corev1.ResourceRequirements `json:"resources,omitempty"`
}

type JavaContainerConfig struct {
	ContainerConfig `json:",inline"`

	RequireNativeTls *bool `json:"requireNativeTls,omitempty"`

	Logback LogbackConfig `json:"logback,omitempty"`
}

type EndpointConfig struct {
	EnableDefaultRoute *bool               `json:"enableDefaultRoute,omitempty"`
	SecretNameStrategy *SecretNameStrategy `json:"secretNameStrategy,omitempty"`
}

type AdapterConfig struct {
	Enabled *bool `json:"enabled,omitempty"`

	Options *AdapterOptions `json:"options,omitempty"`
}

type SecretNameStrategy struct {
	TlsSecretName string `json:"secretName,omitempty"`
}

type CommonAdapterContainers struct {
	Adapter           JavaContainerConfig `json:"adapter,omitempty"`
	Proxy             ContainerConfig     `json:"proxy,omitempty"`
	ProxyConfigurator ContainerConfig     `json:"proxyConfigurator,omitempty"`
}

//region DeviceConnection

type DeviceConnectionServiceConfig struct {
	Infinispan *InfinispanDeviceConnection `json:"infinispan,omitempty"`
	JDBC       *JdbcDeviceConnection       `json:"jdbc,omitempty"`
}

type InfinispanDeviceConnection struct {
	ServiceConfig        `json:",inline"`
	CommonDeviceRegistry `json:",inline"`
	CommonServiceConfig  `json:",inline"`

	Server InfinispanDeviceConnectionServer `json:"server"`
}

type InfinispanDeviceConnectionServer struct {
	External *ExternalInfinispanDeviceConnectionServer `json:"external,omitempty"`
}

type ExternalInfinispanDeviceConnectionServer struct {
	ExternalInfinispanServer `json:",inline"`

	CacheNames *ExternalDeviceConnectionCacheNames `json:"cacheNames,omitempty"`
}

type ExternalDeviceConnectionCacheNames struct {
	DeviceConnections string `json:"deviceConnections,omitempty"`
}

type JdbcDeviceConnection struct {
	ServiceConfig        `json:",inline"`
	CommonDeviceRegistry `json:",inline"`
	CommonServiceConfig  `json:",inline"`

	Server JdbcDeviceConnectionServer `json:"server"`
}

type JdbcDeviceConnectionServer struct {
	External *ExternalJdbcDeviceConnectionServer `json:"external,omitempty"`
}

type ExternalJdbcDeviceConnectionServer struct {
	JdbcConnectionInformation `json:",inline"`

	Extensions []ExtensionImage `json:"extensions,omitempty"`
}

//endregion

type ExternalInfinispanServer struct {
	Host string `json:"host"`
	Port uint16 `json:"port"`

	Credentials `json:",inline"`

	SaslServerName string `json:"saslServerName,omitempty"`
	SaslRealm      string `json:"saslRealm,omitempty"`

	DeletionChunkSize uint32 `json:"deletionChunkSize"`
}

type JdbcConnectionInformation struct {
	URL         string `json:"url"`
	DriverClass string `json:"driverClass,omitempty"`
	Username    string `json:"username,omitempty"`
	Password    string `json:"password,omitempty"`

	MaximumPoolSize uint32 `json:"maximumPoolSize,omitempty"`

	TableName string `json:"tableName,omitempty"`
}

type CommonDeviceRegistry struct {
	// Disable configuration temporarily (use for development only)
	Disabled bool `json:"disabled,omitempty"`
}

//region DeviceRegistry

type DeviceRegistryServiceConfig struct {
	Infinispan *InfinispanDeviceRegistry `json:"infinispan,omitempty"`
	JDBC       *JdbcDeviceRegistry       `json:"jdbc,omitempty"`

	Management ManagementConfig `json:"management,omitempty"`
}

type ManagementConfig struct {
	Endpoint EndpointConfig `json:"endpoint,omitempty"`
}

type InfinispanRegistryServer struct {
	External *ExternalInfinispanRegistryServer `json:"external,omitempty"`
}

type ExternalInfinispanRegistryServer struct {
	ExternalInfinispanServer `json:",inline"`

	CacheNames *ExternalRegistryCacheNames `json:"cacheNames,omitempty"`
}

type InfinispanRegistryManagement struct {
	AuthTokenCacheExpiration string `json:"authTokenCacheExpiration,omitempty"`
}

type ExternalRegistryCacheNames struct {
	Devices            string `json:"devices,omitempty"`
	AdapterCredentials string `json:"adapterCredentials,omitempty"`
}

type InfinispanDeviceRegistry struct {
	ServiceConfig        `json:",inline"`
	CommonDeviceRegistry `json:",inline"`
	CommonServiceConfig  `json:",inline"`

	Server     InfinispanRegistryServer     `json:"server"`
	Management InfinispanRegistryManagement `json:"management"`
}

type JdbcDeviceRegistry struct {
	CommonDeviceRegistry `json:",inline"`

	Server     JdbcRegistryServer     `json:"server"`
	Management JdbcRegistryManagement `json:"management"`
}

type JdbcRegistryServer struct {
	External *ExternalJdbcRegistryServer `json:"external,omitempty"`
}

type ExternalJdbcRegistryServer struct {
	Management *ExternalJdbcRegistryService `json:"management,omitempty"`
	Adapter    *ExternalJdbcRegistryService `json:"adapter,omitempty"`

	Extensions []ExtensionImage `json:"extensions,omitempty"`
}

type ExternalJdbcRegistryService struct {
	CommonServiceConfig `json:",inline"`
	ServiceConfig       `json:",inline"`

	Connection JdbcConnectionInformation `json:"connection"`
}

type JdbcRegistryManagement struct {
	AuthTokenCacheExpiration string `json:"authTokenCacheExpiration,omitempty"`
}

//endregion

// Common options for a single container Java service
type CommonServiceConfig struct {
	Container JavaContainerConfig `json:"container,omitempty"`
	Tls       TlsOptions          `json:"tls,omitempty"`
}

//region TenantService

type TenantServiceConfig struct {
	ServiceConfig       `json:",inline"`
	CommonServiceConfig `json:",inline"`
}

//endregion

//region AuthenticationService

type AuthenticationServiceConfig struct {
	ServiceConfig       `json:",inline"`
	CommonServiceConfig `json:",inline"`
}

//endregion

//region Adapters

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
	Tls        TlsOptions              `json:"tls,omitempty"`

	EndpointConfig EndpointConfig `json:"endpoint,omitempty"`
}

type AmqpAdapterConfig struct {
	CommonAdapterConfig `json:",inline"`
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

//endregion

//region Status

type IoTConfigStatus struct {
	Phase   ConfigPhaseType `json:"phase"`
	Message string          `json:"message,omitempty"`

	Adapters map[string]AdapterStatus `json:"adapters,omitempty"`
	Services map[string]ServiceStatus `json:"services,omitempty"`

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
	ConfigConditionTypeReady    ConfigConditionType = "Ready"
	ConfigConditionTypeDegraded ConfigConditionType = "Degraded"

	ConfigConditionTypeReconciled ConfigConditionType = "Reconciled"

	ConfigConditionTypeCommandMeshReady                     ConfigConditionType = "CommandMeshReady"
	ConfigConditionTypeAuthServiceReady                     ConfigConditionType = "AuthServiceReady"
	ConfigConditionTypeTenantServiceReady                   ConfigConditionType = "TenantServiceReady"
	ConfigConditionTypeDeviceConnectionServiceReady         ConfigConditionType = "DeviceConnectionServiceReady"
	ConfigConditionTypeDeviceRegistryAdapterServiceReady    ConfigConditionType = "DeviceRegistryAdapterServiceReady"
	ConfigConditionTypeDeviceRegistryManagementServiceReady ConfigConditionType = "DeviceRegistryManagementServiceReady"

	ConfigConditionTypeAmqpAdapterReady    ConfigConditionType = "AmqpAdapterReady"
	ConfigConditionTypeHttpAdapterReady    ConfigConditionType = "HttpAdapterReady"
	ConfigConditionTypeLorawanAdapterReady ConfigConditionType = "LorawanAdapterReady"
	ConfigConditionTypeMqttAdapterReady    ConfigConditionType = "MqttAdapterReady"
	ConfigConditionTypeSigfoxAdapterReady  ConfigConditionType = "SigfoxAdapterReady"
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

	Enabled bool `json:"enabled"`
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
