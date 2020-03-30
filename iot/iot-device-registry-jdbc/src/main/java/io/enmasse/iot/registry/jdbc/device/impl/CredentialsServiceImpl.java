/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.device.impl;

import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_REGISTRY_ADAPTER;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.eclipse.hono.util.CacheDirective.noCacheDirective;

import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.hono.deviceregistry.service.credentials.AbstractCredentialsService;
import org.eclipse.hono.deviceregistry.service.tenant.TenantKey;
import org.eclipse.hono.util.Constants;
import org.eclipse.hono.util.CredentialsConstants;
import org.eclipse.hono.util.CredentialsResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.enmasse.iot.jdbc.store.device.AbstractDeviceAdapterStore;
import io.opentracing.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Component
@Profile(PROFILE_REGISTRY_ADAPTER)
public class CredentialsServiceImpl extends AbstractCredentialsService {

    private final AbstractDeviceAdapterStore store;

    @Autowired
    public CredentialsServiceImpl(final AbstractDeviceAdapterStore store) {
        this.store = store;
    }

    @Override
    protected Future<CredentialsResult<JsonObject>> processGet(TenantKey tenant, org.eclipse.hono.deviceregistry.service.credentials.CredentialKey key, JsonObject clientContext, Span span) {
        return this.store.findCredentials(key, span.context())
                .map(r -> {

                    if (r.isEmpty()) {
                        return CredentialsResult.<JsonObject>from(HttpURLConnection.HTTP_NOT_FOUND);
                    }

                    var result = r.get();

                    var secrets = result.getCredentials()
                            .stream()
                            .map(JsonObject::mapFrom)
                            .filter(filter(key.getType(), key.getAuthId()))
                            .flatMap(c -> c.getJsonArray(CredentialsConstants.FIELD_SECRETS)
                                    .stream()
                                    .filter(JsonObject.class::isInstance)
                                    .map(JsonObject.class::cast))
                            .filter(CredentialsServiceImpl::filterSecrets)
                            .collect(Collectors.toList());


                    var payload = new JsonObject()
                            .put(Constants.JSON_FIELD_DEVICE_ID, result.getDeviceId())
                            .put(CredentialsConstants.FIELD_TYPE, key.getType())
                            .put(CredentialsConstants.FIELD_AUTH_ID, key.getAuthId())
                            .put(CredentialsConstants.FIELD_SECRETS, new JsonArray(secrets));

                    return CredentialsResult.from(HTTP_OK, payload, noCacheDirective());

                });
    }

    public static boolean filterSecrets(final JsonObject secret) {
        if (secret == null) {
            return false;
        }

        if (!secret.getBoolean(CredentialsConstants.FIELD_ENABLED, true)) {
            return false;
        }

        if (!validTime(secret, CredentialsConstants.FIELD_SECRETS_NOT_BEFORE, Instant::isAfter)) {
            return false;
        }
        if (!validTime(secret, CredentialsConstants.FIELD_SECRETS_NOT_AFTER, Instant::isBefore)) {
            return false;
        }

        return true;
    }

    public static boolean validTime(final JsonObject obj, final String fieldName, final BiFunction<Instant, Instant, Boolean> comparator) {

        var str = obj.getString(fieldName);
        if (str == null || str.isBlank()) {
            return true;
        }

        final OffsetDateTime dateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(str, OffsetDateTime::from);

        return comparator.apply(Instant.now(), dateTime.toInstant());

    }

    public static Predicate<JsonObject> filter(final String type, final String authId) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(authId);

        return c -> {

            if (!c.getBoolean(CredentialsConstants.FIELD_ENABLED, true)) {
                return false;
            }

            if (!type.equals(c.getString(CredentialsConstants.FIELD_TYPE))) {
                return true;
            }

            if (!authId.equals(c.getString(CredentialsConstants.FIELD_AUTH_ID))) {
                return true;
            }

            return true;
        };
    }

}
