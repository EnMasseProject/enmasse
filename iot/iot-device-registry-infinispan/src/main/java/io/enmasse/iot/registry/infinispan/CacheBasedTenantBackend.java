/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import io.opentracing.Span;
import java.util.Optional;

import javax.security.auth.x500.X500Principal;

import org.eclipse.hono.service.management.Id;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.tenant.Tenant;
import org.eclipse.hono.service.management.tenant.TenantBackend;
import org.eclipse.hono.util.TenantResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;

/**
 * A tenant service that keeps all data in memory but is backed by a file.
 * <p>
 * On startup this adapter loads all registered tenants from a file. On shutdown all tenants kept in memory are written
 * to the file.
 *
 * This class is just a proxy to the {@link CacheTenantService}. It's purpose is to maintain the same architecture for Device,
 * Credential and Tenant API.
 */
@Repository
@Qualifier("backend")
@ConditionalOnProperty(name = "hono.app.type", havingValue = "file", matchIfMissing = true)
public final class CacheBasedTenantBackend extends AbstractVerticle implements TenantBackend, Verticle {

    private final CacheTenantService tenantService;

    /**
     * Create a new instance.
     *
     * @param tenantService an implementation of tenant service.
     */
    @Autowired
    public CacheBasedTenantBackend(
            @Qualifier("serviceImpl") final CacheTenantService tenantService) {
        this.tenantService = tenantService;
    }

    // Tenant management API

    @Override
    public void add(final Optional<String> tenantId, final JsonObject tenantObj,
            final Span span, final Handler<AsyncResult<OperationResult<Id>>> resultHandler) {
        tenantService.add(tenantId, tenantObj, span, resultHandler);
    }

    @Override
    public void read(final String tenantId, final Span span, final Handler<AsyncResult<OperationResult<Tenant>>> resultHandler) {
        tenantService.read(tenantId, span, resultHandler);
    }

    @Override
    public void update(final String tenantId, final JsonObject tenantObj, final Optional<String> resourceVersion,
            final Span span, final Handler<AsyncResult<OperationResult<Void>>> resultHandler) {
        tenantService.update(tenantId, tenantObj, resourceVersion, span, resultHandler);
    }

    @Override
    public void remove(final String tenantId, final Optional<String> resourceVersion, final Span span,
            final Handler<AsyncResult<Result<Void>>> resultHandler) {
        tenantService.remove(tenantId, resourceVersion, span, resultHandler);
    }

    // Tenant AMQP API

    @Override
    public void get(final String tenantId, final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {
        tenantService.get(tenantId, resultHandler);
    }

    @Override
    public void get(final X500Principal subjectDn, final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {
        tenantService.get(subjectDn, resultHandler);
    }
}
