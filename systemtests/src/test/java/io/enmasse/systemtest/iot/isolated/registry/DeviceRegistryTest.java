/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.registry;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.bases.iot.ITestIoTBase;
import io.enmasse.systemtest.bases.iot.ITestIoTShared;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.iot.IoTTestBase;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageSendTester.ConsumerFactory;
import io.enmasse.systemtest.iot.MessageSendTester.Type;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
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
public abstract class DeviceRegistryTest implements ITestIoTShared {

    protected DeviceRegistryClient client;

    @BeforeEach
    void setAttributes() {
        client = sharedIoTResourceManager.getDevClient();
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

            checkCredentials(authId, password, false);

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

            checkCredentials(authId, password, false);

            // set new password

            final String newPassword = "new-password1234";
            credentialsClient.updateCredentials(sharedIoTResourceManager.getTenantID(), randomDeviceId, authId, newPassword, null);

            // expect failure due to cached info

            checkCredentials(authId, newPassword, true);
            log.info("Waiting {} seconds for credentials to expire", cacheExpiration);
            Thread.sleep(cacheExpiration.toMillis());

            // cache must be expired, new password can be used

            checkCredentials(authId, newPassword, false);

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

            checkCredentials(authId, newPassword, false);

            log.info("Waiting {} for credentials to expire", expiry);
            Thread.sleep(expiry.toMillis());

            // second check, after expiration, must fail

            checkCredentials(authId, newPassword, true);

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




}
