/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package state

import (
	"fmt"
)

var (
	NotInitializedError error = fmt.Errorf("not initialized")
	NotSyncedError      error = fmt.Errorf("not synchronized")
	NoEndpointsError    error = fmt.Errorf("no endpoints")
	NotDeletedError     error = fmt.Errorf("error deleting")
)
