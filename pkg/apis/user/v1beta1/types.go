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

	Spec MessagingUserSpec `json:"spec"`
}

type MessagingUserSpec struct {
	Username       string              `json:"username"`
	Authentication AuthenticationSpec  `json:"authentication"`
	Authorization  []AuthorizationSpec `json:"authorization,omitempty"`
}

type AuthenticationSpec struct {
	Type     string `json:"type"`
	Password []byte `json:"password,omitempty"`
	Provider string `json:"provider,omitempty"`
}

type AuthorizationSpec struct {
	Addresses  []string `json:"addresses"`
	Operations []string `json:"operations"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingUserList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []MessagingUser `json:"items"`
}
