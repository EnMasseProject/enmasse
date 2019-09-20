/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.shared.registry;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.utils.IoTUtils.assertCorrectRegistryType;

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
