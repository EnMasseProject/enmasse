/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import static io.enmasse.systemtest.TestTag.sharedIot;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.IoTTestBase;

@Tag(sharedIot)
class IoTConfigTest extends IoTTestBase implements ITestBaseStandard {

    @Test
    void simpleTest() throws Exception {
        createIoTConfig(new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .endMetadata()
                .withNewSpec()
                .withEnableDefaultRoutes(false)
                .endSpec()
                .build());
    }
}
