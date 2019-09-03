/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import "testing"

func TestMax(t *testing.T) {
	if Max(0, 1) != 1 {
		t.Error("Max failed")
	}
}

func TestMin(t *testing.T) {
	if Min(0, 1) != 0 {
		t.Error("Min failed")
	}
}
