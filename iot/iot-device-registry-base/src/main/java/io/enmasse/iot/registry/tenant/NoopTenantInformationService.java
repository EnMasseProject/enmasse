/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.registry.tenant;

import io.opentracing.Span;
import io.vertx.core.Future;

import org.eclipse.hono.deviceregistry.service.tenant.TenantInformationService;
import org.eclipse.hono.deviceregistry.service.tenant.TenantKey;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import java.net.HttpURLConnection;
import java.util.Optional;

public class NoopTenantInformationService implements TenantInformationService {

    @Override
    public Future<Result<TenantKey>> tenantExists(String tenantId, Span span) {
        return Future.succeededFuture(OperationResult.ok(HttpURLConnection.HTTP_OK,
                TenantKey.from(tenantId),
                Optional.empty(),
                Optional.empty()));
    }
}
