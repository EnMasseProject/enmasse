/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.tenant;

import io.opentracing.SpanContext;
import io.vertx.core.Future;

/**
 * A service which provides tenant information to internal service implementations.
 */
public interface TenantInformationService {

    public Future<TenantInformation> tenantExists(String tenantName, int notFoundStatusCode, SpanContext span);

}
