/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.util;

import static io.vertx.core.json.Json.decodeValue;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.credentials.GenericCredential;
import org.eclipse.hono.service.management.credentials.PasswordCredential;
import org.eclipse.hono.service.management.credentials.PasswordSecret;
import org.eclipse.hono.util.Constants;
import org.eclipse.hono.util.CredentialsConstants;

import io.enmasse.iot.registry.infinispan.device.data.DeviceCredential;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public final class Credentials {

    private Credentials() {}

    public static Stream<CommonCredential> streamCredentials(final String json) {
        if (json == null) {
            return Stream.empty();
        }
        var val = decodeValue(json, CommonCredential[].class);
        if (val == null) {
            return null;
        }
        return stream(val);
    }

    public static List<CommonCredential> listCredentials(final String json) {
        if (json == null) {
            return emptyList();
        }
        var val = decodeValue(json, CommonCredential[].class);
        if (val == null) {
            return null;
        }
        return asList(val);
    }

    public static String getType(final CommonCredential credential) {
        if (credential == null) {
            return null;
        }

        if (credential instanceof GenericCredential) {
            return ((GenericCredential) credential).getType();
        } else {
            return JsonObject.mapFrom(credential).getString("type");
        }
    }

    public static String encodeCredentials(final List<CommonCredential> credentials) {
        if (credentials == null) {
            return null;
        }
        return Json.encode(credentials.toArray(CommonCredential[]::new));
    }

    public static List<DeviceCredential> toInternal(final List<CommonCredential> credentials) {
        if (credentials == null) {
            return null;
        }

        // FIXME: very ugly

        return new JsonArray(encodeCredentials(credentials))
                .stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)

                .map(json -> {
                    final DeviceCredential c = new DeviceCredential();
                    c.setAuthId(json.getString(CredentialsConstants.FIELD_AUTH_ID));
                    c.setType(json.getString(CredentialsConstants.FIELD_TYPE));
                    c.setEnabled(json.getBoolean(CredentialsConstants.FIELD_ENABLED));
                    c.setComment(json.getString("comment"));

                    final JsonArray secrets = json.getJsonArray(CredentialsConstants.FIELD_SECRETS);
                    if (secrets != null) {
                        c.setSecrets(
                                secrets
                                        .stream()
                                        .map(Object::toString)
                                        .collect(Collectors.toList()));
                    }

                    return c;
                })
                .collect(toList());

    }

    public static JsonObject fromInternalToAdapterJson(final String deviceId, final DeviceCredential credential) {
        if (credential == null) {
            return null;
        }

        final JsonObject result = new JsonObject();

        result.put(Constants.JSON_FIELD_DEVICE_ID, deviceId);
        result.put(CredentialsConstants.FIELD_AUTH_ID, credential.getAuthId());
        result.put(CredentialsConstants.FIELD_TYPE, credential.getType());
        result.put(CredentialsConstants.FIELD_SECRETS, mapSecrets(credential));

        return result;
    }

    public static CommonCredential fromInternal(final DeviceCredential credential) {

        // FIXME: very ugly

        final JsonObject json = new JsonObject();

        json.put(CredentialsConstants.FIELD_AUTH_ID, credential.getAuthId());
        json.put(CredentialsConstants.FIELD_TYPE, credential.getType());
        json.put(CredentialsConstants.FIELD_ENABLED, credential.getEnabled());
        json.put("comment", credential.getComment());

        json.put(CredentialsConstants.FIELD_SECRETS, mapSecrets(credential));

        return json.mapTo(CommonCredential.class);
    }

    private static JsonArray mapSecrets(final DeviceCredential credential) {
        final JsonArray secrets = new JsonArray();
        credential
                .getSecrets()
                .stream()
                .map(JsonObject::new)
                .forEach(secrets::add);
        return secrets;
    }

    public static List<CommonCredential> fromInternal(final List<DeviceCredential> credentials) {

        if (credentials == null || credentials.isEmpty()) {
            return Collections.emptyList();
        }

        return credentials
                .stream()
                .map(Credentials::fromInternal)
                // this breaks the gut - > put operation flows.
                // This should be re-enabled when the `id` feature for secret is implemented.
                //.map(Credentials::removePasswordDetails)
                .collect(Collectors.toList());

    }

    /**
     * Strips the hashed-password details from the secret if needed.
     */
    private static CommonCredential removePasswordDetails(final CommonCredential credential) {

        if (JsonObject.mapFrom(credential).getString(CredentialsConstants.FIELD_TYPE).equals(CredentialsConstants.SECRETS_TYPE_HASHED_PASSWORD)) {
            final PasswordCredential passwordCredential = (PasswordCredential) credential;
            passwordCredential.getSecrets().forEach(secret -> {
                secret.setHashFunction(null);
                secret.setPasswordHash(null);
                secret.setPasswordPlain(null);
                secret.setSalt(null);
            });

                return passwordCredential;
        } else {
            return credential;
        }
    }
}
