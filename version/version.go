/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package version

import (
	"os"
)

var (
	Version string
)

func init() {
	Version = os.Getenv("ENMASSE_VERSION")
	if Version == "" {
		Version = "latest"
	}
}
