/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.registry;

import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.newInfinispanBased;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.CertificateUtils;

public class InfinispanDeviceRegistryTest {
    @Test
    public void testCorrectTypeDeployed () {
        assertCorrectRegistryType("infinispan");
    }

}
