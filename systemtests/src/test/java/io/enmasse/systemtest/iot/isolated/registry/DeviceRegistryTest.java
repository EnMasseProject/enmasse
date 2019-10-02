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
import io.enmasse.systemtest.bases.iot.ITestIoTShared;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import org.eclipse.hono.service.management.device.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static io.enmasse.systemtest.TestTag.IOT_DEVICE_REG;
import static io.enmasse.systemtest.TestTag.SMOKE;
import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.deviceRegistry;
import static io.enmasse.systemtest.utils.IoTUtils.createIoTConfig;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(SMOKE)
@Tag(IOT_DEVICE_REG)
abstract class DeviceRegistryTest extends TestBase implements ITestIoTIsolated {

    private static final String DEVICE_REGISTRY_TEST_ADDRESSSPACE = "device-registry-test-addrspace";

    private static final String DEVICE_REGISTRY_TEST_PROJECT = "device-registry-test-project";


    private String randomDeviceId;
    private IoTConfig iotConfig = null;
    private IoTProject iotProject = null;
    private Endpoint deviceRegistryEndpoint = null;
    private Endpoint httpAdapterEndpoint = null;
    private DeviceRegistryClient client = null;

    private UserCredentials credentials;

    private AmqpClientFactory iotAmqpClientFactory;

    private AmqpClient amqpClient;

    protected abstract IoTConfigBuilder provideIoTConfig() throws Exception;

    @BeforeEach
    public void setAttributes() throws Exception {
        var iotConfigBuilder = provideIoTConfig();
        iotConfig = iotConfigBuilder.editSpec()
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
        isolatedIoTManager.createIoTConfig(iotConfig);

        iotProject = IoTUtils.getBasicIoTProjectObject(DEVICE_REGISTRY_TEST_PROJECT,
                DEVICE_REGISTRY_TEST_ADDRESSSPACE, this.iotProjectNamespace, getDefaultAddressSpacePlan());
        isolatedIoTManager.createIoTProject(iotProject);

        deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");

        httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");

        client = new DeviceRegistryClient(kubernetes, deviceRegistryEndpoint);

        this.randomDeviceId = UUID.randomUUID().toString();

        this.credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        isolatedIoTManager.createOrUpdateUser(isolatedIoTManager.getAddressSpace(this.iotProjectNamespace, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
        this.iotAmqpClientFactory = new AmqpClientFactory(resourcesManager.getAddressSpace(this.iotProjectNamespace, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
        this.amqpClient = iotAmqpClientFactory.createQueueClient();
        
    }

    protected void doTestRegisterDevice() throws Exception {
        client.registerDevice(isolatedIoTManager.getTenantID(), randomDeviceId);
        final Device result = client.getDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId);
        assertNotNull(result);
        assertDefaultEnabled(result.getEnabled());

        client.deleteDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId);
        client.getDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId, HTTP_NOT_FOUND);
    }

    protected void doTestDisableDevice() throws Exception {
        client.registerDevice(isolatedIoTManager.getTenantID(), randomDeviceId);

        final Device payload = new Device();
        payload.setEnabled(false);

        client.updateDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId, payload);

        final Device result = client.getDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId);

        // as we set it explicitly, we expect the explicit value of "false"
        assertEquals(Boolean.FALSE, result.getEnabled());

        client.deleteDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId);
        client.getDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId, HTTP_NOT_FOUND);
    }

    
    protected void doTestDeviceCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {

            client.registerDevice(isolatedIoTManager.getTenantID(), randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addCredentials(isolatedIoTManager.getTenantID(), randomDeviceId, authId, password);

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(isolatedIoTManager.getTenantID(), randomDeviceId);

            client.deleteDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId);
            client.getDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId, HTTP_NOT_FOUND);

        }
    }


    protected void doTestCacheExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {

            final Duration cacheExpiration = Duration.ofMinutes(3);

            // register device

            client.registerDevice(isolatedIoTManager.getTenantID(), randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final String password = "password1234";
            credentialsClient.addCredentials(isolatedIoTManager.getTenantID(), randomDeviceId, authId, password);

            // first test, cache filled

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, iotProject);

            // set new password

            final String newPassword = "new-password1234";
            credentialsClient.updateCredentials(isolatedIoTManager.getTenantID(), randomDeviceId, authId, newPassword, null);

            // expect failure due to cached info

            IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, iotProject);
            LOGGER.info("Waiting {} seconds for credentials to expire", cacheExpiration);
            Thread.sleep(cacheExpiration.toMillis());

            // cache must be expired, new password can be used

            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(isolatedIoTManager.getTenantID(), randomDeviceId);

            client.deleteDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId);
            client.getDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId, HTTP_NOT_FOUND);
        }
    }

    
    protected void doTestSetExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {
            client.registerDevice(isolatedIoTManager.getTenantID(), randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final Duration expiry = Duration.ofSeconds(30);
            final Instant notAfter = Instant.now().plus(expiry);
            final String newPassword = "password1234";

            credentialsClient.addCredentials(isolatedIoTManager.getTenantID(), randomDeviceId, authId, newPassword, notAfter);

            // first check, must succeed
            Thread.sleep(20_000);
            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, iotProject);

            LOGGER.info("Waiting {} for credentials to expire", expiry);
            Thread.sleep(expiry.toMillis());

            // second check, after expiration, must fail

            IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, iotProject);

            credentialsClient.deleteAllCredentials(isolatedIoTManager.getTenantID(), randomDeviceId);

            client.deleteDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId);
            client.getDeviceRegistration(isolatedIoTManager.getTenantID(), randomDeviceId, HTTP_NOT_FOUND);
        }
    }

    
    protected void doTestCreateForNonExistingTenantFails() throws Exception {
        var response = client.registerDeviceWithResponse("invalid-" + isolatedIoTManager.getTenantID(), randomDeviceId);
        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }


}