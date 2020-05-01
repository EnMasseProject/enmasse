/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.registry;

import static io.enmasse.systemtest.TestTag.SMOKE;
import static io.enmasse.systemtest.iot.IoTTestSession.Adapter.HTTP;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.credentials.PasswordCredential;
import org.eclipse.hono.service.management.device.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
import io.enmasse.systemtest.iot.IoTTestSession.Adapter;
import io.enmasse.systemtest.utils.IoTUtils;

@Tag(SMOKE)
abstract class DeviceRegistryTest extends TestBase implements ITestIoTIsolated {

    private static final String DEVICE_REGISTRY_TEST_ADDRESSSPACE = "device-registry-test-addrspace";

    private static final String DEVICE_REGISTRY_TEST_PROJECT = "device-registry-test-project";

    private String randomDeviceId;
    private IoTProject iotProject;
    private Endpoint deviceRegistryEndpoint;
    private Endpoint httpAdapterEndpoint;
    private DeviceRegistryClient client;

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

        // disable all but HTTP

        for (Adapter adapter : Adapter.values()) {
            iotConfigBuilder = adapter.disable(iotConfigBuilder);
        }
        iotConfigBuilder = HTTP.enable(iotConfigBuilder);

        // build config

        var iotConfig = iotConfigBuilder.build();

        isolatedIoTManager.createIoTConfig(iotConfig);

        iotProject = IoTUtils.getBasicIoTProjectObject(DEVICE_REGISTRY_TEST_PROJECT,
                DEVICE_REGISTRY_TEST_ADDRESSSPACE, IOT_PROJECT_NAMESPACE, getDefaultAddressSpacePlan());
        isolatedIoTManager.createIoTProject(iotProject);

        deviceRegistryEndpoint = IoTUtils.getDeviceRegistryManagementEndpoint();

        httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");

        client = new DeviceRegistryClient(deviceRegistryEndpoint);

        this.randomDeviceId = UUID.randomUUID().toString();

        this.credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        isolatedIoTManager.createOrUpdateUser(isolatedIoTManager.getAddressSpace(IOT_PROJECT_NAMESPACE, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
        this.iotAmqpClientFactory = new AmqpClientFactory(resourcesManager.getAddressSpace(IOT_PROJECT_NAMESPACE, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
        this.amqpClient = iotAmqpClientFactory.createQueueClient();

    }

    protected void doTestRegisterDevice() throws Exception {
        client.registerDevice(isolatedIoTManager.getTenantId(), randomDeviceId);
        final Device result = client.getDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId);
        assertNotNull(result);
        assertDefaultEnabled(result.isEnabled());

        client.deleteDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId);
        client.getDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);
    }

    protected void doTestDisableDevice() throws Exception {
        client.registerDevice(isolatedIoTManager.getTenantId(), randomDeviceId);

        final Device payload = new Device();
        payload.setEnabled(false);

        client.updateDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId, payload);

