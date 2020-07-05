/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// ** MessagingConsole

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +kubebuilder:resource:shortName=msgc;msgconsole;msgconsoles,categories=enmasse
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Host",type="string",JSONPath=".status.host",description="Console hostname"
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
type MessagingConsole struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MessagingConsoleSpec   `json:"spec"`
	Status            MessagingConsoleStatus `json:"status"`
}

type MessagingConsoleSpec struct {
	Replicas             *int32                             `json:"replicas,omitempty"`
	DiscoveryMetadataURL *string                            `json:"discoveryMetadataURL,omitempty"`
	Scope                *string                            `json:"scope,omitempty"`
	Impersonation        *MessagingConsoleImpersonationSpec `json:"impersonation,omitempty"`
	OauthClientSecret    *corev1.SecretReference            `json:"oauthClientSecret,omitempty"`
	CertificateSecret    *corev1.SecretReference            `json:"certificateSecret,omitempty"`
	SsoCookieSecret      *corev1.SecretReference            `json:"ssoCookieSecret,omitempty"`
	SsoCookieDomain      *string                            `json:"ssoCookieDomain,omitempty"`
	Host                 *string                            `json:"host,omitempty"`
	OauthProxy           *MessagingConsoleOauthProxySpec    `json:"oauthProxy,omitempty"`
	ConsoleServer        *MessagingConsoleConsoleServerSpec `json:"consoleServer,omitempty"`
}

type MessagingConsoleImpersonationSpec struct {
	UserHeader string `json:"userHeader"`
}

type MessagingConsoleOauthProxySpec struct {
	ExtraArgs []string                     `json:"extraArgs,omitempty"`
	Resources *corev1.ResourceRequirements `json:"resources,omitempty"`
}

type MessagingConsoleConsoleServerSpec struct {
	Resources      *corev1.ResourceRequirements              `json:"resources,omitempty"`
	Session        *MessagingConsoleConsoleServerSessionSpec `json:"session,omitempty"`
	LivenessProbe  *corev1.Probe                             `json:"livenessProbe,omitempty"`
	ReadinessProbe *corev1.Probe                             `json:"readinessProbe,omitempty"`
}

type MessagingConsoleConsoleServerSessionSpec struct {
	Lifetime    *string `json:"lifetime,omitempty"`
	IdleTimeout *string `json:"idleTimeout,omitempty"`
}

type MessagingConsoleStatus struct {
	Host         string                  `json:"host,omitempty"`
	Port         int                     `json:"port,omitempty"`
	CaCertSecret *corev1.SecretReference `json:"caCertSecret,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MessagingConsoleList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []MessagingConsole `json:"items"`
}
