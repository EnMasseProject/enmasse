/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.service.base;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Callable;
import org.eclipse.hono.service.HealthCheckProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;

public abstract class AbstractKubernetesBasedService extends AbstractVerticle implements HealthCheckProvider {

    private static final Logger logger = LoggerFactory.getLogger(AbstractKubernetesBasedService.class);

    private DefaultKubernetesClient client;

    protected <T> Future<T> runBlocking(final Runnable runnable) {
        final Future<T> future = Future.future();
        runBlocking(runnable, future);
        return future;
    }

    protected <T> void runBlocking(final Runnable runnable, final Handler<AsyncResult<T>> handler) {
        this.vertx.executeBlocking(future -> {
            try {
                runnable.run();
                future.complete();
            } catch (final Exception e) {
                future.fail(e);
            }
        }, handler);
    }

    protected <T> Future<T> callBlocking(final Callable<T> callable) {
        final Future<T> future = Future.future();
        callBlocking(callable, future);
        return future;
    }

    protected <T> void callBlocking(final Callable<T> callable, final Handler<AsyncResult<T>> handler) {
        this.vertx.executeBlocking(future -> {
            try {
                future.complete(callable.call());
            } catch (final Exception e) {
                future.fail(e);
            }
        }, handler);
    }

    @Override
    public final void start(final Future<Void> startFuture) {
        doStart()
                .<Void>mapEmpty()
                .setHandler(startFuture);
    }

    @Override
    public final void stop(final Future<Void> stopFuture) {
        doStop()
                .<Void>mapEmpty()
                .setHandler(stopFuture);
    }

    protected Future<?> doStart() {
        return runBlocking(this::createClient);
    }

    protected Future<?> doStop() {
        return runBlocking(this::disposeClient);
    }

    private void createClient() {
        this.client = new DefaultKubernetesClient();
    }

    private void disposeClient() {
        try {
            if (this.client != null) {
                this.client.close();
            }
        } finally {
            this.client = null;
        }
    }

    @Override
    public void registerReadinessChecks(final HealthCheckHandler readinessHandler) {
        readinessHandler.register("hasClient", future -> {
            future.complete(this.client != null ? Status.OK() : Status.KO());
        });
    }

    @Override
    public void registerLivenessChecks(final HealthCheckHandler livenessHandler) {}

    protected NamespacedKubernetesClient getClient() {
        return this.client;
    }

    protected <T> Future<T> withClient(final KubernetesOperation<T> operation) {
        final Future<T> future = Future.future();
        withClient(operation, future);
        return future;
    }

    protected <T> void withClient(final KubernetesOperation<T> operation, final Handler<AsyncResult<T>> resultHandler) {

        requireNonNull(operation);
        requireNonNull(resultHandler);

        callBlocking(() -> {
            try {
                return operation.run(this.client);
            } catch (final Exception e) {
                logger.info("Failed to execute operation", e);
                throw e;
            }
        }, resultHandler);

    }

}
