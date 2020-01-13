/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.devcon;

import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.service.deviceconnection.DeviceConnectionService;
import org.eclipse.hono.util.DeviceConnectionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.registry.devcon.AbstractDeviceConnectionService;
import io.enmasse.iot.registry.devcon.DeviceConnectionKey;
import io.opentracing.Span;

/**
 * A {@link DeviceConnectionService} that use an Infinispan as a backend service.
 * Infinispan is an open source project providing a distributed in-memory key/value data store
 *
 * <p>
 *
 * @see <a href="https://infinspan.org">https://infinspan.org</a>
 *
 */
@Component
public class DeviceConnectionServiceImpl extends AbstractDeviceConnectionService {

    @Autowired
    protected DeviceConnectionServiceImpl() {
    }

    @Override
    protected CompletableFuture<DeviceConnectionResult> processGetLastKnownGatewayForDevice(DeviceConnectionKey key, Span span) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected CompletableFuture<DeviceConnectionResult> processSetLastKnownGatewayForDevice(DeviceConnectionKey key, String gatewayId, Span span) {
        // TODO Auto-generated method stub
        return null;
    }

}
