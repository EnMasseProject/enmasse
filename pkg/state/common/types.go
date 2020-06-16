/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package common

/**
 * Represents a Kubernetes host and corresponding pod IP
 */
type Host struct {
	Hostname string
	Ip       string
}
