/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.tenant;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.client.ServiceInvocationException;

import io.opentracing.Span;

public class DummyTenantInformationService implements TenantInformationService {

    /**
     * Checks if a tenant exists.
     * <br>
     * @param tenantId The tenant to check, must not be {@code null}.
     * @param notFoundStatusCode The status code, which will be used if the tenant was not found.
     * @param span The tracing span to use, must not be {@code null}.
     * @return A future which completes when the tenant exists, and fails in case the tenant does not exists. The
     * future will fail with a {@link ServiceInvocationException}, which was the provided "not found" status code set.
     */
    @Override
    public CompletableFuture<?> tenantExists(final String tenantId, int notFoundStatusCode, final Span span) {
        Objects.requireNonNull(tenantId);

        return CompletableFuture.completedFuture(null);
    }

}
