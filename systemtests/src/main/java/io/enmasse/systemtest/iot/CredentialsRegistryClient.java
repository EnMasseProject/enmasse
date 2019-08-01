/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.credentials.PasswordCredential;
import org.eclipse.hono.service.management.credentials.PasswordSecret;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Kubernetes;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;

public class CredentialsRegistryClient extends HonoApiClient {

    private static final String CREDENTIALS_PATH = "v1/credentials";
    private static final Random rnd = new SecureRandom();

    public CredentialsRegistryClient(Kubernetes kubernetes, Endpoint endpoint) {
        super(kubernetes, () -> endpoint);
    }

    @Override
    protected String apiClientName() {
        return "iot-credentials-registry";
    }

    public void setCredentials(final String tenantId, final String deviceId, final List<CommonCredential> credentials) throws Exception {
        var requestPath = String.format("/%s/%s/%s", CREDENTIALS_PATH, tenantId, deviceId);
        var body = Json.encode(credentials.toArray(CommonCredential[]::new)); // jackson needs an array
        execute(HttpMethod.PUT, requestPath, body, HttpURLConnection.HTTP_NO_CONTENT, "Error setting credentials to device");
    }

    public List<CommonCredential> getCredentials(final String tenantId, final String deviceId) throws Exception {
        var requestPath = String.format("/%s/%s/%s", CREDENTIALS_PATH, tenantId, deviceId);
        var result = execute(HttpMethod.GET, requestPath, null, HttpURLConnection.HTTP_OK, "Error getting credentials for device");
        return new ArrayList<>(Arrays.asList(Json.decodeValue(result, CommonCredential[].class)));
    }

    public void addCredentials(final String tenantId, final String deviceId, final List<CommonCredential> newCredentials) throws Exception {
        var credentials = getCredentials(tenantId, deviceId);
        credentials.addAll(newCredentials);
        setCredentials(tenantId, deviceId, credentials);
    }

    public void updateCredentials(String tenantId, String deviceId, Function<List<CommonCredential>, List<CommonCredential>> manipulator) throws Exception {
        var credentials = manipulator.apply(getCredentials(tenantId, deviceId));
        if (credentials != null) {
            setCredentials(tenantId, deviceId, credentials);
        }
    }

    public void deleteAllCredentials(final String tenantId, final String deviceId) throws Exception {
        setCredentials(tenantId, deviceId, Collections.emptyList());
    }

    static PasswordSecret createPasswordSecret(final String authId, final String password, final Instant notAfter) {

        var secret = new PasswordSecret();

        var salt = new byte[16];
        rnd.nextBytes(salt);
        secret.setSalt(Base64.getEncoder().encodeToString(salt));

        try {
            var md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(password.getBytes(StandardCharsets.UTF_8));

            var hashedPassword = Base64.getEncoder().encodeToString(md.digest());
            secret.setHashFunction("sha-256");
            secret.setPasswordHash(hashedPassword);

            secret.setNotAfter(notAfter);

            return secret;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static PasswordCredential createCredentialsObject(final String authId, final String password, final Instant notAfter) {

        var secret = createPasswordSecret(authId, password, notAfter);

        // create credentials

        var credentials = new PasswordCredential();
        credentials.setAuthId(authId);
        credentials.setSecrets(Collections.singletonList(secret));

        return credentials;

    }

    public void addCredentials(final String tenantId, final String deviceId, final String authId, final String password) throws Exception {
        addCredentials(tenantId, deviceId, authId, password, null);
    }

    public void addCredentials(final String tenantId, final String deviceId, final String authId, final String password, final Instant notAfter) throws Exception {
        addCredentials(tenantId, deviceId, Collections.singletonList(createCredentialsObject(authId, password, notAfter)));
    }

    public void updateCredentials(final String tenantId, final String deviceId, final String authId, final String newPassword, final Instant notAfter) throws Exception {
        updateCredentials(tenantId, deviceId, credentials -> {

            for ( CommonCredential c : credentials ) {
                if (!(c instanceof PasswordCredential) ) {
                    continue;
                }
                final var pc = (PasswordCredential)c;
                if (! authId.equals(pc.getAuthId())) {
                    continue;
                }
                pc.setSecrets(Collections.singletonList(createPasswordSecret(authId, newPassword, notAfter)));
                // first match ... stop looking further
                break;
            }

            return credentials;
        });
    }
}