        final Device result = client.getDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId);

        // as we set it explicitly, we expect the explicit value of "false"
        assertEquals(Boolean.FALSE, result.isEnabled());

        client.deleteDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId);
        client.getDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);
    }

    protected void doTestDeviceCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {

            client.registerDevice(isolatedIoTManager.getTenantId(), randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addCredentials(isolatedIoTManager.getTenantId(), randomDeviceId, authId, password, null, HttpURLConnection.HTTP_NO_CONTENT);

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(isolatedIoTManager.getTenantId(), randomDeviceId);

            client.deleteDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId);
            client.getDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);

        }
    }

    protected void doTestDeviceCredentialsPlainPassword() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {

            client.registerDevice(isolatedIoTManager.getTenantId(), randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addPlainPasswordCredentials(isolatedIoTManager.getTenantId(), randomDeviceId, authId, password, null, HttpURLConnection.HTTP_NO_CONTENT);

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(isolatedIoTManager.getTenantId(), randomDeviceId);

            client.deleteDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId);
            client.getDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);

        }
    }

    protected void doTestDeviceCredentialsDoesNotContainsPasswordDetails() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {

            client.registerDevice(isolatedIoTManager.getTenantId(), randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addPlainPasswordCredentials(isolatedIoTManager.getTenantId(), randomDeviceId, authId, password, null, HttpURLConnection.HTTP_NO_CONTENT);

            List<CommonCredential> credentials = credentialsClient.getCredentials(isolatedIoTManager.getTenantId(), randomDeviceId);

            assertEquals(1, credentials.size());
            PasswordCredential passwordCredential = ((PasswordCredential) credentials.get(0));
            assertEquals(1, passwordCredential.getSecrets().size());
            assertNull(passwordCredential.getSecrets().get(0).getHashFunction());
            assertNull(passwordCredential.getSecrets().get(0).getPasswordHash());
            assertNull(passwordCredential.getSecrets().get(0).getPasswordPlain());
            assertNull(passwordCredential.getSecrets().get(0).getSalt());

            credentialsClient.deleteAllCredentials(isolatedIoTManager.getTenantId(), randomDeviceId);

            client.deleteDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId);
            client.getDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);

        }
    }


    protected void doTestCacheExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {

            final Duration cacheExpiration = Duration.ofMinutes(3);

            // register device

            client.registerDevice(isolatedIoTManager.getTenantId(), randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final String password = "password1234";
            credentialsClient.addCredentials(isolatedIoTManager.getTenantId(), randomDeviceId, authId, password, null, HttpURLConnection.HTTP_NO_CONTENT);

            // first test, cache filled

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, iotProject);

            // set new password

            final String newPassword = "new-password1234";
            credentialsClient.updateCredentials(isolatedIoTManager.getTenantId(), randomDeviceId, authId, newPassword, null);

            // expect failure due to cached info

            IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, iotProject);
            LOGGER.info("Waiting {} seconds for credentials to expire", cacheExpiration);
            Thread.sleep(cacheExpiration.toMillis());

            // cache must be expired, new password can be used

            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(isolatedIoTManager.getTenantId(), randomDeviceId);

            client.deleteDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId);
            client.getDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);
        }
    }


    protected void doTestSetExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {
            client.registerDevice(isolatedIoTManager.getTenantId(), randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final Duration expiry = Duration.ofSeconds(30);
            final Instant notAfter = Instant.now().plus(expiry);
            final String newPassword = "password1234";

            credentialsClient.addCredentials(isolatedIoTManager.getTenantId(), randomDeviceId, authId, newPassword, notAfter, HttpURLConnection.HTTP_NO_CONTENT);

            // first check, must succeed
            Thread.sleep(20_000);
            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, iotProject);

            LOGGER.info("Waiting {} for credentials to expire", expiry);
            Thread.sleep(expiry.toMillis());

            // second check, after expiration, must fail

            IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(isolatedIoTManager.getTenantId(), randomDeviceId);

            client.deleteDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId);
            client.getDeviceRegistration(isolatedIoTManager.getTenantId(), randomDeviceId, HTTP_NOT_FOUND);
        }
    }


    protected void doTestCreateForNonExistingTenantFails() throws Exception {
        var response = client.registerDeviceWithResponse("invalid-" + isolatedIoTManager.getTenantId(), randomDeviceId);
        assertEquals(tenantDoesNotExistCode(), response.statusCode());
    }

    protected void doTestTenantDeletionTriggersDevicesDeletion() throws Exception {
        var tenantId = isolatedIoTManager.getTenantId();
        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {
            client.registerDevice(tenantId, randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final Duration expiry = Duration.ofSeconds(30);
            final Instant notAfter = Instant.now().plus(expiry);
            final String newPassword = "password1234";

            credentialsClient.addCredentials(tenantId, randomDeviceId, authId, newPassword, notAfter, HttpURLConnection.HTTP_NO_CONTENT);

            // first check, must succeed

            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, iotProject);

            // Now delete the tenant
            isolatedIoTManager.deleteIoTProject(iotProject);

            // second check, the credentials and device should be deleted

            client.getDeviceRegistration(tenantId, randomDeviceId, tenantDoesNotExistCode());
            IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, iotProject);
        }
    }

    protected void doCreateDuplicateDeviceFails() throws Exception {
        var tenantId = isolatedIoTManager.getTenantId();
        var deviceId = UUID.randomUUID().toString();

        // create device

        var response = client.registerDeviceWithResponse(tenantId, deviceId);
        assertEquals(HTTP_CREATED, response.statusCode());

        // create device a second time

        var response2 = client.registerDeviceWithResponse(tenantId, deviceId);
        assertEquals(HTTP_CONFLICT, response2.statusCode());
    }

    @Test
    public void testDeviceWithSameAuthIdOfDifferentTypesSucceeds() throws Exception {

        var tenantId = isolatedIoTManager.getTenantId();
        var deviceId = UUID.randomUUID().toString();

        // create device

        client.registerDevice(tenantId, deviceId);

        var authId = UUID.randomUUID().toString();

        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {
            credentialsClient.addPlainPasswordCredentials(tenantId, deviceId, authId, "foo-bar", null, HttpURLConnection.HTTP_NO_CONTENT);
            credentialsClient.addPskCredentials(tenantId, deviceId, authId, "foo-bar".getBytes(StandardCharsets.UTF_8), null, HttpURLConnection.HTTP_NO_CONTENT);
        }

    }

    @Test
    public void testDeviceWithSameAuthIdOfSameTypesFails() throws Exception {

        var tenantId = isolatedIoTManager.getTenantId();
        var deviceId1 = UUID.randomUUID().toString();
        var deviceId2 = UUID.randomUUID().toString();

        // create device

        client.registerDevice(tenantId, deviceId1);
        client.registerDevice(tenantId, deviceId2);

        var authId = UUID.randomUUID().toString();

        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {
            // must succeed
            credentialsClient.addPlainPasswordCredentials(tenantId, deviceId1, authId, "foo-bar", null, HttpURLConnection.HTTP_NO_CONTENT);
            // must fail
            credentialsClient.addPlainPasswordCredentials(tenantId, deviceId2, authId, "foo-bar", null, HttpURLConnection.HTTP_BAD_REQUEST);
        }

    }

    protected void doRegisterMultipleDevices() throws Exception {

        final String tenantId = isolatedIoTManager.getTenantId();
        final String prefix = UUID.randomUUID().toString();
        final Set<String> devices = new HashSet<>();

        for (int i = 0; i < 1_000; i++) {
            var deviceId = prefix + "-" + UUID.randomUUID().toString();
            client.registerDevice(tenantId, deviceId);
            devices.add(deviceId);
        }

        try (var credentialsClient = new CredentialsRegistryClient(deviceRegistryEndpoint)) {
            for (final String deviceId : devices) {
                credentialsClient.addPlainPasswordCredentials(tenantId, deviceId, UUID.randomUUID().toString(), UUID.randomUUID().toString(), null, HttpURLConnection.HTTP_NO_CONTENT);
            }
        }

        for (final String deviceId : devices) {
            final Device result = client.getDeviceRegistration(tenantId, deviceId);
            assertNotNull(result);
            assertDefaultEnabled(result.isEnabled());
        }

        for (final String deviceId : devices) {
            client.deleteDeviceRegistration(tenantId, deviceId);
        }

        for (final String deviceId : devices) {
            client.getDeviceRegistration(tenantId, deviceId, HTTP_NOT_FOUND);
        }

    }

}