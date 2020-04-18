/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package state

import (
	"fmt"
)

type NotInitializedError struct{}

func (e *NotInitializedError) Error() string {
	return "infrastructure not yet initialized"
}

func NewNotInitializedError() error {
	return &NotInitializedError{}
}

func NewNotConnectedError(host string) error {
	return &NotConnectedError{host: host}
}

type NotConnectedError struct {
	host string
}

func (e *NotConnectedError) Error() string {
	return fmt.Sprintf("not yet connected to %s", e.host)
}
