/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.shared.registry;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.IOT_DEVICE_REG;
import static io.enmasse.systemtest.utils.IoTUtils.assertCorrectRegistryType;

@Tag(IOT_DEVICE_REG)
public class FileDeviceRegistryTest extends DeviceRegistryTest {
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
