/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"fmt"
	"go.uber.org/multierr"
)

//region non recoverable error concept

type causer interface {
	Cause() error
}

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
		log.V(2).Info("Checking error", "cause", me)
		if isNonRecoverable(me) {
			nonrecoverable++
		}
	}
	le := len(errs)
	log.V(2).Info("Checking if all errors are non-recoverable", "nonRecoverable", nonrecoverable, "all", le)
	return nonrecoverable == le
}

// walk up the cause chain and find out if we have a non-recoverable error
func isNonRecoverable(err error) bool {
	for err != nil {
		if _, ok := err.(NonRecoverableError); ok {
			return true
		}
		cause, ok := err.(causer)
		if !ok {
			break
		}
		err = cause.Cause()
	}
	return false
}

//endregion

//region Wrapped

type WrappedNonRecoverableError struct {
	cause error
}

func (e *WrappedNonRecoverableError) Error() string {
	return "Wrapped Non-Recoverable Error: " + e.cause.Error()
}

func (e WrappedNonRecoverableError) Cause() error {
	return e.cause
}

func (e WrappedNonRecoverableError) nonRecoverable() {}

var _ error = &WrappedNonRecoverableError{}
var _ NonRecoverableError = &WrappedNonRecoverableError{}
var _ causer = &WrappedNonRecoverableError{}

// Wrap any error as non-recoverable. This allows
// having a proper cause chain, but still be able to flag certain errors
// as non-recoverable.
func WrapAsNonRecoverable(err error) error {
	if err == nil {
		return nil
	}

	return &WrappedNonRecoverableError{cause: err}
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
