/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	routev1 "github.com/openshift/api/route/v1"
)

/*
 * Be careful with the comments in this file. The prefix "+" indicates that this is being processed
 * by the client generator. The location, empty lines, and other comments in this file may confuse
 * the generator, and produce a non-version version.
 */

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +kubebuilder:resource:shortName=msge;msgendpoint;msgendpoints,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The current phase."
// +kubebuilder:printcolumn:name="Type",type="string",JSONPath=".status.type",description="The endpoint type."
// +kubebuilder:printcolumn:name="Host",type="string",JSONPath=".status.host",description="The hostname."
// +kubebuilder:printcolumn:name="Message",type="string",JSONPath=".status.message",priority=1,description="Message describing the reason for the current Phase."
// +kubebuilder:printcolumn:name="Protocols",type="string",JSONPath=".spec.protocols",priority=1,description="Supported protocols."
// +kubebuilder:printcolumn:name="CertificateExpiry",type="string",JSONPath=".status.tls.certificateValidity.notAfter",priority=1,description="Certificate expiry."
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
type MessagingEndpoint struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingEndpointSpec   `json:"spec,omitempty"`
	Status            MessagingEndpointStatus `json:"status,omitempty"`
}

type MessagingEndpointSpec struct {
	// Tls configuration for this endpoint.
	Tls *MessagingEndpointSpecTls `json:"tls,omitempty"`

	// Annotations to apply to the endpoint objects.
	Annotations map[string]string `json:"annotations,omitempty"`

	// Protocols that should be supported by this endpoint.
	Protocols []MessagingEndpointProtocol `json:"protocols"`

	// Hostname to use for endpoint (default assigned based on type.)
	Host *string `json:"host,omitempty"`

	// A cluster endpoint creates an endpoint available through a ClusterIP service.
	Cluster *MessagingEndpointSpecCluster `json:"cluster,omitempty"`

	// A cluster endpoint creates an endpoint available through ingress.
	Ingress *MessagingEndpointSpecIngress `json:"ingress,omitempty"`

	// A cluster endpoint creates an endpoint available through an OpenShift route.
	Route *MessagingEndpointSpecRoute `json:"route,omitempty"`

	// A cluster endpoint creates an endpoint available through a NodePort service.
	NodePort *MessagingEndpointSpecNodePort `json:"nodePort,omitempty"`

	// A cluster endpoint creates an endpoint available through a LoadBalancer service.
	LoadBalancer *MessagingEndpointSpecLoadBalancer `json:"loadBalancer,omitempty"`
}

type MessagingEndpointSpecTls struct {
	// Which TLS protocols that should be enabled for this endpoint.
	Protocols *string `json:"protocols,omitempty"`
	// Which TLS ciphers that should be enabled for this endpoint.
	Ciphers *string `json:"ciphers,omitempty"`
	// Create self-signed certificates.
	Selfsigned *MessagingEndpointSpecTlsSelfsigned `json:"selfsigned,omitempty"`
	// Creates cluster-internal certificates on OpenShift.
	Openshift *MessagingEndpointSpecTlsOpenshift `json:"openshift,omitempty"`
	// Uses certificates from a provided secret.
	External *MessagingEndpointSpecTlsExternal `json:"external,omitempty"`
}

type MessagingEndpointSpecTlsSelfsigned struct {
}

type MessagingEndpointSpecTlsOpenshift struct {
}

type MessagingEndpointSpecTlsExternal struct {
	// The private key of the certificate.
	Key InputValue `json:"key"`
	// The certificate value.
	Certificate InputValue `json:"certificate"`
}

type InputValue struct {
	// Raw input value
	Value string `json:"value,omitempty"`

	// Source for the value stored in a secret
	ValueFromSecret *corev1.SecretKeySelector `json:"valueFromSecret,omitempty"`
}

type MessagingEndpointProtocol string

