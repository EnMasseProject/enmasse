/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package errors

import (
	"fmt"
)

type NotFoundError struct {
	kind      string
	name      string
	namespace string
}

func NewNotFoundError(kind string, name string, namespace string) error {
	return &NotFoundError{kind: kind, name: name, namespace: namespace}
}

func IsNotFound(err error) bool {
	_, ok := err.(*NotFoundError)
	return ok
}

func (e *NotFoundError) Error() string {
	return fmt.Sprintf("%s %s in namespace %s not found", e.kind, e.name, e.namespace)
}

type NotBoundError struct {
	namespace string
}

func NewNotBoundError(namespace string) error {
	return &NotBoundError{namespace: namespace}
}

func IsNotBound(err error) bool {
	_, ok := err.(*NotBoundError)
	return ok
}

func (e *NotBoundError) Error() string {
	return fmt.Sprintf("MessagingTenant in namespace %s not bound to any infrastructure", e.namespace)
}
