/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"os"
	"strconv"
)

func DefaultJavaRequiresNativeTls() bool {
	val := os.Getenv("DEFAULT_JAVA_REQUIRES_NATIVE_TLS")
	if val == "" {
		return true
	}

	bval, err := strconv.ParseBool(val)
	if err != nil {
		panic(err)
	}

	return bval
}
