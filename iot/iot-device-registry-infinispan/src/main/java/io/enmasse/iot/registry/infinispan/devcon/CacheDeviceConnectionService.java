/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.devcon;

import static io.enmasse.iot.infinispan.devcon.DeviceConnectionKey.deviceConnectionKey;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.eclipse.hono.util.DeviceConnectionResult.from;

import org.eclipse.hono.service.deviceconnection.DeviceConnectionService;
import org.eclipse.hono.util.DeviceConnectionConstants;
import org.eclipse.hono.util.DeviceConnectionResult;
import org.infinispan.client.hotrod.RemoteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.infinispan.cache.DeviceConnectionCacheProvider;
import io.enmasse.iot.infinispan.devcon.DeviceConnectionKey;
import io.enmasse.iot.registry.devcon.AbstractDeviceConnectionService;
import io.enmasse.iot.utils.MoreFutures;
import io.opentracing.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

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
public class CacheDeviceConnectionService extends AbstractDeviceConnectionService {

    private final RemoteCache<DeviceConnectionKey, String> cache;

    @Autowired
    protected CacheDeviceConnectionService(final DeviceConnectionCacheProvider provider) {
        this(provider.getOrCreateDeviceStateCache());
    }

    CacheDeviceConnectionService(final RemoteCache<DeviceConnectionKey, String> cache) {
        this.cache = cache;
    }

    @Override
    protected Future<DeviceConnectionResult> processGetLastKnownGatewayForDevice(final io.enmasse.iot.registry.devcon.DeviceConnectionKey key, final Span span) {

        var f = this.cache
                .getAsync(deviceConnectionKey(key))
                .thenApply(result -> {

                    if (result == null) {
                        return from(HTTP_NOT_FOUND);
                    }

                    return from(HTTP_OK, result);

                });

        return MoreFutures.map(f);

    }

    @Override
    protected Future<DeviceConnectionResult> processSetLastKnownGatewayForDevice(final io.enmasse.iot.registry.devcon.DeviceConnectionKey key, final String gatewayId,
            final Span span) {

        /*
         * Currently we are simply setting/replacing the value. This only works as long as the
         * gatewayId is the only value in this object
         */

        final var value = new JsonObject();
        value.put(DeviceConnectionConstants.FIELD_GATEWAY_ID, gatewayId);

        var f = this.cache
                .putAsync(deviceConnectionKey(key), value.encode())
                .thenApply(result -> from(HTTP_NO_CONTENT));

        return MoreFutures.map(f);

    }

}
