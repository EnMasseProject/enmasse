/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package qdr

import (
	"fmt"
	"testing"
)

func TestNotFound(t *testing.T) {
	var err error = &ResourceNotFoundError{}

	if IsNotFound(err) != true {
		t.Error("IsNotFound should return true")
	}
	if IsNotFound(fmt.Errorf("not found")) != false {
		t.Error("IsNotFound should return false")
	}
	if IsNotFound(nil) != false {
		t.Error("IsNotFound should return false")
	}
}
