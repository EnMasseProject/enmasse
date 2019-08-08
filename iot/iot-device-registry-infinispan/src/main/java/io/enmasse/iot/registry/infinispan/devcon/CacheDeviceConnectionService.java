/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.devcon;

import static io.enmasse.iot.registry.infinispan.util.MoreFutures.completeHandler;
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
import io.enmasse.iot.registry.infinispan.cache.DeviceConnectionCacheProvider;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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
public class CacheDeviceConnectionService implements DeviceConnectionService {

    private final RemoteCache<DeviceConnectionKey, String> cache;

    @Autowired
    protected CacheDeviceConnectionService(final DeviceConnectionCacheProvider provider) {
        this(provider.getDeviceStateCache());
    }

    CacheDeviceConnectionService(final RemoteCache<DeviceConnectionKey, String> cache) {
        this.cache = cache;
    }

    @Override
    public void getLastKnownGatewayForDevice(String tenantId, String deviceId, Span span, Handler<AsyncResult<DeviceConnectionResult>> resultHandler) {

        final var key = new DeviceConnectionKey(tenantId, deviceId);

        final var future = this.cache

                .getAsync(key)
                .thenApply(result -> {

                    if (result == null) {
                        return from(HTTP_NOT_FOUND);
                    }

                    return from(HTTP_OK, result);

                });

        completeHandler(() -> future, resultHandler);
    }

    @Override
    public void setLastKnownGatewayForDevice(String tenantId, String deviceId, String gatewayId, Span span, Handler<AsyncResult<DeviceConnectionResult>> resultHandler) {

        final var key = new DeviceConnectionKey(tenantId, deviceId);

        /*
         * Currently we are simply setting/replacing the value. This only works as long as the
         * gatewayId is the only value in this object
         */

        final var value = new JsonObject();
        value.put(DeviceConnectionConstants.FIELD_GATEWAY_ID, gatewayId);

        final var future = this.cache
                .putAsync(key, value.encode())
                .thenApply(result -> {
                    return from(HTTP_NO_CONTENT);
                });

        completeHandler(() -> future, resultHandler);
    }


}
