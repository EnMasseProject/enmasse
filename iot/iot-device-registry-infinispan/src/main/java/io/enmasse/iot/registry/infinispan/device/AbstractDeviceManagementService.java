/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device;

import org.springframework.beans.factory.annotation.Autowired;

import io.enmasse.iot.registry.infinispan.cache.AdapterCredentialsCacheProvider;
import io.enmasse.iot.registry.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.registry.infinispan.device.data.CredentialsKey;
import io.enmasse.iot.registry.infinispan.device.data.DeviceInformation;
import io.enmasse.iot.registry.infinispan.device.data.DeviceKey;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import static io.enmasse.iot.registry.infinispan.util.MoreFutures.completeHandler;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.hono.service.management.Id;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.device.Device;
import org.eclipse.hono.service.management.device.DeviceManagementService;
import org.infinispan.client.hotrod.RemoteCache;

public abstract class AbstractDeviceManagementService implements DeviceManagementService {

    // Adapter cache :
    // <( tenantId + authId + type), (credential + deviceId + sync-flag + registration data version)>
    protected RemoteCache<CredentialsKey, String> adapterCache;

    // Management cache
    // <(TenantId+DeviceId), (Device information + version + credentials)>
    protected RemoteCache<DeviceKey, DeviceInformation> managementCache;

    @Autowired
    public AbstractDeviceManagementService(final DeviceManagementCacheProvider managementProvider, final AdapterCredentialsCacheProvider adapterProvider) {
        this.adapterCache = adapterProvider.getAdapterCredentialsCache();
        this.managementCache = managementProvider.getDeviceManagementCache();
    }

    protected abstract CompletableFuture<OperationResult<Id>> processCreateDevice(String tenantId, Optional<String> deviceId, Device device, Span span);

    protected abstract CompletableFuture<OperationResult<Device>> processReadDevice(String tenantId, String deviceId, Span span);

    protected abstract CompletableFuture<OperationResult<Id>> processUpdateDevice(String tenantId, String deviceId, Device device, Optional<String> resourceVersion, Span span);

    protected abstract CompletableFuture<Result<Void>> processDeleteDevice(String tenantId, String deviceId, Optional<String> resourceVersion, Span span);

    @Override
    public void createDevice(String tenantId, Optional<String> deviceId, Device device, Span span, Handler<AsyncResult<OperationResult<Id>>> resultHandler) {
        completeHandler(() -> processCreateDevice(tenantId, deviceId, device, span), resultHandler);
    }

    @Override
    public void readDevice(String tenantId, String deviceId, Span span, Handler<AsyncResult<OperationResult<Device>>> resultHandler) {
        completeHandler(() -> processReadDevice(tenantId, deviceId, span), resultHandler);
    }

    @Override
    public void updateDevice(String tenantId, String deviceId, Device device, Optional<String> resourceVersion, Span span,
            Handler<AsyncResult<OperationResult<Id>>> resultHandler) {
        completeHandler(() -> processUpdateDevice(tenantId, deviceId, device, resourceVersion, span), resultHandler);
    }

    @Override
    public void deleteDevice(String tenantId, String deviceId, Optional<String> resourceVersion, Span span, Handler<AsyncResult<Result<Void>>> resultHandler) {
        completeHandler(() -> processDeleteDevice(tenantId, deviceId, resourceVersion, span), resultHandler);
    }

}
