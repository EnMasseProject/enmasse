/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.registry;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.IoTConstants;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.utils.IoTUtils;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.credentials.PasswordCredential;
import org.eclipse.hono.service.management.device.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.enmasse.systemtest.TestTag.ISOLATED_IOT;
import static io.enmasse.systemtest.TestTag.SMOKE;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag(SMOKE)
@Tag(ISOLATED_IOT)
abstract class DeviceRegistryTest extends TestBase {

    private static final String DEVICE_REGISTRY_TEST_ADDRESSSPACE = "device-registry-test-addrspace";

    private static final String DEVICE_REGISTRY_TEST_PROJECT = "device-registry-test-project";

    private static final Logger LOGGER = CustomLogger.getLogger();

    private String randomDeviceId;
    private IoTConfig iotConfig;
    private IoTProject iotProject;
    private Endpoint deviceRegistryEndpoint;
    private Endpoint httpAdapterEndpoint;
    private DeviceRegistryClient client;
    private String tenantId;

    private UserCredentials credentials;

    private AmqpClientFactory iotAmqpClientFactory;

    private AmqpClient amqpClient;

    protected abstract IoTConfigBuilder provideIoTConfig() throws Exception;

    protected int tenantDoesNotExistCode() {
        return HttpURLConnection.HTTP_UNAUTHORIZED;
    }

