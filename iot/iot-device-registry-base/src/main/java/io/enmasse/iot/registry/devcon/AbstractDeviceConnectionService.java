/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.devcon;

import static io.enmasse.iot.utils.MoreFutures.finishHandler;

import org.eclipse.hono.service.deviceconnection.DeviceConnectionService;
import org.eclipse.hono.util.DeviceConnectionResult;

import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public abstract class AbstractDeviceConnectionService implements DeviceConnectionService {

    protected abstract Future<DeviceConnectionResult> processSetLastKnownGatewayForDevice(final DeviceConnectionKey key, final String gatewayId, final Span span);

    protected abstract Future<DeviceConnectionResult> processGetLastKnownGatewayForDevice(final DeviceConnectionKey key, final Span span);

    @Override
    public void getLastKnownGatewayForDevice(final String tenantId, final String deviceId, final Span span, final Handler<AsyncResult<DeviceConnectionResult>> resultHandler) {
        final var key = DeviceConnectionKey.deviceConnectionKey(tenantId, deviceId);
        finishHandler(() -> processGetLastKnownGatewayForDevice(key, span), resultHandler);
    }

    @Override
    public void setLastKnownGatewayForDevice(final String tenantId, final String deviceId, final String gatewayId, final Span span,
            final Handler<AsyncResult<DeviceConnectionResult>> resultHandler) {
        final var key = DeviceConnectionKey.deviceConnectionKey(tenantId, deviceId);
        finishHandler(() -> processSetLastKnownGatewayForDevice(key, gatewayId, span), resultHandler);
    }

}
