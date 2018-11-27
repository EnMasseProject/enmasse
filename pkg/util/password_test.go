/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"fmt"
	"testing"
)

func TestLen(t *testing.T) {

	for i := 0; i < 10; i++ {
		pwd, err := GeneratePassword(i)
		if err != nil {
			t.Error("Failed to generate password", err)
		}
		pl := len([]rune(pwd))
		fmt.Println(pwd)
		if pl != i {
			t.Errorf("Expected PWD of length %v, but was length %v", i, pl)
		}
	}

}
