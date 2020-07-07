/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

/*
 * Be careful with the comments in this file. The prefix "+" indicates that this is being processed
 * by the client generator. The location, empty lines, and other comments in this file may confuse
 * the generator, and produce a non-version version.
 */

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +kubebuilder:resource:shortName=msgusr;msguser;msgusers,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
type MessagingUser struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingUserSpec   `json:"spec,omitempty"`
	Status            MessagingUserStatus `json:"status,omitempty"`
}

type MessagingUserSpec struct {
	Password string `json:"password,omitempty"`
}

type MessagingUserStatus struct {
	// The current phase of the identity provider.
	Phase MessagingUserPhase `json:"phase,omitempty"`
}

type MessagingUserPhase string

const (
	MessagingUserPhaseConfiguring MessagingUserPhase = "Configuring"
	MessagingUserPhaseActive      MessagingUserPhase = "Active"
	MessagingUserPhaseTerminating MessagingUserPhase = "Terminating"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingUserList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []IdentityProvider `json:"items"`
}
