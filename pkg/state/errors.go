/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package state

type NotInitializedError struct{}

func (e *NotInitializedError) Error() string {
	return "not initialized"
}

func NewNotInitializedError() error {
	return &NotInitializedError{}
}
