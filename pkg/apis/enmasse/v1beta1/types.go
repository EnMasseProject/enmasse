/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1beta1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

/*
 * Be careful with the comments in this file. The prefix "+" indicates that this is being processed
 * by the client generator. The location, empty lines, and other comments in this file may confuse
 * the generator, and produce a non-version version.
 */

// ** AddressSpaceSchema

// +genclient
// +genclient:nonNamespaced
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AddressSpaceSchema struct {
       metav1.TypeMeta   `json:",inline"`
       metav1.ObjectMeta `json:"metadata,omitempty"`

       Spec    AddressSpaceSchemaSpec   `json:"spec"`
}

type AddressSpaceSchemaSpec struct {
	   AuthenticationServices []string `json:"authenticationServices,omitempty"`
       Description            string   `json:"description,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AddressSpaceSchemaList struct {
       metav1.TypeMeta `json:",inline"`
       metav1.ListMeta `json:"metadata,omitempty"`

       Items []AddressSpaceSchema `json:"items"`
}

// ** AuthenticationService

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AuthenticationService struct {

	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Type string `json:"type,omitempty"`
	Name string `json:"name,omitempty"`

	Overrides *AuthenticationServiceSettings `json:"overrides,omitempty"`
}

type AuthenticationServiceSettings struct {
	Host  string `json:"host,omitempty"`
	Port  int    `json:"port,omitempty"`
	Realm string `json:"realm,omitempty"`

	CaCertSecret     *corev1.SecretReference `json:"caCertSecret,omitempty"`
	ClientCertSecret *corev1.SecretReference `json:"clientCertSecret,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AuthenticationServiceList struct {
       metav1.TypeMeta `json:",inline"`
       metav1.ListMeta `json:"metadata,omitempty"`

       Items []AuthenticationService `json:"items"`
}

// ** AddressSpace

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AddressSpace struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   AddressSpaceSpec   `json:"spec"`
	Status AddressSpaceStatus `json:"status"`
}

type AddressSpaceSpec struct {
	Plan string `json:"plan"`
	Type string `json:"type"`

	AuthenticationService *AuthenticationService `json:"authenticationService,omitempty"`

	Endpoints  []EndpointSpec  `json:"endpoints,omitempty"`
	Connectors []ConnectorSpec `json:"connectors,omitempty"`
}

type EndpointSpec struct {
	Name        string           `json:"name"`
	Service     string           `json:"service"`
	Certificate *CertificateSpec `json:"cert,omitempty"`
	Expose      *ExposeSpec      `json:"expose,omitempty"`
}

type CertificateSpec struct {
	Provider   string `json:"provider"`
	SecretName string `json:"secretName,omitempty"`
}

type ExposeSpec struct {
	Type                string `json:"type"`
	RouteServicePort    string `json:"routeServicePort"`
	RouteTlsTermination string `json:"routeTlsTermination"`
}

type ConnectorSpec struct {
	Name          string                   `json:"name"`
	EndpointHosts []ConnectorEndpointHost  `json:"endpointHosts"`
	Tls           ConnectorTlsSpec         `json:"tls,omitempty"`
	Credentials   ConnectorCredentialsSpec `json:"credentials,omitempty"`
	Addresses     []ConnectorAddressRule   `json:"addresses"`
}

type ConnectorEndpointHost struct {
	Host string `json:"host"`
	Port int    `json:"port,omitempty"`
}

type ConnectorTlsSpec struct {
	CaCert     StringOrSecretSelector `json:"caCert,omitempty"`
	ClientCert StringOrSecretSelector `json:"clientCert,omitempty"`
	ClientKey  StringOrSecretSelector `json:"clientKey,omitempty"`
}

type ConnectorCredentialsSpec struct {
	Username StringOrSecretSelector `json:"username"`
	Password StringOrSecretSelector `json:"password"`
}

type StringOrSecretSelector struct {
	Value           string            `json:"value,omitempty"`
	ValueFromSecret SecretKeySelector `json:"valueFromSecret,omitempty"`
}

