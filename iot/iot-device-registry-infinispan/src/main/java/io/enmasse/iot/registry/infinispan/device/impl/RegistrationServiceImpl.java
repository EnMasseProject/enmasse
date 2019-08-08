/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.impl;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.util.RegistrationResult;
import org.springframework.stereotype.Component;

import io.enmasse.iot.registry.infinispan.cache.DeviceCacheProvider;
import io.enmasse.iot.registry.infinispan.device.AbstractRegistrationService;
import io.enmasse.iot.registry.infinispan.device.data.DeviceKey;
import io.opentracing.Span;
import io.vertx.core.json.JsonObject;

@Component
public class RegistrationServiceImpl extends AbstractRegistrationService {

    public RegistrationServiceImpl(final DeviceCacheProvider provider) {
        super(provider);
    }

    @Override
    protected CompletableFuture<RegistrationResult> processGetDevice(final String tenantId, final String deviceId, final Span span) {

        final DeviceKey key = new DeviceKey(tenantId, deviceId);

        return this.managementCache

                .getAsync(key)
                .thenApply(result -> {

                    if (result == null) {
                        return RegistrationResult.from(HTTP_NOT_FOUND);
                    }

                    return RegistrationResult.from(HTTP_OK, convertTo(result.getRegistrationInformationAsJson()));
                });

    }

    private JsonObject convertTo(final JsonObject deviceInfo) {
        return deviceInfo;
    }

}
