/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.registry;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static io.enmasse.systemtest.TestTag.smoke;
import static io.enmasse.systemtest.apiclients.Predicates.in;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.bases.IoTTestBase;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.utils.IoTUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Tag(sharedIot)
@Tag(smoke)
public abstract class DeviceRegistryTestBase extends IoTTestBase {

    private Logger log = CustomLogger.getLogger();

    private String randomDeviceId;
    private IoTConfig iotConfig;
    private IoTProject iotProject;
    private Endpoint deviceRegistryEndpoint;
    private Endpoint httpAdapterEndpoint;
    private DeviceRegistryClient client;

    protected abstract IoTConfig provideIoTConfig() throws Exception;

    protected void removeIoTConfig() throws Exception {
        log.info("Shared IoTConfig will be removed");
        var iotConfigApiClient = kubernetes.getIoTConfigClient();
        if (iotConfigApiClient.withName(iotConfig.getMetadata().getName()).get() != null) {
            IoTUtils.deleteIoTConfigAndWait(kubernetes, iotConfig);
        } else {
            log.info("IoTConfig '{}' doesn't exists!", iotConfig.getMetadata().getName());
        }
    }

    @BeforeEach
    void init() throws Exception {
        if (iotConfig == null) {
            iotConfig = provideIoTConfig();
            createIoTConfig(iotConfig);
        }
        if (iotProject == null) {
            iotProject = IoTUtils.getBasicIoTProjectObject("device-registry-test-project", "device-registry-test-addrspace", this.iotProjectNamespace);
            createIoTProject(iotProject);
        }
        if (deviceRegistryEndpoint == null) {
            deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        }
        if (httpAdapterEndpoint == null) {
            httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        }
        if (client == null) {
            client = new DeviceRegistryClient(kubernetes, deviceRegistryEndpoint);
        }
        this.randomDeviceId = UUID.randomUUID().toString();
    }

    @AfterEach
    public void tearDown(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            cleanEnv();
        }
    }

    @AfterAll
    public void cleanAll() throws Exception{
        log.info("Cleaning environment, test class completely executed");
        cleanEnv();
    }

    private void cleanEnv() throws Exception {
        if (!environment.skipCleanup()) {
            if (iotProject != null) {
                log.info("Shared IoTProject will be removed");
                var iotProjectApiClient = kubernetes.getIoTProjectClient(iotProject.getMetadata().getNamespace());
                if (iotProjectApiClient.withName(iotProject.getMetadata().getName()).get() != null) {
                    IoTUtils.deleteIoTProjectAndWait(kubernetes, iotProject);
                } else {
                    log.info("IoTProject '{}' doesn't exists!", iotProject.getMetadata().getName());
                }
                iotProject = null;
            }
            if (iotConfig != null) {
                removeIoTConfig();
                iotConfig = null;
            }
        } else {
            log.warn("Remove shared iotproject when test failed - SKIPPED!");
        }
    }

    @Test
    void testRegisterDevice() throws Exception {
        client.registerDevice(tenantId(), randomDeviceId);

        JsonObject result = client.getDeviceRegistration(tenantId(), randomDeviceId);
        assertEquals(randomDeviceId, result.getString("device-id"));
        assertNotNull(result.getJsonObject("data"));
        assertEquals(true, result.getJsonObject("data").getBoolean("enabled"));

        client.deleteDeviceRegistration(tenantId(), randomDeviceId);
        client.getDeviceRegistration(tenantId(), randomDeviceId, HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    void testDisableDevice() throws Exception {
        client.registerDevice(tenantId(), randomDeviceId);

        JsonObject payload = new JsonObject(Map.of("enabled", false));

        client.updateDeviceRegistration(tenantId(), randomDeviceId, payload);

        JsonObject result = client.getDeviceRegistration(tenantId(), randomDeviceId);
        assertEquals(randomDeviceId, result.getString("device-id"));
        assertNotNull(result.getJsonObject("data"));
        assertEquals(false, result.getJsonObject("data").getBoolean("enabled"));

        client.deleteDeviceRegistration(tenantId(), randomDeviceId);
        client.getDeviceRegistration(tenantId(), randomDeviceId, HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    void testDeviceCredentials() throws Exception {

        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {

            client.registerDevice(tenantId(), randomDeviceId);

            String authId = "sensor1234";
            String password = "password1234";
            credentialsClient.addCredentials(tenantId(), randomDeviceId, authId, password);

            JsonObject result = credentialsClient.getCredentials(tenantId(), randomDeviceId);
            assertNotNull(result);
            assertEquals(1, result.getInteger("total"));
            JsonArray credentials = result.getJsonArray("credentials");
            assertNotNull(credentials);
            assertEquals(1, credentials.size());
            JsonObject credential = credentials.getJsonObject(0);
            assertNotNull(credential);
            assertEquals(randomDeviceId, credential.getString("device-id"));
            assertEquals(authId, credential.getString("auth-id"));
            assertEquals(true, credential.getBoolean("enabled"));
            //TODO chech secret[0].pwd-hash matches "password1234" hash, waiting for issue #2569 to be resolved

            checkCredentials(authId, password, false);

            credentialsClient.deleteAllCredentials(tenantId(), randomDeviceId);
            credentialsClient.getCredentials(tenantId(), randomDeviceId, HttpURLConnection.HTTP_NOT_FOUND);

            client.deleteDeviceRegistration(tenantId(), randomDeviceId);
            client.getDeviceRegistration(tenantId(), randomDeviceId, HttpURLConnection.HTTP_NOT_FOUND);

        }
    }

    @Test
    void testSetExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {
            client.registerDevice(tenantId(), randomDeviceId);

            String authId = "sensor1234";
            String password = "password1234";
            credentialsClient.addCredentials(tenantId(), randomDeviceId, authId, password);

            checkCredentials(authId, password, false);

            int expirySeconds = 30;
            Instant notAfter = Instant.now().plusSeconds(expirySeconds);
            String newPassword = "new-password1234";
            credentialsClient.updateCredentials(tenantId(), randomDeviceId, authId, newPassword, notAfter);

            JsonObject result = credentialsClient.getCredentials(tenantId(), randomDeviceId);
            assertNotNull(result);
            assertEquals(1, result.getInteger("total"));
            JsonArray credentials = result.getJsonArray("credentials");
            assertNotNull(credentials);
            assertEquals(1, credentials.size());
            JsonObject credential = credentials.getJsonObject(0);
            assertNotNull(credential);
            assertEquals(randomDeviceId, credential.getString("device-id"));
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

            credentialsClient.deleteAllCredentials(tenantId(), randomDeviceId);
            credentialsClient.getCredentials(tenantId(), randomDeviceId, HttpURLConnection.HTTP_NOT_FOUND);

            client.deleteDeviceRegistration(tenantId(), randomDeviceId);
            client.getDeviceRegistration(tenantId(), randomDeviceId, HttpURLConnection.HTTP_NOT_FOUND);
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

    @Override
    public IoTConfig getSharedIoTConfig() {
        return iotConfig;
    }

    @Override
    public IoTProject getSharedIoTProject() {
        return iotProject;
    }

}
