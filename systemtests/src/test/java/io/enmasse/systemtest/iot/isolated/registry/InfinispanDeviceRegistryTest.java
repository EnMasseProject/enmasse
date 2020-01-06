/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.registry;

import io.enmasse.iot.model.v1.IoTConfigBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.newInfinispanBased;
import static io.enmasse.systemtest.utils.IoTUtils.assertCorrectRegistryType;

import java.net.HttpURLConnection;

class InfinispanDeviceRegistryTest extends DeviceRegistryTest {

    static {
        TENANT_DOESNT_EXISTS_CODE = HttpURLConnection.HTTP_UNAUTHORIZED;
    }

    @Override
    protected IoTConfigBuilder provideIoTConfig() throws Exception {
        return new IoTConfigBuilder()
                .withNewSpec()
                .withNewServices()
                .withDeviceRegistry(newInfinispanBased())
                .endServices()
                .withNewAdapters()
                .withNewMqtt()
                .endMqtt()
                .endAdapters()
                .endSpec();
    }

    @Test
    void testCorrectTypeDeployed () {
        assertCorrectRegistryType("infinispan");
    }

    @Test
    void testRegisterDevice() throws Exception {
        super.doTestRegisterDevice();
    }

    @Test
    void testDisableDevice() throws Exception {
        super.doTestDisableDevice();
    }

    @Test
    void testDeviceCredentials() throws Exception {
        super.doTestDeviceCredentials();
    }

    @Test
    void testDeviceCredentialsPlainPassword() throws Exception {
        super.doTestDeviceCredentialsPlainPassword();
    }

    @Test
    @Disabled("Fixed in hono/pull/1565")
    void testDeviceCredentialsDoesNotContainsPasswordDetails() throws Exception {
        super.doTestDeviceCredentialsDoesNotContainsPasswordDetails();
    }

    @Test
    @Disabled("Caches expire a bit unpredictably")
    void testCacheExpiryForCredentials() throws Exception {
        super.doTestCacheExpiryForCredentials();
    }

    @Test
    void testSetExpiryForCredentials() throws Exception {
        super.doTestSetExpiryForCredentials();
    }

    @Test
    void testCreateForNonExistingTenantFails() throws Exception {
        super.doTestCreateForNonExistingTenantFails();
    }

    @Test
    void testCreateDuplicateDeviceFails() throws Exception {
        super.doCreateDuplicateDeviceFails();
    }

    @Test
    void testTenantDeletionTriggersDevicesDeletion() throws Exception {
        super.doTestTenantDeletionTriggersDevicesDeletion();
    }
}
