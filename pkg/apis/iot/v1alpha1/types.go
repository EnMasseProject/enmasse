/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"github.com/enmasseproject/enmasse/pkg/util"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type IoTProject struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   IoTProjectSpec   `json:"spec"`
	Status IoTProjectStatus `json:"status"`
}

// Get the IoT tenant name from an IoT project.
// This is not in any way encoded
func (project *IoTProject) TenantName() string {
	return util.TenantName(project.Namespace, project.Name)
}

type IoTProjectSpec struct {
	DownstreamStrategy DownstreamStrategy `json:"downstreamStrategy"`
}

type IoTProjectStatus struct {
	IsReady            bool                        `json:"isReady"`
	DownstreamEndpoint *ExternalDownstreamStrategy `json:"downstreamEndpoint,omitempty"`

	// FIXME: add conditions
}

type DownstreamStrategy struct {
	ExternalDownstreamStrategy *ExternalDownstreamStrategy `json:"externalStrategy,omitempty"`
	ProvidedDownstreamStrategy *ProvidedDownstreamStrategy `json:"providedStrategy,omitempty"`
	ManagedDownstreamStrategy  *ManagedDownstreamStrategy  `json:"managedStrategy,omitempty"`
}

type ProvidedDownstreamStrategy struct {
	Namespace        string `json:"namespace"`
	AddressSpaceName string `json:"addressSpaceName"`

	Credentials `json:",inline"`

	EndpointMode *EndpointMode `json:"endpointMode,omitempty"`
	EndpointName string        `json:"endpointName,omitempty"`
	PortName     string        `json:"portName,omitempty"`
	TLS          *bool         `json:"tls,omitempty"`
}

type ManagedDownstreamStrategy struct {
	AddressSpaceName string `json:"addressSpaceName"`
}

type ExternalDownstreamStrategy struct {
	Host string `json:"host"`
	Port uint16 `json:"port"`

	Credentials `json:",inline"`

	TLS         bool   `json:"tls"`
	Certificate []byte `json:"certificate,omitempty"`
}

type Credentials struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type IoTProjectList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"'`

	Items []IoTProject `json:"items"`
}
