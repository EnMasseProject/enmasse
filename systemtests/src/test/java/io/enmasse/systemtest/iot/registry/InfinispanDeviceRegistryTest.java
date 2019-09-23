/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.registry;

import static io.enmasse.systemtest.bases.DefaultDeviceRegistry.newInfinispanBased;

import org.junit.jupiter.api.Test;

import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;

public class InfinispanDeviceRegistryTest extends DeviceRegistryTestBase {

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

    @Override
    protected void removeIoTConfig() throws Exception {
        super.removeIoTConfig();
        SystemtestsKubernetesApps.deleteInfinispanServer(kubernetes.getInfraNamespace());
    }

    @Test
    public void testCorrectTypeDeployed () {
        assertCorrectRegistryType("infinispan");
    }

}
