/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device;

import io.enmasse.iot.registry.infinispan.CacheProvider;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.eclipse.hono.service.registration.CompleteBaseRegistrationService;
import org.eclipse.hono.util.RegistrationResult;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import static io.enmasse.iot.registry.infinispan.util.MoreFutures.completeHandler;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.eclipse.hono.util.RegistrationResult.from;

/**
 * A Registration service that use an Infinispan as a backend service.
 * Infinispan is an open source project providing a distributed in-memory key/value data store
 *
 * <p>
 *
 * @see <a href="https://infinspan.org">https://infinspan.org</a>
 *
 */
@Repository
@Primary
public class CacheRegistrationService extends CompleteBaseRegistrationService<Void> {

    private final RemoteCache<RegistrationKey, String> cache;

    @Autowired
    protected CacheRegistrationService(CacheProvider provider) {
        this(provider
                .<RegistrationKey,String>getOrCreateCache("devices"));
    }

    CacheRegistrationService(RemoteCache<RegistrationKey, String> cache) {
        this.cache = cache;
    }

    @Override
    public void setConfig(final Void configuration) {}

    @Override
    public void addDevice(final String tenantId, final String deviceId, final JsonObject otherKeys, final Handler<AsyncResult<RegistrationResult>> resultHandler) {

        var key = new RegistrationKey(tenantId, deviceId);

        var future = cache
                .withFlags(Flag.FORCE_RETURN_VALUE)
                .putIfAbsentAsync(key, otherKeys.encode())
                .thenApplyAsync(result -> {
                    if (result == null) {
                        return from(HTTP_CREATED);
                    } else {
                        return from(HTTP_CONFLICT);
                    }
                });

        completeHandler(future, resultHandler);

    }

    @Override
    public void updateDevice(final String tenantId, final String deviceId, final JsonObject otherKeys, final Handler<AsyncResult<RegistrationResult>> resultHandler) {

        var key = new RegistrationKey(tenantId, deviceId);

        var future = cache
                .withFlags(Flag.FORCE_RETURN_VALUE)
                .replaceAsync(key, otherKeys.encode())
                .thenApplyAsync(result -> {
                    if (result == null) {
                        return from(HTTP_NOT_FOUND);
                    } else {
                        return from(HTTP_NO_CONTENT);
                    }
                });

        completeHandler(future, resultHandler);
    }

    @Override
    public void removeDevice(final String tenantId, final String deviceId, final Handler<AsyncResult<RegistrationResult>> resultHandler) {

        var key = new RegistrationKey(tenantId, deviceId);

        var future = cache
                .withFlags(Flag.FORCE_RETURN_VALUE)
                .removeAsync(key)
                .thenApplyAsync(result -> {
                    if (result == null) {
                        return from(HTTP_NOT_FOUND);
                    } else {
                        return from(HTTP_NO_CONTENT);
                    }
                });

        completeHandler(future, resultHandler);
    }

    @Override
    public void getDevice(final String tenantId, final String deviceId, final Handler<AsyncResult<RegistrationResult>> resultHandler) {

        var key = new RegistrationKey(tenantId, deviceId);

        var future = cache
                .getAsync(key)
                .thenApplyAsync(result -> {
                    if (result == null) {
                        return from(HTTP_NOT_FOUND);
                    } else {
                        return from(HTTP_OK, getResultPayload(deviceId, new JsonObject(result)));
                    }
                });

        completeHandler(future, resultHandler);
    }
}
