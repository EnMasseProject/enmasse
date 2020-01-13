/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.tenant;

import java.util.concurrent.CompletableFuture;

import io.opentracing.Span;

/**
 * A service which provides tenant information to internal service implementations.
 */
public interface TenantInformationService {

    public CompletableFuture<TenantInformation> tenantExists(String tenantName, int notFoundStatusCode, Span span);

}