    @BeforeEach
    public void setAttributes() throws Exception {
        var iotConfigBuilder = provideIoTConfig();
        iotConfig = iotConfigBuilder
                .withNewMetadata()
                .withName("default")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .editSpec()
                .withNewAdapters()
                .withNewMqtt()
                .withEnabled(false)
                .endMqtt()
                .withNewLoraWan()
                .withEnabled(false)
                .endLoraWan()
                .withNewSigfox()
                .withEnabled(false)
                .endSigfox()
                .endAdapters()
                .endSpec()
                .build();

        resourceManager.createResource(iotConfig);

        iotProject = IoTUtils.getBasicIoTProjectObject(DEVICE_REGISTRY_TEST_PROJECT,
                DEVICE_REGISTRY_TEST_ADDRESSSPACE, IoTConstants.IOT_PROJECT_NAMESPACE, IoTConstants.IOT_DEFAULT_ADDRESS_SPACE_PLAN);
        resourceManager.createResource(iotProject);

        deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");

        httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");

        client = new DeviceRegistryClient(deviceRegistryEndpoint);

        this.randomDeviceId = UUID.randomUUID().toString();

        this.credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        resourceManager.createOrUpdateUser(resourceManager.getAddressSpace(IoTConstants.IOT_PROJECT_NAMESPACE, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
        this.iotAmqpClientFactory = new AmqpClientFactory(resourceManager.getAddressSpace(IoTConstants.IOT_PROJECT_NAMESPACE, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
        this.amqpClient = iotAmqpClientFactory.createQueueClient();
        tenantId = resourceManager.getTenantId(iotProject);
    }

    protected void doTestRegisterDevice() throws Exception {
        client.registerDevice(tenantId, randomDeviceId);
        final Device result = client.getDeviceRegistration(tenantId, randomDeviceId);
        assertNotNull(result);
        assertDefaultEnabled(result.getEnabled());

        client.deleteDeviceRegistration(tenantId, randomDeviceId);
        client.getDeviceRegistration(tenantId, randomDeviceId, HTTP_NOT_FOUND);
    }

    protected void doTestDisableDevice() throws Exception {
        client.registerDevice(tenantId, randomDeviceId);

        final Device payload = new Device();
        payload.setEnabled(false);

        client.updateDeviceRegistration(tenantId, randomDeviceId, payload);

        final Device result = client.getDeviceRegistration(tenantId, randomDeviceId);

        // as we set it explicitly, we expect the explicit value of "false"
        assertEquals(Boolean.FALSE, result.getEnabled());

        client.deleteDeviceRegistration(tenantId, randomDeviceId);
        client.getDeviceRegistration(tenantId, randomDeviceId, HTTP_NOT_FOUND);
    }

    protected void doTestDeviceCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {

            client.registerDevice(tenantId, randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addCredentials(tenantId, randomDeviceId, authId, password);

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(tenantId, randomDeviceId);

            client.deleteDeviceRegistration(tenantId, randomDeviceId);
            client.getDeviceRegistration(tenantId, randomDeviceId, HTTP_NOT_FOUND);

        }
    }

    protected void doTestDeviceCredentialsPlainPassword() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {

            client.registerDevice(tenantId, randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addPlainPasswordCredentials(tenantId, randomDeviceId, authId, password);

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(tenantId, randomDeviceId);

            client.deleteDeviceRegistration(tenantId, randomDeviceId);
            client.getDeviceRegistration(tenantId, randomDeviceId, HTTP_NOT_FOUND);

        }
    }

    protected void doTestDeviceCredentialsDoesNotContainsPasswordDetails() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {

            client.registerDevice(tenantId, randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addPlainPasswordCredentials(tenantId, randomDeviceId, authId, password);

            List<CommonCredential> credentials = credentialsClient.getCredentials(tenantId, randomDeviceId);

            assertEquals(1, credentials.size());
            PasswordCredential passwordCredential = ((PasswordCredential) credentials.get(0));
            assertEquals(1, passwordCredential.getSecrets().size());
            assertNull(passwordCredential.getSecrets().get(0).getHashFunction());
            assertNull(passwordCredential.getSecrets().get(0).getPasswordHash());
            assertNull(passwordCredential.getSecrets().get(0).getPasswordPlain());
            assertNull(passwordCredential.getSecrets().get(0).getSalt());

            credentialsClient.deleteAllCredentials(tenantId, randomDeviceId);

            client.deleteDeviceRegistration(tenantId, randomDeviceId);
            client.getDeviceRegistration(tenantId, randomDeviceId, HTTP_NOT_FOUND);

        }
    }


    protected void doTestCacheExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {

            final Duration cacheExpiration = Duration.ofMinutes(3);

            // register device

            client.registerDevice(tenantId, randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final String password = "password1234";
            credentialsClient.addCredentials(tenantId, randomDeviceId, authId, password);

            // first test, cache filled

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, iotProject);

            // set new password

            final String newPassword = "new-password1234";
            credentialsClient.updateCredentials(tenantId, randomDeviceId, authId, newPassword, null);

            // expect failure due to cached info

            IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, iotProject);
            LOGGER.info("Waiting {} seconds for credentials to expire", cacheExpiration);
            Thread.sleep(cacheExpiration.toMillis());

            // cache must be expired, new password can be used

            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(tenantId, randomDeviceId);

            client.deleteDeviceRegistration(tenantId, randomDeviceId);
            client.getDeviceRegistration(tenantId, randomDeviceId, HTTP_NOT_FOUND);
        }
    }


    protected void doTestSetExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {
            client.registerDevice(tenantId, randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final Duration expiry = Duration.ofSeconds(30);
            final Instant notAfter = Instant.now().plus(expiry);
            final String newPassword = "password1234";

            credentialsClient.addCredentials(tenantId, randomDeviceId, authId, newPassword, notAfter);

            // first check, must succeed
            Thread.sleep(20_000);
            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, iotProject);

            LOGGER.info("Waiting {} for credentials to expire", expiry);
            Thread.sleep(expiry.toMillis());

            // second check, after expiration, must fail

            IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(tenantId, randomDeviceId);

            client.deleteDeviceRegistration(tenantId, randomDeviceId);
            client.getDeviceRegistration(tenantId, randomDeviceId, HTTP_NOT_FOUND);
        }
    }


    protected void doTestCreateForNonExistingTenantFails() throws Exception {
        var response = client.registerDeviceWithResponse("invalid-" + tenantId, randomDeviceId);
        assertEquals(tenantDoesNotExistCode(), response.statusCode());
    }

    protected void doTestTenantDeletionTriggersDevicesDeletion() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {
            client.registerDevice(tenantId, randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final Duration expiry = Duration.ofSeconds(30);
            final Instant notAfter = Instant.now().plus(expiry);
            final String newPassword = "password1234";

            credentialsClient.addCredentials(tenantId, randomDeviceId, authId, newPassword, notAfter);

            // first check, must succeed

            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, iotProject);

            // Now delete the tenant
            resourceManager.removeResource(iotProject);

            // second check, the credentials and device should be deleted

            client.getDeviceRegistration(tenantId, randomDeviceId, tenantDoesNotExistCode());
            IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, iotProject);
        }
    }

    protected void doCreateDuplicateDeviceFails() throws Exception {
        var deviceId = UUID.randomUUID().toString();

        // create device

        var response = client.registerDeviceWithResponse(tenantId, deviceId);
        assertEquals(HTTP_CREATED, response.statusCode());

        // create device a second time

        var response2 = client.registerDeviceWithResponse(tenantId, deviceId);
        assertEquals(HTTP_CONFLICT, response2.statusCode());
    }

}