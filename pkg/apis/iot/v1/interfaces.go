/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1

type CommonJavaContainerOptions interface {
	IsNativeTlsRequired(config *IoTConfig) bool
	TlsVersions(config *IoTConfig) []string
}

// ensure we implement the interface

var _ CommonJavaContainerOptions = &CommonAdapterConfig{}
var _ CommonJavaContainerOptions = &CommonServiceConfig{}
