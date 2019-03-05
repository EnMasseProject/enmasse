/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"time"
)

func Max(x, y int64) int64 {
	if x < y {
		return y
	}
	return x
}

func MaxDuration(x, y time.Duration) time.Duration {
	return time.Duration(Max(int64(x), int64(y)))
}
