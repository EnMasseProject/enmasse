/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"fmt"
	"strings"
)

func SplitUserName(name string) (string, string, error) {
	tokens := strings.Split(name, ".")

	if len(tokens) != 2 {
		return "", "", fmt.Errorf("username in wrong format, must be <addressspace>.<name>: %v", name)
	}

	return tokens[0], tokens[1], nil
}
