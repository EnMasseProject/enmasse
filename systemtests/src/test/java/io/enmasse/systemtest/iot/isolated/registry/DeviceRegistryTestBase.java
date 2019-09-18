/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.registry;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.platform.Kubernetes;
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
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.CommonResourcesManager;
import io.enmasse.systemtest.utils.IoTUtils;
import org.eclipse.hono.service.management.device.Device;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import static io.enmasse.systemtest.TestTag.SHARED_IOT;
import static io.enmasse.systemtest.TestTag.SMOKE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(SHARED_IOT)
@Tag(SMOKE)
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

    /**
     * Test if the enabled flag is set to "enabled".
     * <br>
     * The flag is considered "enabled", in case the value is "true" or missing.
     *
     * @param enabled The object to test.
     */
    private static void assertDefaultEnabled(final Boolean enabled) {
        if ( enabled != null && !Boolean.TRUE.equals(enabled)) {
            fail("Default value must be 'null' or 'true'");
        }
    }

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

    @BeforeAll
    void forceTeardown() throws Exception {
        var kubernetes = Kubernetes.getInstance();
        log.info("Deleting all IoT resources before start");
        for (IoTProject project : kubernetes.getIoTProjectClient(iotProjectNamespace).list().getItems()) {
            IoTUtils.deleteIoTProjectAndWait(kubernetes, project);
        }
        for (IoTConfig config : kubernetes.getIoTConfigClient(Kubernetes.getInstance().getInfraNamespace()).list().getItems()) {
            IoTUtils.deleteIoTConfigAndWait(kubernetes, config);
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
        CommonResourcesManager.getInstance().createOrUpdateUser(resourcesManager.getAddressSpace(this.iotProjectNamespace, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
        this.iotAmqpClientFactory = new AmqpClientFactory(resourcesManager.getAddressSpace(this.iotProjectNamespace, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
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
    public void cleanAll() throws Exception {
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

        final Device result = client.getDeviceRegistration(tenantId(), randomDeviceId);
        assertNotNull(result);
        assertDefaultEnabled(result.getEnabled());

        client.deleteDeviceRegistration(tenantId(), randomDeviceId);
        client.getDeviceRegistration(tenantId(), randomDeviceId, HTTP_NOT_FOUND);
    }

    @Test
    void testDisableDevice() throws Exception {
        client.registerDevice(tenantId(), randomDeviceId);

        final Device payload = new Device();
        payload.setEnabled(false);

        client.updateDeviceRegistration(tenantId(), randomDeviceId, payload);

        final Device result = client.getDeviceRegistration(tenantId(), randomDeviceId);

        // as we set it explicitly, we expect the explicit value of "false"
        assertEquals(Boolean.FALSE, result.getEnabled());

        client.deleteDeviceRegistration(tenantId(), randomDeviceId);
        client.getDeviceRegistration(tenantId(), randomDeviceId, HTTP_NOT_FOUND);
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
            client.getDeviceRegistration(tenantId(), randomDeviceId, HTTP_NOT_FOUND);

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
            client.getDeviceRegistration(tenantId(), randomDeviceId, HTTP_NOT_FOUND);
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
            client.getDeviceRegistration(tenantId(), randomDeviceId, HTTP_NOT_FOUND);
        }
    }

    @Test
    void testCreateForNonExistingTenantFails() throws Exception {
        var response = client.registerDeviceWithResponse("invalid-" + tenantId(), randomDeviceId);
        assertEquals(HTTP_NOT_FOUND, response.statusCode());
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

    protected void assertCorrectRegistryType(final String type) {
        final Deployment deployment = kubernetes.getClient().apps().deployments().inNamespace(Kubernetes.getInstance().getInfraNamespace()).withName("iot-device-registry").get();
        assertNotNull(deployment);
        assertEquals(type, deployment.getMetadata().getAnnotations().get("iot.enmasse.io/registry.type"));
    }


}
