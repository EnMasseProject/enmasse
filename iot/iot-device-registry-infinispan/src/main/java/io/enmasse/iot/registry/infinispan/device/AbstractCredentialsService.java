/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device;

import static io.enmasse.iot.infinispan.device.CredentialKey.credentialKey;
import static io.enmasse.iot.utils.MoreFutures.completeHandler;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.service.credentials.CredentialsService;
import org.eclipse.hono.util.CredentialsResult;
import org.infinispan.client.hotrod.RemoteCache;
import org.springframework.beans.factory.annotation.Autowired;

import io.enmasse.iot.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.infinispan.device.CredentialKey;
import io.enmasse.iot.infinispan.device.DeviceInformation;
import io.enmasse.iot.infinispan.device.DeviceKey;
import io.enmasse.iot.infinispan.tenant.TenantHandle;
import io.enmasse.iot.registry.infinispan.tenant.TenantInformationService;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public abstract class AbstractCredentialsService implements CredentialsService {

    // Adapter cache :
    // <( tenantId + authId + type), (adapter credentials)>
    protected final RemoteCache<CredentialKey, String> adapterCache;

    // Management cache
    // <(TenantId+DeviceId), (Device information + version + credentials)>
    protected final RemoteCache<DeviceKey, DeviceInformation> managementCache;

    @Autowired
    protected TenantInformationService tenantInformationService;

    @Autowired
    public AbstractCredentialsService(final DeviceManagementCacheProvider cacheProvider) {
        this.adapterCache = cacheProvider.getOrCreateAdapterCredentialsCache();
        this.managementCache = cacheProvider.getOrCreateDeviceManagementCache();
    }

    public void setTenantInformationService(TenantInformationService tenantInformationService) {
        this.tenantInformationService = tenantInformationService;
    }

    @Override
    public void get(final String tenantId, final String type, final String authId, final Span span, final Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        completeHandler(() -> processGet(tenantId, type, authId, span), resultHandler);
    }

    @Override
    public void get(String tenantId, String type, String authId, JsonObject clientContext, Span span, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        get(tenantId, type, authId, span, resultHandler);
    }

    protected CompletableFuture<CredentialsResult<JsonObject>> processGet(final String tenantId, final String type, final String authId, final Span span) {

        return this.tenantInformationService
                .tenantExists(tenantId, HTTP_NOT_FOUND, span )
                .thenCompose(tenantHandle -> processGet(tenantHandle, credentialKey(tenantHandle, authId, type), span));

    }

    protected abstract CompletableFuture<CredentialsResult<JsonObject>> processGet(TenantHandle tenant, CredentialKey key, Span span);

}
