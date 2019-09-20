/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.iot.model.v1.DeviceRegistryServiceConfig;
import io.enmasse.iot.model.v1.DeviceRegistryServiceConfigBuilder;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;

public final class DefaultDeviceRegistry {

    private static final Kubernetes kubernetes = Kubernetes.getInstance();

    private DefaultDeviceRegistry() {
    }

    public static DeviceRegistryServiceConfig newInfinispanBased() throws Exception {
        var infinispanEndpoint = SystemtestsKubernetesApps.deployInfinispanServer(kubernetes.getInfraNamespace());
        return new DeviceRegistryServiceConfigBuilder()
                .withNewInfinispan()
                .withNewServer()

                .withNewExternal()
                .withHost(infinispanEndpoint.getHost())
                .withPort(infinispanEndpoint.getPort())
                // credentials aligned with 'templates/iot/examples/infinispan/manual'
                .withUsername("app")
                .withPassword("test12")
                .withSaslRealm("ApplicationRealm")
                .withSaslServerName("hotrod")
                .endExternal()

                .endServer()
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

    /**
     * Create default device registry setup.
     *
     * @return A new instance of a device registry configuration.
     * @throws Exception in case anything goes wrong.
     */
    public static DeviceRegistryServiceConfig deviceRegistry() throws Exception {
        var r = Environment.getInstance().getDefaultDeviceRegistry();

        switch (r) {
            case "file":
                return newFileBased();
            case "infinispan":
                return newInfinispanBased();
            default:
                throw new IllegalArgumentException(String.format("Device registry type '%s' unknown", r));
        }

    }

}
