/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.eclipse.hono.service.HealthCheckProvider;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import com.google.common.base.Throwables;

import io.enmasse.iot.service.base.utils.MoreFutures;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;

public abstract class AbstractInfinispanService implements HealthCheckProvider {

    private String serviceName;
    private AtomicLong errors = new AtomicLong();

    public AbstractInfinispanService(final String serviceName) {
        this.serviceName = serviceName;
    }

    protected <T> void completeHandler(final Supplier<CompletableFuture<T>> supplier, final Handler<AsyncResult<T>> handler) {
        MoreFutures.completeHandler(supplier, ar -> {
            try {
                checkResponseHealth(ar.cause());
            } finally {
                handler.handle(ar);
            }
        });
    }

    @Override
    public void registerLivenessChecks(final HealthCheckHandler livenessHandler) {
        livenessHandler.register(this.serviceName, f -> checkAlive().setHandler(f));
    }

    /**
     * Called for each operation with the exception
     *
     * @param error The error, may be {@code null} if there was no error.
     */
    protected void checkResponseHealth(final Throwable error) {
        if (wasClientError(error)) {
            this.errors.incrementAndGet();
        } else {
            this.errors.set(0);
        }
    }

    protected boolean wasClientError(final Throwable error) {
        return Throwables.getCausalChain(error)
                .stream()
                .filter(HotRodClientException.class::isInstance)
                .findAny()
                .isPresent();
    }

    /**
     * Check if we are alive.
     *
     * @return A future reporting the result.
     */
    private Future<Status> checkAlive() {
        final Status result;

        long num = this.errors.get();
        if (num == 0) {
            result = Status.OK();
        } else {
            result = Status.KO(
                    new JsonObject()
                            .put("errors", num));
        }

        return Future.succeededFuture(result);
    }

    @Override
    public void registerReadinessChecks(final HealthCheckHandler readinessHandler) {}

}
