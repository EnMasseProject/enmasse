/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import java.util.Optional;

import io.enmasse.iot.model.v1.DeviceRegistryServiceConfig;
import io.enmasse.iot.model.v1.DeviceRegistryServiceConfigBuilder;
import io.enmasse.iot.model.v1.ExternalInfinispanServer;
import io.enmasse.iot.model.v1.ExternalInfinispanServerBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;

public final class DefaultDeviceRegistry {

    public static enum InfinispanVersion {
        V9, V10,
    }

    private DefaultDeviceRegistry() {}

    public static ExternalInfinispanServer externalServer(final InfinispanVersion version, final Endpoint infinispanEndpoint) {
        var builder = new ExternalInfinispanServerBuilder()
                .withHost(infinispanEndpoint.getHost())
                .withPort(infinispanEndpoint.getPort());

        // credentials aligned with 'templates/iot/examples/infinispan/manual'
        builder = builder.withUsername("app")
                .withPassword("test12");

        switch (version) {
            case V10:
                builder = builder.withSaslRealm("default")
                        .withSaslServerName("infinispan");
            default:
                builder = builder.withSaslRealm("ApplicationRealm")
                        .withSaslServerName("hotrod");
                break;
        }

        return builder.build();
    }

    public static DeviceRegistryServiceConfig newInfinispanBased() throws Exception {
        var infinispanEndpoint = SystemtestsKubernetesApps.deployInfinispanServer();
        return new DeviceRegistryServiceConfigBuilder()
                .withNewInfinispan()
                .withNewServer()

                .withExternal(externalServer(version(), infinispanEndpoint))

                .endServer()
                .endInfinispan()
                .build();
    }

    /**
     * Get the version to use for infinispan.
     * @return The version to use, never returns {@code null}.
     * @throws IllegalArgumentException If a value is set, but cannot be mapped to an enum literal.
     */
    private static InfinispanVersion version() {
        return Optional
                .ofNullable(System.getenv("INFINISPAN_VERSION"))
                .map(InfinispanVersion::valueOf)
                .orElse(InfinispanVersion.V10);
    }

    public static DeviceRegistryServiceConfig newFileBased() {
        return new DeviceRegistryServiceConfigBuilder()
                .withNewFile()
                .withNumberOfDevicesPerTenant(100_000)
                .endFile()
                .build();
    }

}
