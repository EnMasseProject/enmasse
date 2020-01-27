/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import "testing"

func TestContains(t *testing.T) {

	var data = []struct {
		strings []string
		value   string
		found   bool
	}{
		{[]string{}, "a", false},
		{[]string{"a"}, "a", true},
		{[]string{"a", "b", "c"}, "a", true},
		{[]string{"a", "b", "c"}, "b", true},
		{[]string{"a", "b", "c"}, "c", true},
		{[]string{"a", "b", "c"}, "d", false},
	}

	for _, d := range data {
		if ContainsString(d.strings, d.value) != d.found {
			t.Errorf("Failed result - strings: %v, value: %v, expectedResult: %v", d.strings, d.value, d.found)
		}
	}

}
