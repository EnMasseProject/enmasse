/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

/* Reuses implementation from https://github.com/vcabbage/amqp */

package amqp

import (
	"errors"
	"fmt"
)

// Default stdlib-based error functions.
var (
	errorNew    = errors.New
	errorErrorf = fmt.Errorf
	errorWrapf  = func(err error, _ string, _ ...interface{}) error { return err }
)
