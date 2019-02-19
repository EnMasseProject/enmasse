/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"bytes"
	"encoding/json"
)

type EndpointMode int

const (
	Service EndpointMode = iota
	External
)

func (et EndpointMode) String() string {
	return toString[et]
}

var toString = map[EndpointMode]string{
	Service:  "service",
	External: "external",
}

var toId = map[string]EndpointMode{
	"service":  Service,
	"external": External,
}

func (et *EndpointMode) MarshalJSON() ([]byte, error) {
	buffer := bytes.NewBufferString(`"`)
	buffer.WriteString(toString[*et])
	buffer.WriteString(`"`)
	return buffer.Bytes(), nil
}

func (et *EndpointMode) UnmarshalJSON(data []byte) error {
	var value string
	err := json.Unmarshal(data, &value)

	if err != nil {
		return err
	}

	*et = toId[value]

	return nil
}
