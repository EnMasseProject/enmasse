/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.registry;

import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTShared;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.INFINISPAN_REG;
import static io.enmasse.systemtest.TestTag.IOT_DEVICE_REG;
import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.newFileBased;
import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.newInfinispanBased;
import static io.enmasse.systemtest.utils.IoTUtils.assertCorrectRegistryType;

class InfinispanDeviceRegistryTest extends DeviceRegistryTest {

    @Override
    protected IoTConfigBuilder provideIoTConfig() throws Exception {
        return new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .endMetadata()
                .withNewSpec()
                .withNewServices()
                .withDeviceRegistry(newInfinispanBased())
                .endServices()
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
}
