// Code generated by github.com/99designs/gqlgen, DO NOT EDIT.

package resolvers

import (
	"fmt"
	"io"
	"strconv"

	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
)

type AddressQueryResultConsoleapiEnmasseIoV1beta1 struct {
	Total     int                             `json:"total"`
	Addresses []*consolegraphql.AddressHolder `json:"addresses"`
}

type AddressSpaceQueryResultConsoleapiEnmasseIoV1beta1 struct {
	Total         int                                  `json:"total"`
	AddressSpaces []*consolegraphql.AddressSpaceHolder `json:"addressSpaces"`
}

type AddressSpaceTypeSpecConsoleapiEnmasseIoV1beta1 struct {
	AddressSpaceType AddressSpaceType `json:"addressSpaceType"`
	DisplayName      string           `json:"displayName"`
	LongDescription  string           `json:"longDescription"`
	ShortDescription string           `json:"shortDescription"`
	DisplayOrder     int              `json:"displayOrder"`
}

type AddressSpaceTypeConsoleapiEnmasseIoV1beta1 struct {
	ObjectMeta *v1.ObjectMeta                             `json:"metadata"`
	Spec       *AddressTypeSpecConsoleapiEnmasseIoV1beta1 `json:"spec"`
}

type AddressTypeSpecConsoleapiEnmasseIoV1beta1 struct {
	AddressType      AddressType      `json:"addressType"`
	AddressSpaceType AddressSpaceType `json:"addressSpaceType"`
	DisplayName      string           `json:"displayName"`
	LongDescription  string           `json:"longDescription"`
	ShortDescription string           `json:"shortDescription"`
	DisplayOrder     int              `json:"displayOrder"`
}

type AddressTypeConsoleapiEnmasseIoV1beta1 struct {
	ObjectMeta *v1.ObjectMeta                             `json:"metadata"`
	Spec       *AddressTypeSpecConsoleapiEnmasseIoV1beta1 `json:"spec"`
}

type ConnectionQueryResultConsoleapiEnmasseIoV1beta1 struct {
	Total       int                          `json:"total"`
	Connections []*consolegraphql.Connection `json:"connections"`
}

type KeyValue struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

type LinkQueryResultConsoleapiEnmasseIoV1beta1 struct {
	Total int                    `json:"total"`
	Links []*consolegraphql.Link `json:"links"`
}

type AddressSpaceType string

const (
	AddressSpaceTypeStandard AddressSpaceType = "standard"
	AddressSpaceTypeBrokered AddressSpaceType = "brokered"
)

var AllAddressSpaceType = []AddressSpaceType{
	AddressSpaceTypeStandard,
	AddressSpaceTypeBrokered,
}

func (e AddressSpaceType) IsValid() bool {
	switch e {
	case AddressSpaceTypeStandard, AddressSpaceTypeBrokered:
		return true
	}
	return false
}

func (e AddressSpaceType) String() string {
	return string(e)
}

func (e *AddressSpaceType) UnmarshalGQL(v interface{}) error {
	str, ok := v.(string)
	if !ok {
		return fmt.Errorf("enums must be strings")
	}

	*e = AddressSpaceType(str)
	if !e.IsValid() {
		return fmt.Errorf("%s is not a valid AddressSpaceType", str)
	}
	return nil
}

func (e AddressSpaceType) MarshalGQL(w io.Writer) {
	fmt.Fprint(w, strconv.Quote(e.String()))
}

type AddressType string

const (
	AddressTypeQueue        AddressType = "queue"
	AddressTypeTopic        AddressType = "topic"
	AddressTypeSubscription AddressType = "subscription"
	AddressTypeMulticast    AddressType = "multicast"
	AddressTypeAnycast      AddressType = "anycast"
)

var AllAddressType = []AddressType{
	AddressTypeQueue,
	AddressTypeTopic,
	AddressTypeSubscription,
	AddressTypeMulticast,
	AddressTypeAnycast,
}

func (e AddressType) IsValid() bool {
	switch e {
	case AddressTypeQueue, AddressTypeTopic, AddressTypeSubscription, AddressTypeMulticast, AddressTypeAnycast:
		return true
	}
	return false
}

func (e AddressType) String() string {
	return string(e)
}

