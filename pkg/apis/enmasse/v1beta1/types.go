/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1beta1

import (
	"encoding/json"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

/*
 * Be careful with the comments in this file. The prefix "+" indicates that this is being processed
 * by the client generator. The location, empty lines, and other comments in this file may confuse
 * the generator, and produce a non-version version.
 */

// ** AddressSpace

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AddressSpace struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   AddressSpaceSpec   `json:"spec"`
	Status AddressSpaceStatus `json:"status"`
}

type AddressSpaceSpec struct {
	Plan string `json:"plan"`
	Type string `json:"type"`

	AuthenticationService *AuthenticationService `json:"authenticationService,omitempty"`

	Ednpoints []EndpointSpec `json:"endpoints,omitempty"`
}

type AuthenticationService struct {
	Type    string          `json:"type"`
	Details json.RawMessage `json:"details,omitempty"`
}

type Detail struct {
	StringValue  *string
	FloatValue   *float64
	BooleanValue *bool
}

func (d *Detail) UnmarshalJSON(b []byte) error {
	return nil
}

func (d Detail) MarshalJSON() ([]byte, error) {
	if d.StringValue != nil {
		return json.Marshal(d.StringValue)
	} else if d.FloatValue != nil {
		return json.Marshal(d.FloatValue)
	} else if d.BooleanValue != nil {
		return json.Marshal(d.BooleanValue)
	} else {
		return []byte("null"), nil
	}
}

type EndpointSpec struct {
	Name        string           `json:"name"`
	Service     string           `json:"service"`
	Certificate *CertificateSpec `json:"cert,omitempty"`
	Expose      *ExposeSpec      `json:"expose,omitempty"`
}

type CertificateSpec struct {
	Provider string `json:"provider"`
}

type ExposeSpec struct {
	Type                string `json:"route"`
	RouteServicePort    string `json:"routeServicePort"`
	RouteTlsTermination string `json:"routeTlsTermination"`
}

type AddressSpaceStatus struct {
	IsReady bool `json:"isReady"`

	CACertificate  []byte           `json:"caCert,omitempty"`
	EndpointStatus []EndpointStatus `json:"endpointStatuses,omitempty"`
}

type EndpointStatus struct {
	Name        string `json:"name"`
	Certificate []byte `json:"cert,omitempty"`

	ServiceHost  string `json:"serviceHost"`
	ServicePorts []Port `json:"servicePorts,omitempty"`

	ExternalHost  string `json:"externalHost"`
	ExternalPorts []Port `json:"externalPorts,omitempty"`
}

type Port struct {
	Name string `json:"name"`
	Port uint16 `json:"port"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AddressSpaceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []AddressSpace `json:"items"`
}

// ** Address

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +genclient:noStatus

type Address struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   AddressSpec   `json:"spec"`
	Status AddressStatus `json:"status,omitempty"`
}

type AddressStatus struct {
	IsReady        bool              `json:"isReady"`
	Phase          string            `json:"phase,omitempty"`
	Messages       []string          `json:"messages,omitempty"`
	BrokerStatuses []BrokerStatus    `json:"brokerStatus,omitempty"`
	PlanStatus     AddressPlanStatus `json:"planStatus,omitempty"`
}

type BrokerStatus struct {
	ClusterID   string `json:"clusterId,omitempty"`
	ContainerID string `json:"containerId,omitempty"`
	State       string `json:"state,omitempty"`
}

type AddressPlanStatus struct {
	Name       string             `json:"name,omitempty"`
	Partitions int                `json:"partitions,omitempty"`
	Resources  map[string]float64 `json:"resources,omitempty"`
}

type AddressSpec struct {
	Address      string `json:"address"`
	AddressSpace string `json:"addressSpace,omitempty"`
	Type         string `json:"type"`
	Plan         string `json:"plan"`
	Topic        string `json:"topic,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type AddressList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []Address `json:"items"`
}
