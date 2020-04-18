/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1beta2

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

/*
 * Be careful with the comments in this file. The prefix "+" indicates that this is being processed
 * by the client generator. The location, empty lines, and other comments in this file may confuse
 * the generator, and produce a non-version version.
 */

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +kubebuilder:resource:shortName=msgep;msgendpoint;msgendpoints,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The current phase."
// +kubebuilder:printcolumn:name="Host",type="string",JSONPath=".status.host",description="The hostname."
// +kubebuilder:printcolumn:name="Message",type="string",JSONPath=".status.message",priority=1,description="Message describing the reason for the current Phase."
// +kubebuilder:printcolumn:name="Protocols",type="string",JSONPath=".spec.protocols",priority=1,description="Supported protocols."
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
	// Create self-signed certificates.
	SelfSigned *MessagingEndpointSpecTlsSelfSigned `json:"selfsigned,omitempty"`
	// Creates cluster-internal certificates on OpenShift.
	OpenShift *MessagingEndpointSpecTlsOpenShift `json:"openshift,omitempty"`
	// Uses certificates from a provided secret.
	Secret *MessagingEndpointSpecTlsSecret `json:"secret,omitempty"`
}

type MessagingEndpointSpecTlsSelfSigned struct {
}

type MessagingEndpointSpecTlsOpenShift struct {
}

type MessagingEndpointSpecTlsSecret struct {
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
}

type MessagingEndpointSpecNodePort struct {
}

type MessagingEndpointSpecLoadBalancer struct {
}

type MessagingEndpointStatus struct {
	// +kubebuilder:printcolumn
	Phase         MessagingEndpointPhase       `json:"phase,omitempty"`
	Message       string                       `json:"message,omitempty"`
	Conditions    []MessagingEndpointCondition `json:"conditions,omitempty"`
	Host          string                       `json:"host,omitempty"`
	Ports         []MessagingEndpointPort      `json:"ports,omitempty"`
	InternalPorts []MessagingEndpointPort      `json:"internalPorts,omitempty"`
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
	MessagingEndpointFoundTenant    MessagingEndpointConditionType = "FoundTenant"
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
