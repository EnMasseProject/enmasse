/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1

import (
	v1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
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
// +kubebuilder:resource:shortName=msgi;msginfra;msginfras;messaginginfras,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The current phase."
// +kubebuilder:printcolumn:name="Message",type="string",JSONPath=".status.message",priority=1,description="Message describing the reason for the current Phase."
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
type MessagingInfrastructure struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingInfrastructureSpec   `json:"spec,omitempty"`
	Status            MessagingInfrastructureStatus `json:"status,omitempty"`
}

type MessagingInfrastructureSpec struct {
	// A selector defining which namespaces this infra should serve. Default is all namespaces.
	NamespaceSelector *NamespaceSelector `json:"namespaceSelector,omitempty"`
	// Router configuration options.
	Router MessagingInfrastructureSpecRouter `json:"router,omitempty"`
	// Broker configuration options.
	Broker MessagingInfrastructureSpecBroker `json:"broker,omitempty"`

	AccessControl MessagingInfrastructureSpecAccessControl `json:"accessControl,omitempty"`
}

type MessagingInfrastructureSpecRouter struct {
	// Router image to use instead of default image.
	Image *v1beta1.ImageOverride `json:"image,omitempty"`
	// Strategy for scaling the routers. Default is 'static'.
	ScalingStrategy *MessagingInfrastructureSpecRouterScalingStrategy `json:"scalingStrategy,omitempty"`
}

type MessagingInfrastructureSpecRouterScalingStrategy struct {
	// Strategy which configures a static number of router pods.
	Static *MessagingInfrastructureSpecRouterScalingStrategyStatic `json:"static,omitempty"`
}

type MessagingInfrastructureSpecRouterScalingStrategyStatic struct {
	// The number of router replicas to create.
	Replicas int32 `json:"replicas"`
}

type MessagingInfrastructureSpecBroker struct {
	// Broker init image to use instead of default image.
	InitImage *v1beta1.ImageOverride `json:"initImage,omitempty"`
	// Broker image to use instead of default image.
	Image *v1beta1.ImageOverride `json:"image,omitempty"`
	// Strategy for scaling the brokers. Default is 'static'.
	ScalingStrategy *MessagingInfrastructureSpecBrokerScalingStrategy `json:"scalingStrategy,omitempty"`
}

type MessagingInfrastructureSpecBrokerScalingStrategy struct {
	// Scaler which configures a static number of broker pods.
	Static *MessagingInfrastructureSpecBrokerScalingStrategyStatic `json:"static,omitempty"`
}

type MessagingInfrastructureSpecBrokerScalingStrategyStatic struct {
	// The number of brokers to create.
	PoolSize int32 `json:"poolSize"`
}

type MessagingInfrastructureSpecAccessControl struct {
	Replicas *int32 `json:"replicas,omitempty"`
}

type MessagingInfrastructureStatus struct {
	// +kubebuilder:printcolumn
	Phase      MessagingInfrastructurePhase          `json:"phase,omitempty"`
	Message    string                                `json:"message,omitempty"`
	Conditions []MessagingInfrastructureCondition    `json:"conditions,omitempty"`
	Routers    []MessagingInfrastructureStatusRouter `json:"routers,omitempty"`
	Brokers    []MessagingInfrastructureStatusBroker `json:"brokers,omitempty"`
}

type MessagingInfrastructureStatusRouter struct {
	Host string `json:"host"`
}

type MessagingInfrastructureStatusBroker struct {
	Host string `json:"host"`
}

type MessagingInfrastructureCondition struct {
	Type               MessagingInfrastructureConditionType `json:"type"`
	Status             corev1.ConditionStatus               `json:"status"`
	LastTransitionTime metav1.Time                          `json:"lastTransitionTime,omitempty"`
	Reason             string                               `json:"reason,omitempty"`
	Message            string                               `json:"message,omitempty"`
}

type MessagingInfrastructureConditionType string

const (
	MessagingInfrastructureReady                MessagingInfrastructureConditionType = "Ready"
	MessagingInfrastructureCaCreated            MessagingInfrastructureConditionType = "CaCreated"
	MessagingInfrastructureCertCreated          MessagingInfrastructureConditionType = "CertCreated"
	MessagingInfrastructureAccessControlCreated MessagingInfrastructureConditionType = "AccessControlCreated"
	MessagingInfrastructureBrokersCreated       MessagingInfrastructureConditionType = "BrokersCreated"
	MessagingInfrastructureRoutersCreated       MessagingInfrastructureConditionType = "RoutersCreated"
	MessagingInfrastructureSynchronized         MessagingInfrastructureConditionType = "Synchronized"
	MessagingInfrastructureBrokersConnected     MessagingInfrastructureConditionType = "BrokersConnected"
)

type MessagingInfrastructurePhase string

const (
	MessagingInfrastructurePending     MessagingInfrastructurePhase = "Pending"
	MessagingInfrastructureConfiguring MessagingInfrastructurePhase = "Configuring"
	MessagingInfrastructureActive      MessagingInfrastructurePhase = "Active"
	MessagingInfrastructureTerminating MessagingInfrastructurePhase = "Terminating"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingInfrastructureList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []MessagingInfrastructure `json:"items"`
}
