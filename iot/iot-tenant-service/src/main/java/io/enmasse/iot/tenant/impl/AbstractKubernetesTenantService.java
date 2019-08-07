/*
 * Copyright 2018, 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.impl;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.hono.service.HealthCheckProvider;
import org.eclipse.hono.service.tenant.TenantService;
import org.springframework.beans.factory.annotation.Autowired;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;

public abstract class AbstractKubernetesTenantService implements TenantService, HealthCheckProvider, Verticle {

    private AtomicReference<DefaultKubernetesClient> client = new AtomicReference<>();

    protected TenantServiceConfigProperties configuration;

    private Vertx vertx;

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
    public void init(Vertx vertx, Context context) {
        this.vertx = vertx;
    }

    @Override
    public Vertx getVertx() {
        return this.vertx;
    }

    @Override
    public void start(Future<Void> startFuture) {
        runBlocking(this::createClient, startFuture);
    }

    @Override
    public void stop(final Future<Void> stopFuture) {
        runBlocking(this::disposeClient, stopFuture);
    }

    private synchronized void createClient() {
        setClient(new DefaultKubernetesClient());
    }

    private void disposeClient() {
        setClient(null);
    }

    private void setClient(final DefaultKubernetesClient newClient) {
        final DefaultKubernetesClient oldClient = this.client.getAndSet(newClient);
        if (oldClient != null) {
            oldClient.close();
        }
    }

    @Autowired
    public void setConfig(final TenantServiceConfigProperties configuration) {
        this.configuration = configuration;
    }

    @Override
    public void registerReadinessChecks(final HealthCheckHandler readinessHandler) {
        readinessHandler.register("hasClient", future -> {
            future.complete(this.client.get() != null ? Status.OK() : Status.KO());
        });
    }

    @Override
    public void registerLivenessChecks(final HealthCheckHandler livenessHandler) {
    }

    protected Optional<KubernetesClient> getClient() {
        return Optional.ofNullable(this.client.get());
    }

}