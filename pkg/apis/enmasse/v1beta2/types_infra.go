/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1beta2

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
// +kubebuilder:resource:shortName=msgi;msginfra;msginfras,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The current phase."
// +kubebuilder:printcolumn:name="Message",type="string",JSONPath=".status.message",priority=1,description="Message describing the reason for the current Phase."
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
type MessagingInfra struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingInfraSpec   `json:"spec,omitempty"`
	Status            MessagingInfraStatus `json:"status,omitempty"`
}

type MessagingInfraSpec struct {
	// A selector defining which namespaces this infra should serve. Default is all namespaces.
	Selector *Selector `json:"selector,omitempty"`
	// Router configuration options.
	Router MessagingInfraSpecRouter `json:"router,omitempty"`
	// Broker configuration options.
	Broker MessagingInfraSpecBroker `json:"broker,omitempty"`
}

type MessagingInfraSpecRouter struct {
	// Router image to use instead of default image.
	Image *v1beta1.ImageOverride `json:"image,omitempty"`
	// Strategy for scaling the routers. Default is 'static'.
	ScalingStrategy *MessagingInfraSpecRouterScalingStrategy `json:"scalingStrategy,omitempty"`
}

type MessagingInfraSpecRouterScalingStrategy struct {
	// Strategy which configures a static number of router pods.
	Static *MessagingInfraSpecRouterScalingStrategyStatic `json:"static,omitempty"`
}

type MessagingInfraSpecRouterScalingStrategyStatic struct {
	// The number of router replicas to create.
	Replicas int32 `json:"replicas"`
}

type MessagingInfraSpecBroker struct {
	// Broker init image to use instead of default image.
	InitImage *v1beta1.ImageOverride `json:"initImage,omitempty"`
	// Broker image to use instead of default image.
	Image *v1beta1.ImageOverride `json:"image,omitempty"`
	// Strategy for scaling the brokers. Default is 'static'.
	ScalingStrategy *MessagingInfraSpecBrokerScalingStrategy `json:"scalingStrategy,omitempty"`
}

type MessagingInfraSpecBrokerScalingStrategy struct {
	// Scaler which configures a static number of broker pods.
	Static *MessagingInfraSpecBrokerScalingStrategyStatic `json:"static,omitempty"`
}

type MessagingInfraSpecBrokerScalingStrategyStatic struct {
	// The number of brokers to create.
	PoolSize int32 `json:"poolSize"`
}

type MessagingInfraStatus struct {
	// +kubebuilder:printcolumn
	Phase      MessagingInfraPhase       `json:"phase,omitempty"`
	Message    string                    `json:"message,omitempty"`
	Conditions []MessagingInfraCondition `json:"conditions,omitempty"`
}

type MessagingInfraCondition struct {
	Type               MessagingInfraConditionType `json:"type"`
	Status             corev1.ConditionStatus      `json:"status"`
	LastTransitionTime metav1.Time                 `json:"lastTransitionTime,omitempty"`
	Reason             string                      `json:"reason,omitempty"`
	Message            string                      `json:"message,omitempty"`
}

type MessagingInfraConditionType string

const (
	MessagingInfraReady            MessagingInfraConditionType = "Ready"
	MessagingInfraCaCreated        MessagingInfraConditionType = "CaCreated"
	MessagingInfraBrokersCreated   MessagingInfraConditionType = "BrokersCreated"
	MessagingInfraRoutersCreated   MessagingInfraConditionType = "RoutersCreated"
	MessagingInfraSynchronized     MessagingInfraConditionType = "Synchronized"
	MessagingInfraBrokersConnected MessagingInfraConditionType = "BrokersConnected"
)

type MessagingInfraPhase string

const (
	MessagingInfraPending     MessagingInfraPhase = "Pending"
	MessagingInfraConfiguring MessagingInfraPhase = "Configuring"
	MessagingInfraActive      MessagingInfraPhase = "Active"
	MessagingInfraTerminating MessagingInfraPhase = "Terminating"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingInfraList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []MessagingInfra `json:"items"`
}
