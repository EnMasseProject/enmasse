/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device;

import static io.enmasse.iot.registry.infinispan.util.MoreFutures.completeHandler;

import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.util.RegistrationResult;
import org.infinispan.client.hotrod.RemoteCache;
import org.springframework.beans.factory.annotation.Autowired;

import io.enmasse.iot.registry.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.registry.infinispan.device.data.DeviceInformation;
import io.enmasse.iot.registry.infinispan.device.data.DeviceKey;
import io.opentracing.Span;
import io.opentracing.noop.NoopSpan;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public abstract class AbstractRegistrationService extends org.eclipse.hono.service.registration.AbstractRegistrationService {

    // Management cache
    // <(TenantId+DeviceId), (Device information + version + credentials)>
    protected RemoteCache<DeviceKey, DeviceInformation> managementCache;

    @Autowired
    public AbstractRegistrationService(final DeviceManagementCacheProvider provider) {
        this.managementCache = provider.getDeviceManagementCache();
    }

    protected abstract CompletableFuture<RegistrationResult> processGetDevice(String tenantId, String deviceId, Span span);

    @Override
    protected void getDevice(final String tenantId, final String deviceId, final Handler<AsyncResult<RegistrationResult>> resultHandler) {
        completeHandler(() -> processGetDevice(tenantId, deviceId, NoopSpan.INSTANCE), resultHandler);
    }

}
