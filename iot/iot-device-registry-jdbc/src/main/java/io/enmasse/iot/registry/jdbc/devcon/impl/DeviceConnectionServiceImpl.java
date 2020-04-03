/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.devcon.impl;

import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_DEVICE_CONNECTION;

import java.net.HttpURLConnection;

import org.eclipse.hono.service.deviceconnection.DeviceConnectionService;
import org.eclipse.hono.util.DeviceConnectionConstants;
import org.eclipse.hono.util.DeviceConnectionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.enmasse.iot.jdbc.store.devcon.Store;
import io.enmasse.iot.registry.devcon.AbstractDeviceConnectionService;
import io.enmasse.iot.registry.devcon.DeviceConnectionKey;
import io.opentracing.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * A {@link DeviceConnectionService} that use an JDBC as a backend service.
 */
@Component
@Profile(PROFILE_DEVICE_CONNECTION)
public class DeviceConnectionServiceImpl extends AbstractDeviceConnectionService {

    private Store store;

    @Autowired
    protected DeviceConnectionServiceImpl(final Store store) {
        this.store = store;
    }

    private static JsonObject toGatewayResult(final String gatewayId) {
        return new JsonObject()
                .put(DeviceConnectionConstants.FIELD_GATEWAY_ID, gatewayId);
    }

    @Override
    protected Future<DeviceConnectionResult> processGetLastKnownGatewayForDevice(final DeviceConnectionKey key, final Span span) {

        return this.store
                .readDeviceState(key, span.context())
                .map(result -> result.flatMap(state -> state.getLastKnownGateway()))
                .map(gw -> gw
                        .map(gatewayId -> DeviceConnectionResult.from(HttpURLConnection.HTTP_OK, toGatewayResult(gatewayId)))
                        .orElseGet(() -> DeviceConnectionResult.from(HttpURLConnection.HTTP_NOT_FOUND)));

    }

    @Override
    protected Future<DeviceConnectionResult> processSetLastKnownGatewayForDevice(final DeviceConnectionKey key, final String gatewayId, final Span span) {

        return this.store
                .setLastKnownGateway(key, gatewayId, span.context())
                .map(r -> DeviceConnectionResult.from(HttpURLConnection.HTTP_NO_CONTENT));

    }

}
