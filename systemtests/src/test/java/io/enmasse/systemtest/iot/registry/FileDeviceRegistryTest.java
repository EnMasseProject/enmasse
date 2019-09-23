/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.registry;

import io.enmasse.iot.model.v1.IoTConfigBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.bases.DefaultDeviceRegistry.newFileBased;

public class FileDeviceRegistryTest extends DeviceRegistryTestBase {

    @Override
    protected IoTConfigBuilder provideIoTConfig() throws Exception {
        return new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .endMetadata()
                .withNewSpec()
                .withNewServices()
                .withDeviceRegistry(newFileBased())
                .endServices()
                .endSpec();
    }

    @Test
    public void testCorrectTypeDeployed () {
        assertCorrectRegistryType("file");
    }

    @Disabled("Not supported by file based device registry")
    @Override
    void testCreateForNonExistingTenantFails() throws Exception {
        super.testCreateForNonExistingTenantFails();
    }
}
