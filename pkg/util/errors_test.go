/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"fmt"
	"github.com/pkg/errors"
	"go.uber.org/multierr"
	"testing"
)

func TestOnlyNonRecoverableErrors(t *testing.T) {
	for _, entry := range []struct {
		input  error
		output bool
	}{
		{input: fmt.Errorf("just a test"), output: false},
		{input: NewConfigurationError("just another test"), output: true},
		{input: WrapAsNonRecoverable(fmt.Errorf("just a test")), output: true},
		{input: multierr.Append(
			NewConfigurationError("test 1"),
			NewConfigurationError("test 2")),
			output: true},
		{input: multierr.Append(
			fmt.Errorf("just a test"),
			NewConfigurationError("test 2")),
			output: false},
		{input: multierr.Append(
			errors.Wrap(fmt.Errorf("just a test"), "it's a wrap"),
			NewConfigurationError("test 2")),
			output: false},
		{input: multierr.Append(
			fmt.Errorf("just a test"),
			errors.Wrap(NewConfigurationError("test 2"), "it's a wrap")),
			output: false},
		{input: multierr.Append(
			NewConfigurationError("test 1"),
			errors.Wrap(NewConfigurationError("test 2"), "it's a wrap")),
			output: true},
		{input: multierr.Append(
			errors.Wrap(NewConfigurationError("test 1"), "it's a wrap"),
			errors.Wrap(NewConfigurationError("test 2"), "it's a wrap")),
			output: true},
	} {

		if result := OnlyNonRecoverableErrors(entry.input); result != entry.output {
			t.Errorf("Wrong result - expected: %v, actual: %v, input: %v", entry.output, result, entry.input)
		}

	}
}

func TestOnlyNonRecoverableErrorsForNil(t *testing.T) {
	if result := OnlyNonRecoverableErrors(nil); result != true {
		t.Errorf("Wrong result - expected: %v, actual: %v, input: %v", true, result, nil)
	}
}
