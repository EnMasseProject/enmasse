/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.shared.registry;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTShared;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.IoTUtils;
import org.eclipse.hono.service.management.device.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static io.enmasse.systemtest.TestTag.IOT_DEVICE_REG;
import static io.enmasse.systemtest.TestTag.SHARED_IOT;
import static io.enmasse.systemtest.TestTag.SMOKE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(SMOKE)
@Tag(IOT_DEVICE_REG)
public abstract class DeviceRegistryTest extends TestBase implements ITestIoTShared  {

    protected DeviceRegistryClient client;
    protected String randomDeviceId;
    protected Kubernetes kubernetes = Kubernetes.getInstance();
    protected Endpoint deviceRegistryEndpoint;
    protected Endpoint httpAdapterEndpoint;
    private AmqpClient amqpClient;

    @BeforeEach
    void setAttributes() {
        client = sharedIoTResourceManager.getDevClient();
        randomDeviceId = UUID.randomUUID().toString();
        deviceRegistryEndpoint = sharedIoTResourceManager.getDeviceRegistryEndpoint();
        httpAdapterEndpoint = sharedIoTResourceManager.getHttpAdapterEndpoint();
        amqpClient = sharedIoTResourceManager.getAmqpClient();
        
    }

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

    @Test
    void testRegisterDevice() throws Exception {
        client.registerDevice(sharedIoTResourceManager.getTenantID(), randomDeviceId);
        final Device result = client.getDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId);
        assertNotNull(result);
        assertDefaultEnabled(result.getEnabled());

        client.deleteDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId);
        client.getDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId, HTTP_NOT_FOUND);
    }

    @Test
    void testDisableDevice() throws Exception {
        client.registerDevice(sharedIoTResourceManager.getTenantID(), randomDeviceId);

        final Device payload = new Device();
        payload.setEnabled(false);

        client.updateDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId, payload);

        final Device result = client.getDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId);

        // as we set it explicitly, we expect the explicit value of "false"
        assertEquals(Boolean.FALSE, result.getEnabled());

        client.deleteDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId);
        client.getDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId, HTTP_NOT_FOUND);
    }

    @Test
    void testDeviceCredentials() throws Exception {

        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {

            client.registerDevice(sharedIoTResourceManager.getTenantID(), randomDeviceId);

            String authId = "sensor-" + UUID.randomUUID().toString();
            String password = "password1234";
            credentialsClient.addCredentials(sharedIoTResourceManager.getTenantID(), randomDeviceId, authId, password);

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, getSharedIoTProject());

            credentialsClient.deleteAllCredentials(sharedIoTResourceManager.getTenantID(), randomDeviceId);

            client.deleteDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId);
            client.getDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId, HTTP_NOT_FOUND);

        }
    }

    @Disabled("Caches expire a bit unpredictably")
    @Test
    void testCacheExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {

            final Duration cacheExpiration = Duration.ofMinutes(3);

            // register device

            client.registerDevice(sharedIoTResourceManager.getTenantID(), randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final String password = "password1234";
            credentialsClient.addCredentials(sharedIoTResourceManager.getTenantID(), randomDeviceId, authId, password);

            // first test, cache filled

            IoTUtils.checkCredentials(authId, password, false, httpAdapterEndpoint, amqpClient, getSharedIoTProject());

            // set new password

            final String newPassword = "new-password1234";
            credentialsClient.updateCredentials(sharedIoTResourceManager.getTenantID(), randomDeviceId, authId, newPassword, null);

            // expect failure due to cached info

        IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, getSharedIoTProject());
            LOGGER.info("Waiting {} seconds for credentials to expire", cacheExpiration);
            Thread.sleep(cacheExpiration.toMillis());

            // cache must be expired, new password can be used

            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, getSharedIoTProject());

            credentialsClient.deleteAllCredentials(sharedIoTResourceManager.getTenantID(), randomDeviceId);

            client.deleteDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId);
            client.getDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId, HTTP_NOT_FOUND);
        }
    }

    @Test
    void testSetExpiryForCredentials() throws Exception {
        try (var credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint)) {
            client.registerDevice(sharedIoTResourceManager.getTenantID(), randomDeviceId);

            final String authId = UUID.randomUUID().toString();
            final Duration expiry = Duration.ofSeconds(30);
            final Instant notAfter = Instant.now().plus(expiry);
            final String newPassword = "password1234";

            credentialsClient.addCredentials(sharedIoTResourceManager.getTenantID(), randomDeviceId, authId, newPassword, notAfter);

            // first check, must succeed

            IoTUtils.checkCredentials(authId, newPassword, false, httpAdapterEndpoint, amqpClient, getSharedIoTProject());

            LOGGER.info("Waiting {} for credentials to expire", expiry);
            Thread.sleep(expiry.toMillis());

            // second check, after expiration, must fail

IoTUtils.checkCredentials(authId, newPassword, true, httpAdapterEndpoint, amqpClient, getSharedIoTProject());

            credentialsClient.deleteAllCredentials(sharedIoTResourceManager.getTenantID(), randomDeviceId);

            client.deleteDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId);
            client.getDeviceRegistration(sharedIoTResourceManager.getTenantID(), randomDeviceId, HTTP_NOT_FOUND);
        }
    }

    @Test
    void testCreateForNonExistingTenantFails() throws Exception {
        var response = client.registerDeviceWithResponse("invalid-" + sharedIoTResourceManager.getTenantID(), randomDeviceId);
        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }


}