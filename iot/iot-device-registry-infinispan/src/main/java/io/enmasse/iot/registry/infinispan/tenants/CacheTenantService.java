/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.tenants;

import io.opentracing.noop.NoopSpan;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import io.enmasse.iot.registry.infinispan.CacheProvider;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import java.util.UUID;
import org.eclipse.hono.service.management.Id;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.tenant.Tenant;
import org.eclipse.hono.service.management.tenant.TenantManagementService;
import org.eclipse.hono.service.tenant.TenantService;
import org.eclipse.hono.util.TenantObject;
import org.eclipse.hono.util.TenantResult;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import javax.security.auth.x500.X500Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
@Qualifier("serviceImpl")
public class CacheTenantService implements TenantService, TenantManagementService {

    // <(tenantId), (credential + deviceId + sync-flag + registration data version)>
    private final RemoteCache<String, RegistryTenantObject> tenantsCache;
    // <Cert SubjectDn, tenantId>
    private final RemoteCache<String, String> certificateMappingCache;

    @Autowired
    protected CacheTenantService(final CacheProvider provider){
        this.tenantsCache = provider.getOrCreateCache("tenants");
        this.certificateMappingCache = provider.getOrCreateCache("tenantsCertsMapCache");
    }

    @Override
    public void add(Optional<String> optionalTenantId, JsonObject tenantObj, Span span,
            Handler<AsyncResult<OperationResult<Id>>> resultHandler) {

        final String tenantId = optionalTenantId.orElseGet(this::generateTenantId);

        final RegistryTenantObject tenantObject = new RegistryTenantObject(tenantObj);
        tenantObject.setTenantId(tenantId);

        //verify if a duplicate cert exists
        final String certName = tenantObject.getCertName();
        if (certName != null) {
            final RegistryTenantObject tenant = searchByCert(certName);
            if (tenant != null) {
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_CONFLICT)));
                return;
            } else {
                tenantsCache.containsKeyAsync(tenantId).thenAccept(conflict -> {
                    if (conflict) {
                        resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_CONFLICT)));
                        return;
                    } else {
                        certificateMappingCache.putAsync(certName, tenantId);
                        }
                });
            }
        }

        tenantsCache.withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsentAsync(
                tenantId, tenantObject).thenAccept(result -> {
            if (result == null) {
                resultHandler.handle(Future.succeededFuture(
                        OperationResult.ok(
                                HTTP_CREATED,
                                Id.of(tenantId),
                                Optional.empty(),
                                Optional.of(tenantObject.getVersion()))));
            } else {
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_CONFLICT)));
            }
        });
    }

    @Override
    // todo : add resource version check
    public void update(String tenantId, JsonObject tenantObj, Optional<String> resourceVersion, Span span,
            Handler<AsyncResult<OperationResult<Void>>> resultHandler) {

        final TenantObject tenantDetails = Optional.ofNullable(tenantObj)
                .map(json -> json.mapTo(TenantObject.class)).orElse(null);

        //verify if a duplicate cert exists
        if (tenantDetails.getTrustedCaSubjectDn() != null) {
            final String certName = tenantDetails.getTrustedCaSubjectDn().getName();
            final RegistryTenantObject tenant = searchByCert(certName);
            if (tenant != null) {
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_CONFLICT)));
                return;
            } else {
                tenantsCache.getAsync(tenantId).thenAccept(result -> {
                    if (result != null) {
                        if (result.isVersionMatch(resourceVersion)){
                            certificateMappingCache.putAsync(certName, tenantId);
                        } else {
                            resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_PRECON_FAILED)));
                        }
                    } else {
                        resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
                        return;
                    }
                });
            }
        }

        tenantsCache.containsKeyAsync(tenantId).thenAccept(containsKey -> {
            if (containsKey) {
                final RegistryTenantObject value = new RegistryTenantObject(tenantObj);
                tenantsCache.replaceAsync(tenantId, value).thenAccept(result -> {
                    resultHandler.handle(Future.succeededFuture(
                            OperationResult.ok(
                                HTTP_NO_CONTENT,
                                    null,
                                    Optional.empty(),
                                    Optional.of(value.getVersion()))));
                });
            } else {
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
            }
        });
    }

    @Override
    //fixme nested async call
    public void remove(String tenantId, Optional<String> resourceVersion, Span span,
            Handler<AsyncResult<Result<Void>>> resultHandler) {

        tenantsCache.getAsync(tenantId).thenAccept(result -> {

                if (result != null) {
                    if ( result.isVersionMatch(resourceVersion)) {
                        tenantsCache.withFlags(Flag.FORCE_RETURN_VALUE).removeAsync(tenantId).thenAccept(delResult -> {

                            final String cert = delResult.getCertName();
                            certificateMappingCache.removeAsync(cert);
                            resultHandler.handle(Future.succeededFuture(
                                    Result.from(HTTP_NO_CONTENT)));
                                });
                    } else {
                        resultHandler.handle(Future.succeededFuture(Result.from(HTTP_PRECON_FAILED)));
                    }
                } else {
                    resultHandler.handle(Future.succeededFuture(Result.from(HTTP_NOT_FOUND)));
                }
        });
    }

    @Override public void read(String tenantId, Span span, Handler<AsyncResult<OperationResult<Tenant>>> resultHandler) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(resultHandler);

        tenantsCache.getAsync(tenantId).thenAccept(registryTenantObject -> {
            if (registryTenantObject == null) {
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
            } else {
                resultHandler.handle(Future.succeededFuture(
                        OperationResult.ok(
                                HTTP_OK,
                                new JsonObject(registryTenantObject.getTenantObject()).mapTo(Tenant.class),
                                Optional.empty(),
                                Optional.of(registryTenantObject.getVersion()))));
            }
        });
    }

    @Override public void get(String tenantId, Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {
        get(tenantId, NoopSpan.INSTANCE, resultHandler);
    }

    @Override
    public void get(final String tenantId, final Span span, final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {

        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(resultHandler);

        tenantsCache.getAsync(tenantId).thenAccept(registryTenantObject -> {
            if (registryTenantObject == null) {
                resultHandler.handle(Future.succeededFuture(TenantResult.from(HTTP_NOT_FOUND)));
            } else {
                resultHandler.handle(Future.succeededFuture(
                        TenantResult.from(HTTP_OK, new JsonObject(registryTenantObject.getTenantObject()))));
            }
        });
    }

    @Override
    public void get(X500Principal x500Principal, Handler<AsyncResult<TenantResult<JsonObject>>> handler) {
        get(x500Principal, NoopSpan.INSTANCE, handler);
    }

    @Override
    public void get(final X500Principal subjectDn, final Span span, final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {

        if (subjectDn == null) {
            resultHandler.handle(Future.succeededFuture(TenantResult.from(HTTP_BAD_REQUEST)));
        } else {

            final RegistryTenantObject searchResult = searchByCert(subjectDn.getName());

            if (searchResult == null) {
                resultHandler.handle(Future.succeededFuture(TenantResult.from(HTTP_NOT_FOUND)));
            } else {
                resultHandler.handle(Future.succeededFuture(TenantResult.from(HTTP_OK,
                        new JsonObject(searchResult.getTenantObject()))));
            }
        }
    }

    // fixme : async
    private RegistryTenantObject searchByCert(final String subjectDnName){

        final String tenantId = certificateMappingCache.get(subjectDnName);

        if (tenantId != null) {
            return tenantsCache.get(tenantId);
        } else {
            return null;
        }
    }

    /**
     * Generate a random device ID.
     */
    private String generateTenantId(){

        String tempTenantId;
        do {
            tempTenantId = UUID.randomUUID().toString();
        } while (tenantsCache.containsKey(tempTenantId));
        return tempTenantId;
    }
}
