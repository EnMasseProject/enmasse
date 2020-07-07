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
// +kubebuilder:resource:shortName=idpr;idprovider;idproviders,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The current phase."
// +kubebuilder:printcolumn:name="Type",type="string",JSONPath=".status.type",description="The provider type."
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
type IdentityProvider struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              IdentityProviderSpec   `json:"spec,omitempty"`
	Status            IdentityProviderStatus `json:"status,omitempty"`
}

type IdentityProviderSpec struct {
	// An identity provide support anonymous authentication
	AnonymousProvider *IdentityProviderSpecAnonymousProvider `json:"anonymousProvider,omitempty"`
	// An identity provide support anonymous authentication
	NamespaceProvider *IdentityProviderSpecNamespaceProvider `json:"namespaceProvider,omitempty"`
}

type IdentityProviderSpecAnonymousProvider struct {
}

type IdentityProviderSpecNamespaceProvider struct {
}

type IdentityProviderStatus struct {
	// The current phase of the identity provider.
	Phase IdentityProviderPhase `json:"phase,omitempty"`
	// The identityprovider type.
	Type IdentityProviderType `json:"type,omitempty"`
	// Status messages for the identityprovider.
	Message string `json:"message,omitempty"`
}

type IdentityProviderPhase string

const (
	IdentityProviderPhaseConfiguring IdentityProviderPhase = "Configuring"
	IdentityProviderPhaseActive      IdentityProviderPhase = "Active"
	IdentityProviderPhaseTerminating IdentityProviderPhase = "Terminating"
)

type IdentityProviderType string

const (
	IdentityProviderTypeAnonymous IdentityProviderType = "Anonymous"
	IdentityProviderTypeNamespace IdentityProviderType = "Namespace"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type IdentityProviderList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []IdentityProvider `json:"items"`
}
