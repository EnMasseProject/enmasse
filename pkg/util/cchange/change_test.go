/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cchange

import (
	"testing"
)

func TestStringEqual(t *testing.T) {
	change1 := NewRecorder()
	change2 := NewRecorder()

	change1.AddString("foo")
	change2.AddString("foo")

	if change1.HashString() != change2.HashString() {
		t.Errorf("Should be equal - 1: %s, 2: %s", change1.HashString(), change2.HashString())
	}
}

func TestMap1(t *testing.T) {
	change1 := NewRecorder()
	change2 := NewRecorder()

	change1.AddStringsFromMap(map[string]string{
		"baz": "123",
	})
	change2.AddStringsFromMap(map[string]string{
		"baz": "123",
	})

	if change1.HashString() != change2.HashString() {
		t.Error("Should be equal")
	}
}

func TestMap2(t *testing.T) {
	change1 := NewRecorder()
	change2 := NewRecorder()

	change1.AddStringsFromMap(map[string]string{
		"baz": "123",
		"foo": "456",
	}, "baz")
	change2.AddStringsFromMap(map[string]string{
		"baz": "123",
	})

	if change1.HashString() != change2.HashString() {
		t.Error("Should be equal")
	}
}
