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
import io.enmasse.systemtest.bases.iot.ITestIoTIsolated;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.utils.IoTUtils;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.credentials.PasswordCredential;
import io.enmasse.systemtest.utils.TestUtils;
import org.eclipse.hono.service.management.device.Device;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.enmasse.systemtest.TestTag.SMOKE;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag(SMOKE)
abstract class DeviceRegistryTest extends TestBase implements ITestIoTIsolated {

    private static final String DEVICE_REGISTRY_TEST_ADDRESSSPACE = "device-registry-test-addrspace";

    private static final String DEVICE_REGISTRY_TEST_PROJECT = "device-registry-test-project";


    private String randomDeviceId;
    private IoTProject iotProject;
    private Endpoint deviceRegistryEndpoint;
    private Endpoint httpAdapterEndpoint;
    private DeviceRegistryClient client;

    private AmqpClient amqpClient;

    protected abstract IoTConfigBuilder provideIoTConfig() throws Exception;

    @BeforeEach
    public void setAttributes() throws Exception {
        var iotConfigBuilder = provideIoTConfig();
        IoTConfig iotConfig = iotConfigBuilder
                .withNewMetadata()
                .withName("default")
                .withNamespace(KUBERNETES.getInfraNamespace())
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

        ISOLATED_IOT_MANAGER.createIoTConfig(iotConfig);

        iotProject = IoTUtils.getBasicIoTProjectObject(DEVICE_REGISTRY_TEST_PROJECT,
                DEVICE_REGISTRY_TEST_ADDRESSSPACE, IOT_PROJECT_NAMESPACE, getDefaultAddressSpacePlan());
        ISOLATED_IOT_MANAGER.createIoTProject(iotProject);

        deviceRegistryEndpoint = KUBERNETES.getExternalEndpoint("device-registry");

        httpAdapterEndpoint = KUBERNETES.getExternalEndpoint("iot-http-adapter");

        client = new DeviceRegistryClient(KUBERNETES, deviceRegistryEndpoint);

        this.randomDeviceId = UUID.randomUUID().toString();

        UserCredentials credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        ISOLATED_IOT_MANAGER.createOrUpdateUser(ISOLATED_IOT_MANAGER.getAddressSpace(IOT_PROJECT_NAMESPACE, DEVICE_REGISTRY_TEST_ADDRESSSPACE), credentials);
        AmqpClientFactory iotAmqpClientFactory = new AmqpClientFactory(resourcesManager.getAddressSpace(IOT_PROJECT_NAMESPACE, DEVICE_REGISTRY_TEST_ADDRESSSPACE), credentials);
        this.amqpClient = iotAmqpClientFactory.createQueueClient();

    }

    void doTestRegisterDevice() throws Exception {
        client.registerDevice(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);
        final Device result = client.getDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);
        assertNotNull(result);
        TestUtils.assertDefaultEnabled(result.getEnabled());

        client.deleteDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);
        client.getDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);
    }

    void doTestDisableDevice() throws Exception {
        client.registerDevice(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

        final Device payload = new Device();
        payload.setEnabled(false);

        client.updateDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, payload);

        final Device result = client.getDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

        // as we set it explicitly, we expect the explicit value of "false"
        assertEquals(Boolean.FALSE, result.getEnabled());

        client.deleteDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);
        client.getDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);
    }

    void doTestDeviceCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(KUBERNETES, deviceRegistryEndpoint)) {

            client.registerDevice(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, authId, password);

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

            client.deleteDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);
            client.getDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);

        }
    }

    void doTestDeviceCredentialsPlainPassword() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(KUBERNETES, deviceRegistryEndpoint)) {

            client.registerDevice(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addPlainPasswordCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, authId, password);

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

            client.deleteDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);
            client.getDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);

        }
    }

    void doTestDeviceCredentialsDoesNotContainsPasswordDetails() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(KUBERNETES, deviceRegistryEndpoint)) {

            client.registerDevice(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addPlainPasswordCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, authId, password);

            List<CommonCredential> credentials = credentialsClient.getCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

            assertEquals(1, credentials.size());
            PasswordCredential passwordCredential = ((PasswordCredential) credentials.get(0));
            assertEquals(1, passwordCredential.getSecrets().size());
            assertNull(passwordCredential.getSecrets().get(0).getHashFunction());
            assertNull(passwordCredential.getSecrets().get(0).getPasswordHash());
            assertNull(passwordCredential.getSecrets().get(0).getPasswordPlain());
            assertNull(passwordCredential.getSecrets().get(0).getSalt());

            credentialsClient.deleteAllCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

            client.deleteDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);
            client.getDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);

        }
    }


    void doTestCacheExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(KUBERNETES, deviceRegistryEndpoint)) {

            final Duration cacheExpiration = Duration.ofMinutes(3);

            // register device

            client.registerDevice(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final String password = "password1234";
            credentialsClient.addCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, authId, password);

            // first test, cache filled

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, iotProject);

            // set new password

            final String newPassword = "new-password1234";
            credentialsClient.updateCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, authId, newPassword, null);

            // expect failure due to cached info

            IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, iotProject);
            LOGGER.info("Waiting {} seconds for credentials to expire", cacheExpiration);
            Thread.sleep(cacheExpiration.toMillis());

            // cache must be expired, new password can be used

            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

            client.deleteDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);
            client.getDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);
        }
    }


    void doTestSetExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(KUBERNETES, deviceRegistryEndpoint)) {
            client.registerDevice(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final Duration expiry = Duration.ofSeconds(30);
            final Instant notAfter = Instant.now().plus(expiry);
            final String newPassword = "password1234";

            credentialsClient.addCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, authId, newPassword, notAfter);

            // first check, must succeed
            Thread.sleep(20_000);
            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, iotProject);

            LOGGER.info("Waiting {} for credentials to expire", expiry);
            Thread.sleep(expiry.toMillis());

            // second check, after expiration, must fail

            IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);

            client.deleteDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);
            client.getDeviceRegistration(ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);
        }
    }


    void doTestCreateForNonExistingTenantFails() throws Exception {
        var response = client.registerDeviceWithResponse("invalid-" + ISOLATED_IOT_MANAGER.getTenantId(), randomDeviceId);
        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    void doCreateDuplicateDeviceFails() throws Exception {
        var tenantId = ISOLATED_IOT_MANAGER.getTenantId();
        var deviceId = UUID.randomUUID().toString();

        // create device

        var response = client.registerDeviceWithResponse(tenantId, deviceId);
        assertEquals(HTTP_CREATED, response.statusCode());

        // create device a second time

        var response2 = client.registerDeviceWithResponse(tenantId, deviceId);
        assertEquals(HTTP_CONFLICT, response2.statusCode());
    }

}