/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static io.enmasse.systemtest.iot.DeviceManagementApi.getManagementToken;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.eclipse.hono.service.management.credentials.PskCredential;
import org.eclipse.hono.service.management.credentials.PskSecret;
import org.eclipse.hono.service.management.credentials.X509CertificateCredential;
import org.eclipse.hono.service.management.credentials.X509CertificateSecret;

import io.enmasse.systemtest.Endpoint;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;

public class CredentialsRegistryClient extends HonoApiClient {

    private static final String CREDENTIALS_PATH = "v1/credentials";
    private static final Random rnd = new SecureRandom();

    public CredentialsRegistryClient(final Vertx vertx, final Endpoint endpoint) {
        super(vertx, () -> endpoint, getManagementToken());
    }

    @Override
    protected String apiClientName() {
        return "iot-credentials-registry";
    }

    /**
     * Truncate {@link Instant}s to second resolution manually.
     * <br>
     * <b>Note:</b> This method should be dropped once eclipse/hono#1463 is merged.
     * @param instant The credentials to process.
     */
    private static Instant fixInstant(final Instant instant) {
        if (instant == null) {
            return instant;
        }

        return instant.truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Truncate {@link Instant}s to second resolution manually.
     * <br>
     * <b>Note:</b> This method should be dropped once eclipse/hono#1463 is merged.
     * @param credentials The credentials to process.
     */
    private static void fixInstants(final List<? extends CommonCredential> credentials) {
        for (final CommonCredential c : credentials) {
            if (c instanceof PasswordCredential) {
                for (PasswordSecret s : ((PasswordCredential) c).getSecrets()) {
                    s.setNotBefore(fixInstant(s.getNotBefore()));
                    s.setNotAfter(fixInstant(s.getNotAfter()));
                }
            }
        }
    }

    public void setCredentials(final String tenantId, final String deviceId,  final List<CommonCredential> credentials) throws Exception {
        setCredentials(tenantId, deviceId, HttpURLConnection.HTTP_NO_CONTENT, credentials);
    }

    public void setCredentials(final String tenantId, final String deviceId, int expectedStatusCode, final List<CommonCredential> credentials) throws Exception {
        var requestPath = String.format("/%s/%s/%s", CREDENTIALS_PATH, tenantId, deviceId);
        fixInstants(credentials);
        var body = Json.encode(credentials.toArray(CommonCredential[]::new)); // jackson needs an array
        execute(HttpMethod.PUT, requestPath, body, expectedStatusCode, "Error setting credentials to device");
    }

    public List<CommonCredential> getCredentials(final String tenantId, final String deviceId) throws Exception {
        var requestPath = String.format("/%s/%s/%s", CREDENTIALS_PATH, tenantId, deviceId);
        var result = execute(HttpMethod.GET, requestPath, null, HttpURLConnection.HTTP_OK, "Error getting credentials for device");
        return new ArrayList<>(Arrays.asList(Json.decodeValue(result, CommonCredential[].class)));
    }

    public void addCredentials(final String tenantId, final String deviceId, int expectedStatusCode, final List<CommonCredential> newCredentials) throws Exception {
        var credentials = getCredentials(tenantId, deviceId);
        credentials.addAll(newCredentials);
        setCredentials(tenantId, deviceId, expectedStatusCode, credentials);
    }

    public void updateCredentials(String tenantId, String deviceId, Function<List<CommonCredential>, List<CommonCredential>> manipulator) throws Exception {
        var credentials = manipulator.apply(getCredentials(tenantId, deviceId));
        if (credentials != null) {
            setCredentials(tenantId, deviceId, HttpURLConnection.HTTP_NO_CONTENT, credentials);
        }
    }

    public void deleteAllCredentials(final String tenantId, final String deviceId) throws Exception {
        setCredentials(tenantId, deviceId, HttpURLConnection.HTTP_NO_CONTENT, Collections.emptyList());
    }

    static PskSecret createPskSecret(final byte[] key, final Instant notAfter) {

        var secret = new PskSecret();

        secret.setKey(key);
        secret.setNotAfter(notAfter);

        return secret;

    }

    static X509CertificateSecret createX509CertificateSecret(final String authId, final Instant notAfter) {

        var secret = new X509CertificateSecret();

        secret.setNotAfter(notAfter);

        return secret;

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

    static PasswordSecret createPlainPasswordSecret(final String authId, final String password, final Instant notAfter) {

        final PasswordSecret secret = new PasswordSecret();
        secret.setPasswordPlain(password);
        secret.setNotAfter(notAfter);

        return secret;
    }

    public static PasswordCredential createPlainPasswordCredentialsObject(final String authId, final String password, final Instant notAfter) {

        var secret = createPlainPasswordSecret(authId, password, notAfter);

        // create credentials

        var credentials = new PasswordCredential(authId);
        credentials.setSecrets(Collections.singletonList(secret));

        return credentials;

    }

    static PasswordCredential createCredentialsObject(final String authId, final String password, final Instant notAfter) {

        var secret = createPasswordSecret(authId, password, notAfter);

        // create credentials

        var credentials = new PasswordCredential(authId);
        credentials.setSecrets(Collections.singletonList(secret));

        return credentials;

    }

    static PskCredential createPskCredentialsObject(final String authId, final byte[] key, final Instant notAfter) {

        var secret = createPskSecret(key, notAfter);

        // create credentials

        var credentials = new PskCredential(authId);
        credentials.setSecrets(Collections.singletonList(secret));

        return credentials;

    }

    static X509CertificateCredential createX509CertificateCredentialsObject(final String authId, final Instant notAfter) {

        var secret = createX509CertificateSecret(authId, notAfter);

        // create credentials

        var credentials = new X509CertificateCredential(authId);
        credentials.setSecrets(Collections.singletonList(secret));

        return credentials;

    }

    public void addPskCredentials(final String tenantId, final String deviceId, final String authId, final byte[] key, final Instant notAfter, final int expectedStatusCode) throws Exception {
        addCredentials(tenantId, deviceId, expectedStatusCode, Collections.singletonList(createPskCredentialsObject(authId, key, notAfter)));
    }

    public void addPlainPasswordCredentials(final String tenantId, final String deviceId, final String authId, final String password, final Instant notAfter, final int expectedStatusCode) throws Exception {
        addCredentials(tenantId, deviceId, expectedStatusCode, Collections.singletonList(createPlainPasswordCredentialsObject(authId, password, notAfter)));
    }

    public void addCredentials(final String tenantId, final String deviceId, final String authId, final String password, final Instant notAfter, final int expectedStatusCode) throws Exception {
        addCredentials(tenantId, deviceId, expectedStatusCode, Collections.singletonList(createCredentialsObject(authId, password, notAfter)));
    }

    public void addX509Credentials(final String tenantId, final String deviceId, final String authId, final Instant notAfter, final int expectedStatusCode) throws Exception {
        addCredentials(tenantId, deviceId, expectedStatusCode, Collections.singletonList(createX509CertificateCredentialsObject(authId, notAfter)));
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