const (
	MessagingProtocolAMQP    MessagingEndpointProtocol = "AMQP"
	MessagingProtocolAMQPS   MessagingEndpointProtocol = "AMQPS"
	MessagingProtocolAMQPWS  MessagingEndpointProtocol = "AMQP-WS"
	MessagingProtocolAMQPWSS MessagingEndpointProtocol = "AMQP-WSS"
)

type MessagingEndpointSpecCluster struct {
}

type MessagingEndpointSpecIngress struct {
}

type MessagingEndpointSpecRoute struct {
	TlsTermination *routev1.TLSTerminationType `json:"tlsTermination,omitempty"`
}

type MessagingEndpointSpecNodePort struct {
}

type MessagingEndpointSpecLoadBalancer struct {
}

type MessagingEndpointStatus struct {
	// The current phase of the endpoint.
	Phase MessagingEndpointPhase `json:"phase,omitempty"`
	// The endpoint type.
	Type MessagingEndpointType `json:"type,omitempty"`
	// Status messages for the endpoint.
	Message string `json:"message,omitempty"`
	// Conditions and their status for the endpoint.
	Conditions []MessagingEndpointCondition `json:"conditions,omitempty"`
	// The hostname used to connect to this endpoint.
	Host string `json:"host,omitempty"`
	// The ports that can be used for this endpoint.
	Ports []MessagingEndpointPort `json:"ports,omitempty"`
	// TLS status for this endpoint.
	Tls           *MessagingEndpointStatusTls `json:"tls,omitempty"`
	InternalPorts []MessagingEndpointPort     `json:"internalPorts,omitempty"`
}

type MessagingEndpointType string

const (
	MessagingEndpointTypeCluster      MessagingEndpointType = "Cluster"
	MessagingEndpointTypeNodePort     MessagingEndpointType = "NodePort"
	MessagingEndpointTypeLoadBalancer MessagingEndpointType = "LoadBalancer"
	MessagingEndpointTypeRoute        MessagingEndpointType = "Route"
	MessagingEndpointTypeIngress      MessagingEndpointType = "Ingress"
)

type MessagingEndpointStatusTls struct {
	// Certificate info.
	CertificateValidity *MessagingEndpointCertValidity `json:"certificateValidity,omitempty"`
	// CA certificate if provided by certificate type.
	CaCertificate string `json:"caCertificate,omitempty"`
}

type MessagingEndpointCertValidity struct {
	NotBefore metav1.Time `json:"notBefore,omitempty"`
	NotAfter  metav1.Time `json:"notAfter,omitempty"`
}

type MessagingEndpointPort struct {
	Name     string                    `json:"name,omitempty"`
	Protocol MessagingEndpointProtocol `json:"protocol,omitempty"`
	Port     int                       `json:"port,omitempty"`
}

type MessagingEndpointCondition struct {
	Type               MessagingEndpointConditionType `json:"type"`
	Status             corev1.ConditionStatus         `json:"status"`
	LastTransitionTime metav1.Time                    `json:"lastTransitionTime,omitempty"`
	Reason             string                         `json:"reason,omitempty"`
	Message            string                         `json:"message,omitempty"`
}

type MessagingEndpointConditionType string

const (
	MessagingEndpointFoundProject    MessagingEndpointConditionType = "FoundProject"
	MessagingEndpointConfiguredTls  MessagingEndpointConditionType = "ConfiguredTLS"
	MessagingEndpointAllocatedPorts MessagingEndpointConditionType = "AllocatedPorts"
	MessagingEndpointCreated        MessagingEndpointConditionType = "Created"
	MessagingEndpointServiceCreated MessagingEndpointConditionType = "ServiceCreated"
	MessagingEndpointReady          MessagingEndpointConditionType = "Ready"
)

type MessagingEndpointPhase string

const (
	MessagingEndpointConfiguring MessagingEndpointPhase = "Configuring"
	MessagingEndpointActive      MessagingEndpointPhase = "Active"
	MessagingEndpointTerminating MessagingEndpointPhase = "Terminating"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingEndpointList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []MessagingEndpoint `json:"items"`
}
