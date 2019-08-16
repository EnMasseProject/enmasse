/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device;

import static io.enmasse.iot.registry.infinispan.util.MoreFutures.completeHandler;
import static io.vertx.core.json.JsonObject.mapFrom;

import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.service.credentials.CredentialsService;
import org.eclipse.hono.util.CredentialsResult;
import org.infinispan.client.hotrod.RemoteCache;
import org.springframework.beans.factory.annotation.Autowired;

import io.enmasse.iot.registry.infinispan.cache.AdapterCredentialsCacheProvider;
import io.enmasse.iot.registry.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.registry.infinispan.device.data.AdapterCredentials;
import io.enmasse.iot.registry.infinispan.device.data.CredentialsKey;
import io.enmasse.iot.registry.infinispan.device.data.DeviceInformation;
import io.enmasse.iot.registry.infinispan.device.data.DeviceKey;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public abstract class AbstractCredentialsService implements CredentialsService {

    // Adapter cache :
    // <( tenantId + authId + type), (credential + deviceId + sync-flag + registration data version)>
    protected RemoteCache<CredentialsKey, AdapterCredentials> adapterCache;

    // Management cache
    // <(TenantId+DeviceId), (Device information + version + credentials)>
    protected RemoteCache<DeviceKey, DeviceInformation> managementCache;

    @Autowired
    public AbstractCredentialsService(final DeviceManagementCacheProvider managementProvider, final AdapterCredentialsCacheProvider adapterProvider) {
        this.adapterCache = adapterProvider.getAdapterCredentialsCache();
        this.managementCache = managementProvider.getDeviceManagementCache();
    }

    protected abstract CompletableFuture<CredentialsResult<AdapterCredentials>> processGet(String tenantId, String type, String authId, Span span);

    @Override
    public void get(final String tenantId, final String type, final String authId, final Span span, final Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        completeHandler(() -> {
            return processGet(tenantId, type, authId, span)
                    .thenApply(r -> {
                        final var payload = r.getPayload();
                        // FIXME: pass along application properties, when eclipse/hono#1447 is merged
                        return CredentialsResult.from(r.getStatus(), payload != null ? mapFrom(payload) : null, r.getCacheDirective());
                    });
        }, resultHandler);
    }

    @Override
    public void get(String tenantId, String type, String authId, JsonObject clientContext, Span span, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        get(tenantId, type, authId, span, resultHandler);
    }

}
