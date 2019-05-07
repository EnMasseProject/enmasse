/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.registry;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.bases.IoTTestBaseWithShared;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.Map;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static io.enmasse.systemtest.apiclients.Predicates.in;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag(sharedIot)
class DeviceRegistryTest extends IoTTestBaseWithShared {

    private Logger log = CustomLogger.getLogger();

    private static final String DUMMY_DEVICE_ID = "1234";
    private Endpoint deviceRegistryEndpoint;
    private Endpoint httpAdapterEndpoint;
    private DeviceRegistryClient client;

    @BeforeEach
    void initClient() {
        if (deviceRegistryEndpoint == null) {
            deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        }
        if (httpAdapterEndpoint == null) {
            httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        }
        if (client == null) {
            client = new DeviceRegistryClient(kubernetes, deviceRegistryEndpoint);
        }
    }

    @Test
    void testRegisterDevice() throws Exception {
        client.registerDevice(tenantId(), DUMMY_DEVICE_ID);

        JsonObject result = client.getDeviceRegistration(tenantId(), DUMMY_DEVICE_ID);
        assertEquals(DUMMY_DEVICE_ID, result.getString("device-id"));
        assertNotNull(result.getJsonObject("data"));
        assertEquals(true, result.getJsonObject("data").getBoolean("enabled"));

        client.deleteDeviceRegistration(tenantId(), DUMMY_DEVICE_ID);
        client.getDeviceRegistration(tenantId(), DUMMY_DEVICE_ID, HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    void testDisableDevice() throws Exception {
        client.registerDevice(tenantId(), DUMMY_DEVICE_ID);

        JsonObject payload = new JsonObject(Map.of("enabled", false));

        client.updateDeviceRegistration(tenantId(), DUMMY_DEVICE_ID, payload);

        JsonObject result = client.getDeviceRegistration(tenantId(), DUMMY_DEVICE_ID);
        assertEquals(DUMMY_DEVICE_ID, result.getString("device-id"));
        assertNotNull(result.getJsonObject("data"));
        assertEquals(false, result.getJsonObject("data").getBoolean("enabled"));

        client.deleteDeviceRegistration(tenantId(), DUMMY_DEVICE_ID);
        client.getDeviceRegistration(tenantId(), DUMMY_DEVICE_ID, HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    void testDeviceCredentials() throws Exception {

        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {

            client.registerDevice(tenantId(), DUMMY_DEVICE_ID);

            String authId = "sensor1234";
            String password = "password1234";
            credentialsClient.addCredentials(tenantId(), DUMMY_DEVICE_ID, authId, password);

            JsonObject result = credentialsClient.getCredentials(tenantId(), DUMMY_DEVICE_ID);
            assertNotNull(result);
            assertEquals(1, result.getInteger("total"));
            JsonArray credentials = result.getJsonArray("credentials");
            assertNotNull(credentials);
            assertEquals(1, credentials.size());
            JsonObject credential = credentials.getJsonObject(0);
            assertNotNull(credential);
            assertEquals(DUMMY_DEVICE_ID, credential.getString("device-id"));
            assertEquals(authId, credential.getString("auth-id"));
            assertEquals(true, credential.getBoolean("enabled"));
            //TODO chech secret[0].pwd-hash matches "password1234" hash, waiting for issue #2569 to be resolved

            checkCredentials(authId, password, false);

            credentialsClient.deleteAllCredentials(tenantId(), DUMMY_DEVICE_ID);
            credentialsClient.getCredentials(tenantId(), DUMMY_DEVICE_ID, HttpURLConnection.HTTP_NOT_FOUND);

            client.deleteDeviceRegistration(tenantId(), DUMMY_DEVICE_ID);
            client.getDeviceRegistration(tenantId(), DUMMY_DEVICE_ID, HttpURLConnection.HTTP_NOT_FOUND);

        }
    }

    @Test
    void testSetExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {
            client.registerDevice(tenantId(), DUMMY_DEVICE_ID);

            String authId = "sensor1234";
            String password = "password1234";
            credentialsClient.addCredentials(tenantId(), DUMMY_DEVICE_ID, authId, password);

            checkCredentials(authId, password, false);

            int expirySeconds = 30;
            Instant notAfter = Instant.now().plusSeconds(expirySeconds);
            String newPassword = "new-password1234";
            credentialsClient.updateCredentials(tenantId(), DUMMY_DEVICE_ID, authId, newPassword, notAfter);

            JsonObject result = credentialsClient.getCredentials(tenantId(), DUMMY_DEVICE_ID);
            assertNotNull(result);
            assertEquals(1, result.getInteger("total"));
            JsonArray credentials = result.getJsonArray("credentials");
            assertNotNull(credentials);
            assertEquals(1, credentials.size());
            JsonObject credential = credentials.getJsonObject(0);
            assertNotNull(credential);
            assertEquals(DUMMY_DEVICE_ID, credential.getString("device-id"));
            assertEquals(authId, credential.getString("auth-id"));
            assertEquals(true, credential.getBoolean("enabled"));
            JsonArray secrets = credential.getJsonArray("secrets");
            assertNotNull(secrets);
            assertEquals(1, secrets.size());
            JsonObject secret = secrets.getJsonObject(0);
            assertNotNull(secret);
            Instant actualNotAfter = secret.getInstant("not-after");
            assertEquals(notAfter, actualNotAfter);
            //TODO chech secret[0].pwd-hash matches "password1234" hash, waiting for issue #2569 to be resolved

            checkCredentials(authId, newPassword, false);
            log.info("Waiting " + expirySeconds + " seconds for credentials to expire");
            Thread.sleep((expirySeconds + 1) * 1000);
            checkCredentials(authId, newPassword, true);

            credentialsClient.deleteAllCredentials(tenantId(), DUMMY_DEVICE_ID);
            credentialsClient.getCredentials(tenantId(), DUMMY_DEVICE_ID, HttpURLConnection.HTTP_NOT_FOUND);

            client.deleteDeviceRegistration(tenantId(), DUMMY_DEVICE_ID);
            client.getDeviceRegistration(tenantId(), DUMMY_DEVICE_ID, HttpURLConnection.HTTP_NOT_FOUND);
        }
    }

    private void checkCredentials(String authId, String password, boolean authFail) throws Exception {
        try (var httpAdapterClient = new HttpAdapterClient(kubernetes, httpAdapterEndpoint, authId, tenantId(), password)) {
            JsonObject payload = new JsonObject(Map.of("data", "dummy"));

            var expectedResponse = authFail ? in(HTTP_UNAUTHORIZED): in(HTTP_UNAVAILABLE, HTTP_ACCEPTED);
            log.info("Sending event data expected response: {}", expectedResponse);
            httpAdapterClient.sendEvent(payload, expectedResponse );

            log.info("Sending telemetry data expected response: {}", expectedResponse);
            httpAdapterClient.sendTelemetry(payload, expectedResponse);
        }
    }

}
