/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import "testing"

var userTests = []struct {
	input        string
	addressSpace string
	username     string
	err          bool
}{
	{"user", "", "", true},
	{"addressspace.user", "addressspace", "user", false},
}

func TestSplitUser(t *testing.T) {
	for _, tt := range userTests {
		addressSpace, username, err := splitUserName(tt.input)

		if tt.err && err == nil {
			t.Errorf("splitUserName(%s): expected error", tt.input)
		}
		if tt.addressSpace != addressSpace {
			t.Errorf("splitUserName(%s): addressSpace - expected: %v", tt.input, tt.addressSpace)
		}
		if tt.username != username {
			t.Errorf("splitUserName(%s): username - expected: %v", tt.input, tt.username)
		}
	}
}
