/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.registry;

import io.enmasse.iot.model.v1.IoTConfigBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.newFileBased;
import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.newInfinispanBased;
import static io.enmasse.systemtest.utils.IoTUtils.assertCorrectRegistryType;

class FileDeviceRegistryTest extends DeviceRegistryTest {

    @Override
    protected IoTConfigBuilder provideIoTConfig() throws Exception {
        return new IoTConfigBuilder()
                .withNewSpec()
                .withNewServices()
                .withDeviceRegistry(newFileBased())
                .endServices()
                .withNewAdapters()
                .withNewMqtt()
                .endMqtt()
                .endAdapters()
                .endSpec();
    }

    @Test
    void testCorrectTypeDeployed () {
        assertCorrectRegistryType("file");
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
    @Disabled("Caches expire a bit unpredictably")
    void testCacheExpiryForCredentials() throws Exception {
        super.doTestCacheExpiryForCredentials();
    }

    @Test
    void testSetExpiryForCredentials() throws Exception {
        super.doTestSetExpiryForCredentials();
    }

    @Test
    @Disabled
    void testCreateForNonExistingTenantFails() throws Exception {
        super.doTestCreateForNonExistingTenantFails();
    }
}
