/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.devcon;

import static io.enmasse.iot.utils.MoreFutures.completeHandler;

import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.service.deviceconnection.DeviceConnectionService;
import org.eclipse.hono.util.DeviceConnectionResult;

import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public abstract class AbstractDeviceConnectionService implements DeviceConnectionService {

    protected abstract CompletableFuture<DeviceConnectionResult> processSetLastKnownGatewayForDevice(final DeviceConnectionKey key, final String gatewayId, final Span span);

    protected abstract CompletableFuture<DeviceConnectionResult> processGetLastKnownGatewayForDevice(final DeviceConnectionKey key, final Span span);

    @Override
    public void getLastKnownGatewayForDevice(final String tenantId, final String deviceId, final Span span, final Handler<AsyncResult<DeviceConnectionResult>> resultHandler) {
        final var key = DeviceConnectionKey.deviceConnectionKey(tenantId, deviceId);
        completeHandler(() -> processGetLastKnownGatewayForDevice(key, span), resultHandler);
    }

    @Override
    public void setLastKnownGatewayForDevice(final String tenantId, final String deviceId, final String gatewayId, final Span span,
            final Handler<AsyncResult<DeviceConnectionResult>> resultHandler) {
        final var key = DeviceConnectionKey.deviceConnectionKey(tenantId, deviceId);
        completeHandler(() -> processSetLastKnownGatewayForDevice(key, gatewayId, span), resultHandler);
    }

}
