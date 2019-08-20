/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device;

import static io.enmasse.iot.registry.infinispan.util.MoreFutures.completeHandler;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.credentials.CredentialsManagementService;
import org.infinispan.client.hotrod.RemoteCache;
import org.springframework.beans.factory.annotation.Autowired;

import io.enmasse.iot.registry.infinispan.cache.AdapterCredentialsCacheProvider;
import io.enmasse.iot.registry.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.registry.infinispan.device.data.CredentialKey;
import io.enmasse.iot.registry.infinispan.device.data.DeviceInformation;
import io.enmasse.iot.registry.infinispan.device.data.DeviceKey;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public abstract class AbstractCredentialsManagementService implements CredentialsManagementService {

    // Adapter cache :
    // <(tenantId + authId + type), (credential + deviceId)>
    protected final RemoteCache<CredentialKey, String> adapterCache;

    // Management cache
    // <(tenantId + deviceId), (device information + version + credentials)>
    protected final RemoteCache<DeviceKey, DeviceInformation> managementCache;

    @Autowired
    public AbstractCredentialsManagementService(final DeviceManagementCacheProvider managementProvider, final AdapterCredentialsCacheProvider adapterProvider) {
        this.adapterCache = adapterProvider.getAdapterCredentialsCache();
        this.managementCache = managementProvider.getDeviceManagementCache();
    }

    protected abstract CompletableFuture<OperationResult<Void>> processSet(String tenantId, String deviceId, Optional<String> resourceVersion, List<CommonCredential> credentials,
            Span span);

    protected abstract CompletableFuture<OperationResult<List<CommonCredential>>> processGet(String tenantId, String deviceId, Span span);

    @Override
    public void set(final String tenantId, final String deviceId, final Optional<String> resourceVersion, final List<CommonCredential> credentials, final Span span,
            final Handler<AsyncResult<OperationResult<Void>>> resultHandler) {
        completeHandler(() -> processSet(tenantId, deviceId, resourceVersion, credentials, span), resultHandler);
    }

    @Override
    public void get(String tenantId, String deviceId, Span span, Handler<AsyncResult<OperationResult<List<CommonCredential>>>> resultHandler) {
        completeHandler(() -> processGet(tenantId, deviceId, span), resultHandler);
    }

}
