/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"testing"

	"k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestNotReadyYetError(t *testing.T) {
	var err error
	err = NewObjectNotReadyYetError(&v1.ObjectMeta{})
	if !IsNotReadyYetError(err) {
		t.Error("IsNotReadyYetError(...) should return 'true'")
	}
}