type SecretKeySelector struct {
	Name string `json:"name"`
	Key  string `json:"key,omitempty"`
}

type ConnectorAddressRule struct {
	Name    string `json:"name"`
	Pattern string `json:"pattern"`
}

type AddressSpaceStatus struct {
	IsReady        bool              `json:"isReady"`
	Phase          string            `json:"phase,omitempty"`
	Messages       []string          `json:"messages,omitempty"`
	CACertificate  []byte            `json:"caCert,omitempty"`
	EndpointStatus []EndpointStatus  `json:"endpointStatuses,omitempty"`
	Connectors     []ConnectorStatus `json:"connectors,omitempty"`
	Routers        []RouterStatus    `json:"routers,omitempty"`
}

type EndpointStatus struct {
	Name        string `json:"name"`
	Certificate []byte `json:"cert,omitempty"`

	ServiceHost  string `json:"serviceHost"`
	ServicePorts []Port `json:"servicePorts,omitempty"`

	ExternalHost  string `json:"externalHost,omitempty"`
	ExternalPorts []Port `json:"externalPorts,omitempty"`
}

type Port struct {
	Name string `json:"name"`
	Port uint16 `json:"port"`
}

type ConnectorStatus struct {
	Name     string   `json:"name"`
	IsReady  bool     `json:"isReady"`
	Messages []string `json:"messages,omitempty"`
}

type RouterStatus struct {
	Id          string   `json:"id"`
	Neighbors  []string `json:"neighbors,omitempty"`
	Undelivered int      `json:"undelivered"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AddressSpaceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []AddressSpace `json:"items"`
}

// ** Address

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +genclient:noStatus

type Address struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   AddressSpec   `json:"spec"`
	Status AddressStatus `json:"status,omitempty"`
}

type AddressSpec struct {
	Address      string            `json:"address"`
	AddressSpace string            `json:"addressSpace,omitempty"`
	Type         string            `json:"type"`
	Plan         string            `json:"plan"`
	Topic        string            `json:"topic,omitempty"`
	Subscription *SubscriptionSpec `json:"subscription,omitempty"`
	Forwarders   []ForwarderSpec   `json:"forwarders,omitempty"`
}

type SubscriptionSpec struct {
	MaxConsumers *int `json:"maxConsumers,omitempty"`
}

type ForwarderSpec struct {
	Name          string             `json:"name"`
	RemoteAddress string             `json:"remoteAddress"`
	Direction     ForwarderDirection `json:"direction"`
}

type ForwarderDirection string

const (
	In  ForwarderDirection = "in"
	Out ForwarderDirection = "out"
)

type AddressStatus struct {
	IsReady        bool                `json:"isReady"`
	Phase          string              `json:"phase,omitempty"`
	Messages       []string            `json:"messages,omitempty"`
	BrokerStatuses []BrokerStatus      `json:"brokerStatus,omitempty"`
	PlanStatus     *AddressPlanStatus  `json:"planStatus,omitempty"`
	Forwarders     []ForwarderStatus   `json:"forwarders,omitempty"`
	Subscription   *SubscriptionStatus `json:"subscription,omitempty"`
}

type SubscriptionStatus struct {
	MaxConsumers *int `json:"maxConsumers,omitempty"`
}

type BrokerStatus struct {
	ClusterID   string `json:"clusterId,omitempty"`
	ContainerID string `json:"containerId,omitempty"`
	State       string `json:"state,omitempty"`
}

type AddressPlanStatus struct {
	Name       string             `json:"name,omitempty"`
	Partitions int                `json:"partitions,omitempty"`
	Resources  map[string]float64 `json:"resources,omitempty"`
}

type ForwarderStatus struct {
	Name     string   `json:"name"`
	IsReady  bool     `json:"isReady"`
	Messages []string `json:"messages,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AddressList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []Address `json:"items"`
}
