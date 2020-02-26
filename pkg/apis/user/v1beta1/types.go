/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1beta1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingUser struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   MessagingUserSpec   `json:"spec"`
	Status MessagingUserStatus `json:"status,omitempty"`
}

type MessagingUserSpec struct {
	Username       string              `json:"username"`
	Authentication AuthenticationSpec  `json:"authentication"`
	Authorization  []AuthorizationSpec `json:"authorization,omitempty"`
}

type UserPhase string

const (
	UserPending     UserPhase = "Pending"
	UserConfiguring UserPhase = "Configuring"
	UserActive      UserPhase = "Active"
	UserTerminating UserPhase = "Terminating"
)

type MessagingUserStatus struct {
	Phase      UserPhase `json:"phase,omitempty"`
	Message    string    `json:"message,omitempty"`
	Generation int64     `json:"generation,omitempty"`
}

type AuthenticationType string

const (
	Password       AuthenticationType = "password"
	Federated      AuthenticationType = "federated"
	ServiceAccount AuthenticationType = "serviceaccount"
)

type AuthenticationSpec struct {
	Type     AuthenticationType `json:"type"`
	Password []byte             `json:"password,omitempty"`
	Provider string             `json:"provider,omitempty"`
}

type AuthorizationOperation string

const (
	Send   AuthorizationOperation = "send"
	Recv   AuthorizationOperation = "recv"
	View   AuthorizationOperation = "view"
	Manage AuthorizationOperation = "manage"
)

var Operations = map[string]AuthorizationOperation{
	"send":   Send,
	"recv":   Recv,
	"view":   View,
	"manage": Manage,
}

type AuthorizationSpec struct {
	Addresses  []string                 `json:"addresses,omitempty"`
	Operations []AuthorizationOperation `json:"operations"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingUserList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []MessagingUser `json:"items"`
}
