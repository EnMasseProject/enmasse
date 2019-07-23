/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.registry;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static io.enmasse.systemtest.TestTag.smoke;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.IoTTestBase;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.iot.MessageSendTester.Type;
import io.enmasse.systemtest.utils.IoTUtils;
import io.vertx.core.json.JsonObject;

@Tag(sharedIot)
@Tag(smoke)
public abstract class DeviceRegistryTestBase extends IoTTestBase {

    private static final String DEVICE_REGISTRY_TEST_ADDRESSSPACE = "device-registry-test-addrspace";

    private static final String DEVICE_REGISTRY_TEST_PROJECT = "device-registry-test-project";

    private static final Logger log = CustomLogger.getLogger();

    private String randomDeviceId;
    private IoTConfig iotConfig;
    private IoTProject iotProject;
    private Endpoint deviceRegistryEndpoint;
    private Endpoint httpAdapterEndpoint;
    private DeviceRegistryClient client;

    private UserCredentials credentials;

    private AmqpClientFactory iotAmqpClientFactory;

    private AmqpClient iotAmqpClient;

    protected abstract IoTConfig provideIoTConfig() throws Exception;

    /**
     * Test if the enabled flag is set to "enabled".
     * <br>
     * The flag is considered "enabled", in case the value is "true" or missing.
     * @param obj The object to test.
     */
    private static void assertDefaultEnabled(final JsonObject obj) {
        assertTrue(obj.getBoolean("enabled", true));
    }

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
            iotProject = IoTUtils.getBasicIoTProjectObject(DEVICE_REGISTRY_TEST_PROJECT, DEVICE_REGISTRY_TEST_ADDRESSSPACE, this.iotProjectNamespace);
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

        this.credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        createOrUpdateUser(getAddressSpace(this.iotProjectNamespace, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
        this.iotAmqpClientFactory = new AmqpClientFactory(getAddressSpace(this.iotProjectNamespace, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
        this.iotAmqpClient = iotAmqpClientFactory.createQueueClient();
    }

    @AfterEach
    public void tearDown(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            cleanEnv();
        }
        if (this.iotAmqpClient != null) {
            this.iotAmqpClient.close();
            this.iotAmqpClient = null;
        }
        if (this.iotAmqpClientFactory != null) {
            this.iotAmqpClientFactory.close();
            this.iotAmqpClientFactory = null;
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
        assertDefaultEnabled(result.getJsonObject("data"));

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

        // as we set it explicitly, we expect the explicit value of "false"
        assertEquals(false, result.getJsonObject("data").getBoolean("enabled"));

        client.deleteDeviceRegistration(tenantId(), randomDeviceId);
        client.getDeviceRegistration(tenantId(), randomDeviceId, HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    void testDeviceCredentials() throws Exception {

        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {

            client.registerDevice(tenantId(), randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addCredentials(tenantId(), randomDeviceId, authId, password);

            checkCredentials(authId, password, false);

            credentialsClient.deleteAllCredentials(tenantId(), randomDeviceId);

            client.deleteDeviceRegistration(tenantId(), randomDeviceId);
            client.getDeviceRegistration(tenantId(), randomDeviceId, HttpURLConnection.HTTP_NOT_FOUND);

        }
    }

    @Disabled("Caches expire a bit unpredictably")
    @Test
    void testCacheExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {

           final Duration cacheExpiration = Duration.ofMinutes(3);

            // register device

            client.registerDevice(tenantId(), randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final String password = "password1234";
            credentialsClient.addCredentials(tenantId(), randomDeviceId, authId, password);

            // first test, cache filled

            checkCredentials(authId, password, false);

            // set new password

            final String newPassword = "new-password1234";
            credentialsClient.updateCredentials(tenantId(), randomDeviceId, authId, newPassword, null);

            // expect failure due to cached info

            checkCredentials(authId, newPassword, true);
            log.info("Waiting {} seconds for credentials to expire", cacheExpiration);
            Thread.sleep(cacheExpiration.toMillis());

            // cache must be expired, new password can be used

            checkCredentials(authId, newPassword, false);

            credentialsClient.deleteAllCredentials(tenantId(), randomDeviceId);

            client.deleteDeviceRegistration(tenantId(), randomDeviceId);
            client.getDeviceRegistration(tenantId(), randomDeviceId, HttpURLConnection.HTTP_NOT_FOUND);
        }
    }

    @Test
    void testSetExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {
            client.registerDevice(tenantId(), randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final Duration expiry = Duration.ofSeconds(30);
            final Instant notAfter = Instant.now().plus(expiry);
            final String newPassword = "password1234";

            credentialsClient.addCredentials(tenantId(), randomDeviceId, authId, newPassword, notAfter);

            // first check, must succeed

            checkCredentials(authId, newPassword, false);

            log.info("Waiting {} for credentials to expire", expiry);
            Thread.sleep(expiry.toMillis());

            // second check, after expiration, must fail

            checkCredentials(authId, newPassword, true);

            credentialsClient.deleteAllCredentials(tenantId(), randomDeviceId);

            client.deleteDeviceRegistration(tenantId(), randomDeviceId);
            client.getDeviceRegistration(tenantId(), randomDeviceId, HttpURLConnection.HTTP_NOT_FOUND);
        }
    }

    private void checkCredentials(String authId, String password, boolean authFail) throws Exception {

        try (var httpAdapterClient = new HttpAdapterClient(kubernetes, httpAdapterEndpoint, authId, tenantId(), password)) {

            try {
                new MessageSendTester()
                        .type(Type.TELEMETRY)
                        .amount(1)
                        .consumerFactory(ConsumerFactory.of(iotAmqpClient, tenantId()))
                        .sender(httpAdapterClient::send)
                        .execute();
                if (authFail) {
                    fail("Expected to fail telemetry test");
                }
            } catch (TimeoutException e) {
                if (!authFail) {
                    throw e;
                }
            }

            try {
                new MessageSendTester()
                        .type(Type.EVENT)
                        .amount(1)
                        .consumerFactory(ConsumerFactory.of(iotAmqpClient, tenantId()))
                        .sender(httpAdapterClient::send)
                        .execute();
                if (authFail) {
                    fail("Expected to fail telemetry test");
                }
            } catch (TimeoutException e) {
                if (!authFail) {
                    throw e;
                }
            }

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
