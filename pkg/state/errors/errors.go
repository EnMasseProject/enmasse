/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package errors

import (
	"fmt"
)

var (
	BrokerInUseError     error = fmt.Errorf("broker in use")
	ProjectNotFoundError error = fmt.Errorf("project not found")
	NotInitializedError  error = fmt.Errorf("not initialized")
	NotSyncedError       error = fmt.Errorf("not synchronized")
	NoEndpointsError     error = fmt.Errorf("no endpoints")
	NotDeletedError      error = fmt.Errorf("error deleting")
	ResourceInUseError   error = fmt.Errorf("resource is in use")
)
