/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1beta1

import (
	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AuthenticationService struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   AuthenticationServiceSpec   `json:"spec"`
	Status AuthenticationServiceStatus `json:"status"`
}

type AuthenticationServiceSpec struct {
	Type     AuthenticationServiceType          `json:"type"`
	Realm    *string                            `json:"realm,omitempty"`
	None     *AuthenticationServiceSpecNone     `json:"none,omitempty"`
	Standard *AuthenticationServiceSpecStandard `json:"standard,omitempty"`
	External *AuthenticationServiceSpecExternal `json:"external,omitempty"`
}

type AuthenticationServiceType string

const (
	None     AuthenticationServiceType = "none"
	Standard AuthenticationServiceType = "standard"
	External AuthenticationServiceType = "external"
)

type AuthenticationServiceSpecNone struct {
	CertificateSecret *corev1.SecretReference       `json:"certificateSecret,omitempty"`
	Image             *enmassev1beta1.ImageOverride `json:"image,omitempty"`
	Resources         *corev1.ResourceRequirements  `json:"resources,omitempty"`
}

type AuthenticationServiceSpecStandard struct {
	CredentialsSecret  *corev1.SecretReference                      `json:"credentialsSecret,omitempty"`
	CertificateSecret  *corev1.SecretReference                      `json:"certificateSecret,omitempty"`
	ServiceAccountName *string                                      `json:"serviceAccountName,omitempty"`
	DeploymentName     *string                                      `json:"deploymentName,omitempty"`
	ServiceName        *string                                      `json:"serviceName,omitempty"`
	RouteName          *string                                      `json:"routeName,omitempty"`
	Image              *enmassev1beta1.ImageOverride                `json:"image,omitempty"`
	InitImage          *enmassev1beta1.ImageOverride                `json:"initImage,omitempty"`
	JvmOptions         *string                                      `json:"jvmOptions,omitempty"`
	Resources          *corev1.ResourceRequirements                 `json:"resources,omitempty"`
	Storage            *AuthenticationServiceSpecStandardStorage    `json:"storage,omitempty"`
	Datasource         *AuthenticationServiceSpecStandardDatasource `json:"datasource,omitempty"`
	SecurityContext    *corev1.PodSecurityContext                   `json:"securityContext,omitempty"`
}

type StorageType string

const (
	Ephemeral       StorageType = "ephemeral"
	PersistentClaim StorageType = "persistent-claim"
)

type AuthenticationServiceSpecStandardStorage struct {
	Type        StorageType           `json:"type"`
	Class       *string               `json:"class,omitempty"`
	ClaimName   *string               `json:"claimName,omitempty"`
	Selector    *metav1.LabelSelector `json:"selector,omitempty"`
	DeleteClaim *bool                 `json:"deleteClaim,omitempty"`
	Size        resource.Quantity     `json:"size,omitempty"`
}

type AuthenticationServiceSpecStandardDatasource struct {
	Type              DatasourceType         `json:"type"`
	Host              string                 `json:"host,omitempty"`
	Port              int                    `json:"port,omitempty"`
	Database          string                 `json:"database,omitempty"`
	CredentialsSecret corev1.SecretReference `json:"credentialsSecret,omitempty"`
}

type DatasourceType string

const (
	H2Datasource         DatasourceType = "h2"
	PostgresqlDatasource DatasourceType = "postgresql"
)

type AuthenticationServiceSpecExternal struct {
	Host             string                  `json:"host"`
	Port             int                     `json:"port"`
	CaCertSecret     *corev1.SecretReference `json:"caCertSecret,omitempty"`
	ClientCertSecret *corev1.SecretReference `json:"clientCertSecret,omitempty"`
	AllowOverride    bool                    `json:"allowOverride,omitempty"`
}

type AuthenticationServicePhase string

const (
	AuthenticationServicePending     AuthenticationServicePhase = "Pending"
	AuthenticationServiceConfiguring AuthenticationServicePhase = "Configuring"
	AuthenticationServiceActive      AuthenticationServicePhase = "Active"
)

type AuthenticationServiceStatus struct {
	Phase            AuthenticationServicePhase `json:"phase,omitempty"`
	Message          string                     `json:"message,omitempty"`
	Host             string                     `json:"host,omitempty"`
	Port             int                        `json:"port,omitempty"`
	CaCertSecret     *corev1.SecretReference    `json:"caCertSecret,omitempty"`
	ClientCertSecret *corev1.SecretReference    `json:"clientCertSecret,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AuthenticationServiceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []AuthenticationService `json:"items"`
}

// ** ConsoleService

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type ConsoleService struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              ConsoleServiceSpec   `json:"spec"`
	Status            ConsoleServiceStatus `json:"status"`
}

type ConsoleServiceSpec struct {
	Replicas             *int32                           `json:"replicas,omitempty"`
	DiscoveryMetadataURL *string                          `json:"discoveryMetadataURL,omitempty"`
	Scope                *string                          `json:"scope,omitempty"`
	OauthClientSecret    *corev1.SecretReference          `json:"oauthClientSecret,omitempty"`
	CertificateSecret    *corev1.SecretReference          `json:"certificateSecret,omitempty"`
	SsoCookieSecret      *corev1.SecretReference          `json:"ssoCookieSecret,omitempty"`
	SsoCookieDomain      *string                          `json:"ssoCookieDomain,omitempty"`
	Host                 *string                          `json:"host,omitempty"`
	OauthProxy           *ConsoleServiceOauthProxySpec    `json:"oauthProxy,omitempty"`
	ConsoleServer        *ConsoleServiceConsoleServerSpec `json:"consoleServer,omitempty"`
}

type ConsoleServiceOauthProxySpec struct {
	Resources *corev1.ResourceRequirements `json:"resources,omitempty"`
}

type ConsoleServiceConsoleServerSpec struct {
	Resources      *corev1.ResourceRequirements            `json:"resources,omitempty"`
	Session        *ConsoleServiceConsoleServerSessionSpec `json:"session,omitempty"`
	LivenessProbe  *corev1.Probe                           `json:"livenessProbe,omitempty"`
	ReadinessProbe *corev1.Probe                           `json:"readinessProbe,omitempty"`
}

type ConsoleServiceConsoleServerSessionSpec struct {
	Lifetime    *string `json:"lifetime,omitempty"`
	IdleTimeout *string `json:"idleTimeout,omitempty"`
}

type ConsoleServiceStatus struct {
	Host         string                  `json:"host,omitempty"`
	Port         int                     `json:"port,omitempty"`
	CaCertSecret *corev1.SecretReference `json:"caCertSecret,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type ConsoleServiceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []ConsoleService `json:"items"`
}
