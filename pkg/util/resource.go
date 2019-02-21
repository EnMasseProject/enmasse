/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"fmt"

	"k8s.io/apimachinery/pkg/apis/meta/v1"
)

// An error indicating that the resource is not ready yet
type objectNotReadyYetError struct {
	object v1.Object
}

// ensure we implement the interface
var _ error = &objectNotReadyYetError{}

func NewObjectNotReadyYetError(object v1.Object) *objectNotReadyYetError {
	return &objectNotReadyYetError{object}
}

func (e *objectNotReadyYetError) Error() string {
	return fmt.Sprintf("Object is not ready yet: %s/%s",
		e.object.GetNamespace(), e.object.GetName(),
	)
}

// Test if this is a NotReadyYetError error
func IsNotReadyYetError(err error) bool {
	switch err.(type) {
	case *objectNotReadyYetError:
		return true
	}
	return false
}
