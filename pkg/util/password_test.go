/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"testing"
)

func TestLenPassword(t *testing.T) {

	for i := 0; i < 1024; i++ {
		pwd, err := GeneratePassword(i)
		if err != nil {
			t.Error("Failed to generate password", err)
		}
		rl := len([]rune(pwd))
		if rl != i {
			t.Errorf("Expected PWD of length %v (runes), but was length %v: '%s'", i, rl, pwd)
		}
		bl := len([]byte(pwd))
		if bl != i {
			t.Errorf("Expected PWD of length %v (bytes), but was length %v: '%s'", i, bl, pwd)
		}
	}

}
