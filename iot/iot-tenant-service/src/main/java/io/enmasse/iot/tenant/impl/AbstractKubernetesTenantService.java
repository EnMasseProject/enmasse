/*
 * Copyright 2018, 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.impl;

import static io.enmasse.iot.service.base.utils.MoreFutures.completeHandler;

import java.util.concurrent.CompletableFuture;

import javax.security.auth.x500.X500Principal;

import org.eclipse.hono.service.tenant.TenantService;
import org.eclipse.hono.util.TenantResult;
import org.springframework.beans.factory.annotation.Autowired;

import io.enmasse.iot.service.base.AbstractProjectBasedService;
import io.opentracing.Span;
import io.opentracing.noop.NoopSpan;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public abstract class AbstractKubernetesTenantService extends AbstractProjectBasedService implements TenantService {

    protected TenantServiceConfigProperties configuration;

    protected abstract CompletableFuture<TenantResult<JsonObject>> processGet(String tenantName, Span span);
    protected abstract CompletableFuture<TenantResult<JsonObject>> processGet(X500Principal subjectDn, Span span);

    @Autowired
    public void setConfig(final TenantServiceConfigProperties configuration) {
        this.configuration = configuration;
    }

    @Override
    public void get(final String tenantId, final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {
        get(tenantId, NoopSpan.INSTANCE, resultHandler);
    }

    @Override
    public void get(final X500Principal subjectDn, final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {
        get(subjectDn, NoopSpan.INSTANCE, resultHandler);
    }

    @Override
    public void get(final String tenantId, final Span span, final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {
        completeHandler(() -> processGet(tenantId, span), resultHandler);
    }

    @Override
    public void get(final X500Principal subjectDn, final Span span, final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {
        completeHandler(() ->processGet(subjectDn, span), resultHandler);
    }
}