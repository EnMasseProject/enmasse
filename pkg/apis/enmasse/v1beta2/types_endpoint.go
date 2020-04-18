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
// +kubebuilder:subresource:status
type MessagingEndpoint struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingEndpointSpec   `json:"spec,omitempty"`
	Status            MessagingEndpointStatus `json:"status,omitempty"`
}

type MessagingEndpointSpec struct {
	// Tls configuration for this endpoint.
	Tls *MessagingEndpointSpecTls `json:"tls,omitempty"`

	Protocols []MessagingEndpointSpecProtocol `json:"protocols,omitempty"`

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

type MessagingEndpointSpecProtocol string

const (
	MessagingProtocolAMQP    MessagingEndpointSpecProtocol = "AMQP"
	MessagingProtocolAMQPS   MessagingEndpointSpecProtocol = "AMQPS"
	MessagingProtocolAMQPWS  MessagingEndpointSpecProtocol = "AMQP-WS"
	MessagingProtocolAMQPWSS MessagingEndpointSpecProtocol = "AMQP-WSS"
)

type MessagingEndpointSpecCluster struct {
	RequireTls bool `json:"requireTls,omitempty"`
}

type MessagingEndpointSpecIngress struct {
}

type MessagingEndpointSpecRoute struct {
}

type MessagingEndpointSpecNodePort struct {
	RequireTls bool `json:"requireTls,omitempty"`
}

type MessagingEndpointSpecLoadBalancer struct {
	RequireTls bool `json:"requireTls,omitempty"`
}

type MessagingEndpointStatus struct {
	// +kubebuilder:printcolumn
	Phase      MessagingEndpointPhase       `json:"phase,omitempty"`
	Message    string                       `json:"message,omitempty"`
	Conditions []MessagingEndpointCondition `json:"conditions,omitempty"`
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
	MessagingEndpointReady MessagingEndpointConditionType = "Ready"
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
