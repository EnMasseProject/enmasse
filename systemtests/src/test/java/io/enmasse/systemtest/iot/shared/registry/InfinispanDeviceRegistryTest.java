/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.shared.registry;

import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTShared;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.utils.IoTUtils.assertCorrectRegistryType;

public class InfinispanDeviceRegistryTest extends TestBase implements ITestIoTShared {
    @Test
    public void testCorrectTypeDeployed () {
        assertCorrectRegistryType("infinispan");
    }

}
