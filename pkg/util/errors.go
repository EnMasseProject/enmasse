/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"fmt"
	"github.com/pkg/errors"
	"go.uber.org/multierr"
)

//region non recoverable error concept
type NonRecoverableError interface {
	// adding a dummy method, so that the interface only applies to selected structs.
	// poor man's marker interface, learned from Go AST: https://golang.org/src/go/ast/ast.go
	nonRecoverable()
}

// Returns true if all errors covered by this error implement the NonRecoverableError interface.
//
// If the error is a simple error, the only the one error will be checked. If the error is a "multierr"
// error, then all recorded errors will be checked.
//
// If the error is "nil", then "true" is being returned.
func OnlyNonRecoverableErrors(err error) bool {
	nonrecoverable := 0
	errs := multierr.Errors(err)
	for _, me := range errs {
		cause := errors.Cause(me)
		log.V(2).Info("Checking error", "cause", cause)
		if _, ok := cause.(NonRecoverableError); ok {
			nonrecoverable++
		}
	}
	le := len(errs)
	log.V(2).Info("Checking if all errors are non-recoverable", "nonRecoverable", nonrecoverable, "all", le)
	return nonrecoverable == le
}

//endregion

//region Configuration error

// an error in the configuration, non-recoverable
type ConfigurationError struct {
	Reason string
}

func (e *ConfigurationError) Error() string {
	return "Configuration Error: " + e.Reason
}

func NewConfigurationError(format string, a ...interface{}) error {
	return &ConfigurationError{Reason: fmt.Sprintf(format, a...)}
}

func (e ConfigurationError) nonRecoverable() {}

var _ error = &ConfigurationError{}
var _ NonRecoverableError = &ConfigurationError{}

//endregion
