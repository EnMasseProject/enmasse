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
type MessagingAddress struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingAddressSpec   `json:"spec,omitempty"`
	Status            MessagingAddressStatus `json:"status,omitempty"`
}

type MessagingAddressSpec struct {
	// The address type defines the messaging semanitcs for this addreess.
	Type    MessagingAddressType `json:"type"`
	Address *string              `json:"address,omitempty"`
}

type MessagingAddressType string

const (
	// The anycast type provides peer-to-peer messaging via a message router.
	MessagingAddressAnycast MessagingAddressType = "anycast"
	// The multicast type provides fanout messaging via a message router.
	MessagingAddressMulticast MessagingAddressType = "multicast"
	// The queue type provides store and forward messaging using a message broker.
	MessagingAddressQueue MessagingAddressType = "queue"
	// The queue type provides store and forward pub-sub messaging using a message broker.
	MessagingAddressTopic MessagingAddressType = "topic"
	// The queue type provides store and forward durable subscription using a message broker.
	MessagingAddressSubscription MessagingAddressType = "subscription"
)

type MessagingAddressStatus struct {
	// +kubebuilder:printcolumn
	Phase      MessagingAddressPhase       `json:"phase,omitempty"`
	Message    string                      `json:"message,omitempty"`
	Conditions []MessagingAddressCondition `json:"conditions,omitempty"`
	Brokers    []MessagingAddressBroker    `json:"brokers,omitempty"`
}

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
	MessagingAddressReady MessagingAddressConditionType = "Ready"
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