func (e *AddressType) UnmarshalGQL(v interface{}) error {
	str, ok := v.(string)
	if !ok {
		return fmt.Errorf("enums must be strings")
	}

	*e = AddressType(str)
	if !e.IsValid() {
		return fmt.Errorf("%s is not a valid AddressType", str)
	}
	return nil
}

func (e AddressType) MarshalGQL(w io.Writer) {
	fmt.Fprint(w, strconv.Quote(e.String()))
}

type AuthenticationServiceType string

const (
	AuthenticationServiceTypeNone     AuthenticationServiceType = "none"
	AuthenticationServiceTypeStandard AuthenticationServiceType = "standard"
)

var AllAuthenticationServiceType = []AuthenticationServiceType{
	AuthenticationServiceTypeNone,
	AuthenticationServiceTypeStandard,
}

func (e AuthenticationServiceType) IsValid() bool {
	switch e {
	case AuthenticationServiceTypeNone, AuthenticationServiceTypeStandard:
		return true
	}
	return false
}

func (e AuthenticationServiceType) String() string {
	return string(e)
}

func (e *AuthenticationServiceType) UnmarshalGQL(v interface{}) error {
	str, ok := v.(string)
	if !ok {
		return fmt.Errorf("enums must be strings")
	}

	*e = AuthenticationServiceType(str)
	if !e.IsValid() {
		return fmt.Errorf("%s is not a valid AuthenticationServiceType", str)
	}
	return nil
}

func (e AuthenticationServiceType) MarshalGQL(w io.Writer) {
	fmt.Fprint(w, strconv.Quote(e.String()))
}

type LinkRole string

const (
	LinkRoleSender   LinkRole = "sender"
	LinkRoleReceiver LinkRole = "receiver"
)

var AllLinkRole = []LinkRole{
	LinkRoleSender,
	LinkRoleReceiver,
}

func (e LinkRole) IsValid() bool {
	switch e {
	case LinkRoleSender, LinkRoleReceiver:
		return true
	}
	return false
}

func (e LinkRole) String() string {
	return string(e)
}

func (e *LinkRole) UnmarshalGQL(v interface{}) error {
	str, ok := v.(string)
	if !ok {
		return fmt.Errorf("enums must be strings")
	}

	*e = LinkRole(str)
	if !e.IsValid() {
		return fmt.Errorf("%s is not a valid LinkRole", str)
	}
	return nil
}

func (e LinkRole) MarshalGQL(w io.Writer) {
	fmt.Fprint(w, strconv.Quote(e.String()))
}

type MetricType string

const (
	MetricTypeGauge   MetricType = "gauge"
	MetricTypeCounter MetricType = "counter"
	MetricTypeRate    MetricType = "rate"
)

var AllMetricType = []MetricType{
	MetricTypeGauge,
	MetricTypeCounter,
	MetricTypeRate,
}

func (e MetricType) IsValid() bool {
	switch e {
	case MetricTypeGauge, MetricTypeCounter, MetricTypeRate:
		return true
	}
	return false
}

func (e MetricType) String() string {
	return string(e)
}

func (e *MetricType) UnmarshalGQL(v interface{}) error {
	str, ok := v.(string)
	if !ok {
		return fmt.Errorf("enums must be strings")
	}

	*e = MetricType(str)
	if !e.IsValid() {
		return fmt.Errorf("%s is not a valid MetricType", str)
	}
	return nil
}

func (e MetricType) MarshalGQL(w io.Writer) {
	fmt.Fprint(w, strconv.Quote(e.String()))
}

type Protocol string

const (
	ProtocolAmqp  Protocol = "amqp"
	ProtocolAmqps Protocol = "amqps"
)

var AllProtocol = []Protocol{
	ProtocolAmqp,
	ProtocolAmqps,
}

func (e Protocol) IsValid() bool {
	switch e {
	case ProtocolAmqp, ProtocolAmqps:
		return true
	}
	return false
}

func (e Protocol) String() string {
	return string(e)
}

func (e *Protocol) UnmarshalGQL(v interface{}) error {
	str, ok := v.(string)
	if !ok {
		return fmt.Errorf("enums must be strings")
	}

	*e = Protocol(str)
	if !e.IsValid() {
		return fmt.Errorf("%s is not a valid Protocol", str)
	}
	return nil
}

func (e Protocol) MarshalGQL(w io.Writer) {
	fmt.Fprint(w, strconv.Quote(e.String()))
}
