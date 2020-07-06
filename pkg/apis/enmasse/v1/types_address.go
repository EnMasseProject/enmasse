/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1

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
// +kubebuilder:resource:shortName=msga;msgaddr;msgaddress;msgaddresses,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The current phase."
// +kubebuilder:printcolumn:name="Type",type="string",JSONPath=".status.type",description="The type."
// +kubebuilder:printcolumn:name="Message",type="string",JSONPath=".status.message",priority=1,description="Message describing the reason for the current Phase."
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
type MessagingAddress struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingAddressSpec   `json:"spec,omitempty"`
	Status            MessagingAddressStatus `json:"status,omitempty"`
}

type MessagingAddressSpec struct {
	Address *string `json:"address,omitempty"`
	// Anycast addresses are addresses without a broker intermediary.
	Anycast *MessagingAddressSpecAnycast `json:"anycast,omitempty"`
	// Multicast addresses are fan-out addresses without a broker intermediary.
	Multicast *MessagingAddressSpecMulticast `json:"multicast,omitempty"`
	// Queue addresses are addresses where messages are persisted on a broker.
	Queue *MessagingAddressSpecQueue `json:"queue,omitempty"`
	// Topic addresses are fan-out addresses with messages persisted on a broker.
	Topic *MessagingAddressSpecTopic `json:"topic,omitempty"`
	// Subscription addresses are durable subscription on a topic stored on a broker.
	Subscription *MessagingAddressSpecSubscription `json:"subscription,omitempty"`
	// DeadLetter addresses are stored on all brokers and can only be consumed from and referenced by queues.
	DeadLetter *MessagingAddressSpecDeadLetter `json:"deadLetter,omitempty"`
}

type MessagingAddressSpecAnycast struct {
}

type MessagingAddressSpecMulticast struct {
}

type MessagingAddressSpecQueue struct {
	// Dead letter address (must be address with type deadLetter)
	DeadLetterAddress string `json:"deadLetterAddress,omitempty"`
	// Expiry queue address (must be address with type deadLetter)
	ExpiryAddress string `json:"expiryAddress,omitempty"`
}

type MessagingAddressSpecTopic struct {
}

type MessagingAddressSpecSubscription struct {
	// Topic address this subscription should be subscribed to.
	Topic string `json:"topic"`
}

type MessagingAddressSpecDeadLetter struct {
}

type MessagingAddressStatus struct {
	// +kubebuilder:printcolumn
	Phase      MessagingAddressPhase       `json:"phase,omitempty"`
	Message    string                      `json:"message,omitempty"`
	Conditions []MessagingAddressCondition `json:"conditions,omitempty"`
	Brokers    []MessagingAddressBroker    `json:"brokers,omitempty"`
	Type       MessagingAddressType        `json:"type,omitempty"`
}

type MessagingAddressType string

const (
	MessagingAddressTypeQueue        MessagingAddressType = "Queue"
	MessagingAddressTypeTopic        MessagingAddressType = "Topic"
	MessagingAddressTypeAnycast      MessagingAddressType = "Anycast"
	MessagingAddressTypeMulticast    MessagingAddressType = "Multicast"
	MessagingAddressTypeSubscription MessagingAddressType = "Subscription"
	MessagingAddressTypeDeadLetter   MessagingAddressType = "DeadLetter"
)

type MessagingAddressBroker struct {
	State MessagingAddressBrokerState `json:"state,omitempty"`
	Host  string                      `json:"host,omitempty"`
}

type MessagingAddressBrokerState string

const (
	MessagingAddressBrokerScheduled MessagingAddressBrokerState = "Scheduled"
	MessagingAddressBrokerActive    MessagingAddressBrokerState = "Active"
)

type MessagingAddressCondition struct {
	Type               MessagingAddressConditionType `json:"type"`
	Status             corev1.ConditionStatus        `json:"status"`
	LastTransitionTime metav1.Time                   `json:"lastTransitionTime,omitempty"`
	Reason             string                        `json:"reason,omitempty"`
	Message            string                        `json:"message,omitempty"`
}

type MessagingAddressConditionType string

const (
	MessagingAddressFoundProject MessagingAddressConditionType = "FoundProject"
	MessagingAddressValidated   MessagingAddressConditionType = "Validated"
	MessagingAddressScheduled   MessagingAddressConditionType = "Scheduled"
	MessagingAddressCreated     MessagingAddressConditionType = "Created"
	MessagingAddressReady       MessagingAddressConditionType = "Ready"
)

type MessagingAddressPhase string

const (
	MessagingAddressConfiguring MessagingAddressPhase = "Configuring"
	MessagingAddressActive      MessagingAddressPhase = "Active"
	MessagingAddressTerminating MessagingAddressPhase = "Terminating"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingAddressList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []MessagingAddress `json:"items"`
}
