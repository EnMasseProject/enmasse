/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.health;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.hono.service.HealthCheckProvider;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.ServerStatistics;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;

public abstract class AbstractCacheHealthCheck implements HealthCheckProvider {

    private final String name;
    private final RemoteCache<?, ?> cache;
    private final Vertx vertx;

    public AbstractCacheHealthCheck(final String cacheName, final RemoteCache<?, ?> cache, final Vertx vertx) {
        this.name = cacheName;
        this.cache = cache;
        this.vertx = vertx;
    }

    protected String getName() {
        return "cache/" + this.name;
    }

    @Override
    public void registerReadinessChecks(final HealthCheckHandler readinessHandler) {}

    @Override
    public void registerLivenessChecks(final HealthCheckHandler livenessHandler) {
        livenessHandler.register(getName(), future -> {
            this.vertx.executeBlocking(future2 -> {

                try {
                    future2.complete(checkLivenessSync());
                } catch (Exception e) {
                    future2.fail(e);
                }

            }, false, future);
        });
    }

    /**
     * Check if the server is alive.
     * @return The status result.
     */
    protected Status checkLivenessSync() {

        try {
            final ServerStatistics result = this.cache.serverStatistics();
            if (result == null) {
                return KO("No server info", null);
            }
        } catch (final Exception e) {
            return KO("Failed to ping server", e);
        }

        return Status.OK();
    }

    /**
     * Convert a reason and exception to KO status.
     * @param reason The reason. Must not be {@code null}.
     * @param e The exception. May be {@code null}.
     * @return The status. Never is {@code null}.
     */
    protected static Status KO(final String reason, final Throwable e) {

        final JsonObject info = new JsonObject()
                .put("reason", reason);

        if ( e != null ) {
            info.put("message", e.getMessage());
            final StringWriter sw = new StringWriter();
            try (final PrintWriter pw = new PrintWriter(sw)){
                e.printStackTrace(pw);
            }
            info.put("exception", sw.toString());
        }

        return Status.KO(info);

    }
}
