/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;

import io.opentracing.Span;
import io.opentracing.noop.NoopSpan;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.eclipse.hono.service.management.Id;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.tenant.ResourceLimits;
import org.eclipse.hono.service.management.tenant.Tenant;
import org.eclipse.hono.service.management.tenant.TenantBackend;
import org.eclipse.hono.service.management.tenant.TrustedCertificateAuthority;
import org.eclipse.hono.tracing.TracingHelper;
import org.eclipse.hono.util.TenantConstants;
import org.eclipse.hono.util.TenantObject;
import org.eclipse.hono.util.TenantResult;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import javax.security.auth.x500.X500Principal;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A tenant service that use an Infinispan as a backend service.
 * Infinispan is an open source project providing a distributed in-memory key/value data store
 *
 * <p>
 *@see <a href="https://infinspan.org">https://infinspan.org</a>
 *
 */
@Repository
@Primary
public class CacheTenantService extends AbstractVerticle implements TenantBackend, Verticle {

    private final RemoteCache<String, RegistryTenantObject> tenantsCache;

    @Autowired
    protected CacheTenantService(final RemoteCache<String, RegistryTenantObject> cache) {
        this.tenantsCache = cache;
    }

    @Override
    public void add(final Optional<String> paramTenantId, final JsonObject tenantObj, Span span,
            final Handler<AsyncResult<OperationResult<Id>>> resultHandler) {

        final String tenantId = paramTenantId.orElse(generateTenantId());

        final TenantObject tenantDetails = Optional.ofNullable(tenantObj)
                .map(json -> json.mapTo(TenantObject.class)).orElse(null);
        tenantDetails.setTenantId(tenantId);

        //verify if a duplicate cert exists
        if (tenantDetails.getTrustedCaSubjectDn() != null) {
            final RegistryTenantObject tenant = searchByCert(tenantDetails.getTrustedCaSubjectDn().getName());
            if (tenant != null) {
                TracingHelper.logError(span, "Conflict : CA already used by an existing tenant.");
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_CONFLICT)));
                return;
            }
        }

        final RegistryTenantObject tenant = new RegistryTenantObject(tenantDetails);
        tenantsCache.withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsentAsync(tenantId, tenant).thenAccept(result -> {
            if (result == null) {
                resultHandler.handle(Future.succeededFuture(
                        OperationResult.ok(HTTP_CREATED, Id.of(tenantId), Optional.empty(), Optional.of(tenant.getVersion()))));
            } else {
                TracingHelper.logError(span, "Conflict : tenantId already exists.");
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_CONFLICT)));
            }
        });

    }

    @Override
    public void update(final String tenantId, final JsonObject tenantObj, final Optional<String> resourceVersion,
           final Span span, final Handler<AsyncResult<OperationResult<Void>>> resultHandler) {

        final TenantObject tenantDetails = Optional.ofNullable(tenantObj)
                .map(json -> json.mapTo(TenantObject.class)).orElse(null);

        //verify if a duplicate cert exists
        if (tenantDetails.getTrustedCaSubjectDn() != null) {
            final RegistryTenantObject tenant = searchByCert(tenantDetails.getTrustedCaSubjectDn().getName());
            if (tenant != null) {
                TracingHelper.logError(span, "Conflict : CA already used by an existing tenant.");
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_CONFLICT)));
                return;
            }
        }

        tenantsCache.getAsync(tenantId).thenAccept(contained -> {
            if (contained != null) {

                if (versionCheck(contained, resourceVersion)) {

                        final RegistryTenantObject tenant = new RegistryTenantObject(tenantDetails);
                        tenantsCache.replaceAsync(tenantId, tenant).thenAccept(result -> {
                            resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NO_CONTENT)));
                        });
                    } else {
                        TracingHelper.logError(span, "Resource Version mismatch.");
                        resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_PRECON_FAILED)));
                    }
            } else {
                TracingHelper.logError(span, "Tenant not found.");
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
            }
        });
    }

    @Override
    public void remove(final String tenantId, Optional<String> resourceVersion,
            final Span span, final Handler<AsyncResult<Result<Void>>> resultHandler) {

        tenantsCache.getAsync(tenantId).thenAccept(tenant -> {
            if (tenant == null) {
                TracingHelper.logError(span, "Tenant not found.");
                resultHandler.handle(Future.succeededFuture(Result.from(HTTP_NOT_FOUND)));

            } else if (versionCheck(tenant, resourceVersion)) {

                tenantsCache.withFlags(Flag.FORCE_RETURN_VALUE).removeAsync(tenantId).thenAccept(result -> {
                    if (result != null) {
                        resultHandler.handle(Future.succeededFuture(Result.from(HTTP_NO_CONTENT)));
                    }
                });
            } else {
                TracingHelper.logError(span, "Resource Version mismatch.");
                resultHandler.handle(Future.succeededFuture(Result.from(HTTP_PRECON_FAILED)));
            }
        });
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

        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(resultHandler);

        tenantsCache.getAsync(tenantId).thenAccept(registryTenantObject -> {
            if (registryTenantObject == null) {
                TracingHelper.logError(span, "Tenant not found.");
                resultHandler.handle(Future.succeededFuture(TenantResult.from(HTTP_NOT_FOUND)));
            } else {
                resultHandler.handle(Future.succeededFuture(
                        TenantResult.from(HTTP_OK, new JsonObject(registryTenantObject.getTenantObject()))));
            }
        });
    }

    @Override
    public void read(final String tenantId, final Span span,
            final Handler<AsyncResult<OperationResult<Tenant>>> resultHandler) {

        tenantsCache.getAsync(tenantId).thenAccept(registryTenantObject -> {

            if (registryTenantObject == null) {
                TracingHelper.logError(span, "Tenant not found.");
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
            } else {
                resultHandler.handle(Future.succeededFuture(OperationResult.ok(
                        HTTP_OK,
                        convertTenantObject(registryTenantObject.getTenantObject()),
                        Optional.ofNullable(Optional.empty()),
                        Optional.ofNullable(registryTenantObject.getVersion()))
                ));
            }
        });
    }

    static Tenant convertTenantObject(final String tenantJson){

        if (tenantJson == null) {
            return null;
        }

        final TenantObject tenantObject = new JsonObject(tenantJson).mapTo(TenantObject.class);

        final Tenant tenant = new Tenant();

        tenant.setEnabled(tenantObject.getProperty(TenantConstants.FIELD_ENABLED, Boolean.class));

        Optional.ofNullable(tenantObject.getProperty("ext", JsonObject.class))
                .map(JsonObject::getMap)
                .ifPresent(tenant::setExtensions);

        Optional.ofNullable(tenantObject.getAdapterConfigurations())
                .map(JsonArray::getList)
                .ifPresent(tenant::setAdapters);

        Optional.ofNullable(tenantObject.getResourceLimits())
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .map(json -> json.mapTo(ResourceLimits.class))
                .ifPresent(tenant::setLimits);

        Optional.ofNullable(tenantObject.getProperty(TenantConstants.FIELD_PAYLOAD_TRUSTED_CA, JsonObject.class))
                .map(json -> json.mapTo(TrustedCertificateAuthority.class))
                .ifPresent(tenant::setTrustedCertificateAuthority);

        return tenant;
    }

    @Override
    public void get(final X500Principal subjectDn, final Span span, final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {

        if (subjectDn == null) {
            TracingHelper.logError(span, "invalid certificate provided.");
            resultHandler.handle(Future.succeededFuture(TenantResult.from(HTTP_BAD_REQUEST)));
        } else {

            final RegistryTenantObject searchResult = searchByCert(subjectDn.getName());

            if (searchResult == null) {
                TracingHelper.logError(span, "Tenant not found.");
                resultHandler.handle(Future.succeededFuture(TenantResult.from(HTTP_NOT_FOUND)));
            } else {
                resultHandler.handle(Future.succeededFuture(TenantResult.from(HTTP_OK,
                        new JsonObject(searchResult.getTenantObject()))));
            }
        }
    }

    // infinispan async querying coming soon™
    private RegistryTenantObject searchByCert(final String subjectDnName) {

        final QueryFactory queryFactory = Search.getQueryFactory(tenantsCache);
        final Query query = queryFactory
                .from(RegistryTenantObject.class)
                .having("trustedCa")
                .contains(subjectDnName).build();

        final List<RegistryTenantObject> matches = query.list();

        // TODO make a difference between not found and conflict?
        if (matches.size() != 1){
            return null;
        } else {
            return matches.get(0);
        }
    }

    private static String generateTenantId() {
        return UUID.randomUUID().toString();
    }

    private boolean versionCheck(final RegistryTenantObject tenant, final Optional<String> version) {

        return tenant.getVersion().equals(version.orElse(tenant.getVersion()));
    }
}