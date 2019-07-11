/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.credentials;

import io.enmasse.iot.registry.infinispan.CacheProvider;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import static io.enmasse.iot.registry.infinispan.util.MoreFutures.completeHandler;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.hono.auth.HonoPasswordEncoder;
import org.eclipse.hono.service.credentials.CompleteBaseCredentialsService;
import org.eclipse.hono.util.CredentialsConstants;
import org.eclipse.hono.util.CredentialsResult;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class CacheCredentialService extends CompleteBaseCredentialsService<Void> {

    private final RemoteCache<CredentialsKey, String> cache;

    /**
     * Creates a new service instance for a password encoder.
     *
     * @param pwdEncoder The encoder to use for hashing clear text passwords.
     * @throws NullPointerException if encoder is {@code null}.
     */
    @Autowired
    protected CacheCredentialService(final CacheProvider provider, final HonoPasswordEncoder pwdEncoder) {
        this(provider
                .<CredentialsKey, String>getOrCreateCache("credentials"),
                pwdEncoder);
    }

    CacheCredentialService(RemoteCache<CredentialsKey, String> cache, final HonoPasswordEncoder pwdEncoder) {
        super(pwdEncoder);
        this.cache = cache;
    }

    @Override
    public void setConfig(final Void configuration) {}

    @Override
    public void getAll(String tenantId, String deviceId, Span span, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        handleUnimplementedOperation(resultHandler);
    }

    @Override
    public void removeAll(String tenantId, String deviceId, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        /*
         * This implementation is far from optimal, but it will be gone
         * once we switch to the new API of Hono.
         */
        final Iterator<Map.Entry<CredentialsKey, String>> i = cache.entrySet().iterator();
        while (i.hasNext()) {
            var entry = i.next();
            if (isForDevice(tenantId, deviceId, entry.getKey(), entry.getValue())) {
                i.remove();
            }
        }

        resultHandler.handle(Future.succeededFuture(CredentialsResult.from(HTTP_NO_CONTENT)));
    }

    /**
     * Test if a credentials entry is of the requested tenant/device combination.
     *
     * @param tenantId The requested tenant id.
     * @param deviceId The requested device id.
     * @param credentialsKey The credentials key.
     * @param json The JSON payload.
     * @return {@code true} if the entry matches, {@code false} otherwise.
     */
    private static boolean isForDevice(String tenantId, String deviceId, CredentialsKey credentialsKey, String json) {
        if (!credentialsKey.getTenantId().equals(tenantId)) {
            return false;
        }

        if (json == null || json.isBlank()) {
            return false;
        }

        final JsonObject credentials = new JsonObject(json);
        var currentDeviceId = credentials.getString(CredentialsConstants.FIELD_PAYLOAD_DEVICE_ID);
        if (currentDeviceId == null) {
            return false;
        }

        return currentDeviceId.equals(deviceId);
    }

    @Override
    public void get(String tenantId, String type, String authId, Span span, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        get(tenantId, type, authId, null, span, resultHandler);
    }

    @Override
    public void get(String tenantId, String type, String authId, JsonObject clientContext, Span span, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {

        var key = new CredentialsKey(tenantId, authId, type);

        var future = cache
                .getAsync(key)
                .thenApplyAsync(r -> {
                    if (r != null) {
                        return CredentialsResult.<JsonObject>from(HTTP_OK, new JsonObject(r));
                    } else {
                        return CredentialsResult.<JsonObject>from(HTTP_NOT_FOUND);
                    }
                });

        completeHandler(future, resultHandler);

    }

    @Override
    public void add(String tenantId, JsonObject otherKeys, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {

        final String authId = otherKeys.getString(CredentialsConstants.FIELD_AUTH_ID);
        final String type = otherKeys.getString(CredentialsConstants.FIELD_TYPE);

        var key = new CredentialsKey(tenantId, authId, type);

        add(key, otherKeys, resultHandler);

    }

    private void add(CredentialsKey key, JsonObject payload, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {

        var future = cache
                .withFlags(Flag.FORCE_RETURN_VALUE)
                .putIfAbsentAsync(key, payload.encode())
                .thenApplyAsync(r -> {
                    if (r == null) {
                        return CredentialsResult.<JsonObject>from(HTTP_CREATED);
                    } else {
                        return CredentialsResult.<JsonObject>from(HTTP_CONFLICT);
                    }
                });

        completeHandler(future, resultHandler);

    }

    @Override
    public void update(String tenantId, JsonObject otherKeys, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {

        final String authId = otherKeys.getString(CredentialsConstants.FIELD_AUTH_ID);
        final String type = otherKeys.getString(CredentialsConstants.FIELD_TYPE);

        var key = new CredentialsKey(tenantId, authId, type);

        update(key, otherKeys, resultHandler);

    }

    private void update(CredentialsKey key, JsonObject payload, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {

        var future = cache
                .withFlags(Flag.FORCE_RETURN_VALUE)
                .replaceAsync(key, payload.encode())
                .thenApplyAsync(r -> {
                    if (r == null) {
                        return CredentialsResult.<JsonObject>from(HTTP_NOT_FOUND);
                    } else {
                        return CredentialsResult.<JsonObject>from(HTTP_NO_CONTENT);
                    }
                });

        completeHandler(future, resultHandler);

    }

    @Override
    public void remove(String tenantId, String type, String authId, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {

        var key = new CredentialsKey(tenantId, authId, type);

        var future = cache
                .withFlags(Flag.FORCE_RETURN_VALUE)
                .removeAsync(key)
                .thenApplyAsync(r -> {
                    if (r == null) {
                        return CredentialsResult.<JsonObject>from(HTTP_NOT_FOUND);
                    } else {
                        return CredentialsResult.<JsonObject>from(HTTP_NO_CONTENT);
                    }
                });

        completeHandler(future, resultHandler);

    }

}
