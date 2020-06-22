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
import io.vertx.core.Promise;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;

public abstract class AbstractKubernetesBasedService extends AbstractVerticle implements HealthCheckProvider {

    private static final Logger logger = LoggerFactory.getLogger(AbstractKubernetesBasedService.class);

    private DefaultKubernetesClient client;

    protected <T> Future<T> runBlocking(final Runnable runnable) {
        final Promise<T> promise = Promise.promise();
        runBlocking(runnable, promise);
        return promise.future();
    }

    protected <T> void runBlocking(final Runnable runnable, final Handler<AsyncResult<T>> handler) {
        this.vertx.executeBlocking(promise -> {
            try {
                runnable.run();
                promise.complete();
            } catch (final Exception e) {
                promise.fail(e);
            }
        }, handler);
    }

    protected <T> Future<T> callBlocking(final Callable<T> callable) {
        final Promise<T> promise = Promise.promise();
        callBlocking(callable, promise);
        return promise.future();
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
    public void start(final Future<Void> startFuture) {
        doStart()
                .<Void>mapEmpty()
                .onComplete(startFuture);
    }

    @Override
    public void stop(final Future<Void> stopFuture) {
        doStop()
                .<Void>mapEmpty()
                .onComplete(stopFuture);
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
        final Promise<T> promise = Promise.promise();
        withClient(operation, promise);
        return promise.future();
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
