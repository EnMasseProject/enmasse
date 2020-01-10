/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.iot.model.v1.DeviceRegistryServiceConfig;
import io.enmasse.iot.model.v1.DeviceRegistryServiceConfigBuilder;
import io.enmasse.iot.model.v1.ExternalInfinispanServer;
import io.enmasse.iot.model.v1.ExternalInfinispanServerBuilder;
import io.enmasse.iot.model.v1.JavaContainerOptions;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;

public final class DefaultDeviceRegistry {

    private DefaultDeviceRegistry() {}

    public static ExternalInfinispanServer externalServer(final Endpoint infinispanEndpoint) {
        var builder = new ExternalInfinispanServerBuilder()
                .withHost(infinispanEndpoint.getHost())
                .withPort(infinispanEndpoint.getPort());

        // credentials aligned with 'templates/iot/examples/infinispan/manual'
        builder = builder
                .withUsername("app")
                .withPassword("test12")
                .withSaslRealm("ApplicationRealm")
                .withSaslServerName("hotrod");

        return builder.build();
    }

    public static DeviceRegistryServiceConfig newInfinispanBased() throws Exception {
        var infinispanEndpoint = SystemtestsKubernetesApps.deployInfinispanServer();
        return new DeviceRegistryServiceConfigBuilder()
                .withNewInfinispan()
                .withNewServer()

                .withExternal(externalServer(infinispanEndpoint))

                .endServer()
                .withNewJava()
                .withDebug(true)
                .endJava()
                .endInfinispan()
                .build();
    }

    public static DeviceRegistryServiceConfig newFileBased() {
        return new DeviceRegistryServiceConfigBuilder()
                .withNewFile()
                .withNumberOfDevicesPerTenant(100_000)
                .endFile()
                .build();
    }

}
