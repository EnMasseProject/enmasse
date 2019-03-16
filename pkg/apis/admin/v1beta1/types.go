/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1beta1

import (
	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	corev1 "k8s.io/api/core/v1"
	resource "k8s.io/apimachinery/pkg/api/resource"
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
}

type AuthenticationServiceStatus struct {
	Host             string                  `json:"host,omitempty"`
	Port             int                     `json:"port,omitempty"`
	CaCertSecret     *corev1.SecretReference `json:"caCertSecret,omitempty"`
	ClientCertSecret *corev1.SecretReference `json:"clientCertSecret,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AuthenticationServiceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []AuthenticationService `json:"items"`
}
