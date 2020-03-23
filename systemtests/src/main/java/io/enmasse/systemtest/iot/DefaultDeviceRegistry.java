/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.iot.model.v1.DeviceRegistryServiceConfig;
import io.enmasse.iot.model.v1.DeviceRegistryServiceConfigBuilder;
import io.enmasse.iot.model.v1.ExternalInfinispanServer;
import io.enmasse.iot.model.v1.ExternalInfinispanServerBuilder;
import io.enmasse.iot.model.v1.ExternalJdbcServer;
import io.enmasse.iot.model.v1.ExternalJdbcServerBuilder;
import io.enmasse.iot.model.v1.Mode;
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

    public static ExternalJdbcServer externalPostgres(final Endpoint jdbcEndpoint, final Mode mode) {
        var builder = new ExternalJdbcServerBuilder()
                .withUrl(String.format("jdbc:postgresql://%s:%s/device-registry", jdbcEndpoint.getHost(), jdbcEndpoint.getPort()));

        // credentials aligned with 'templates/iot/examples/postgresql/deploy'
        builder = builder
                .withUsername("registry")
                .withPassword("user12");

        builder = builder
                .withNewDevices()
                .withMode(mode)
                .endDevices();

        return builder.build();
    }

    public static ExternalJdbcServer externalH2(final Endpoint jdbcEndpoint) {
        var builder = new ExternalJdbcServerBuilder()
                .withUrl(String.format("jdbc:h2:tcp://%s:%s//data/device-registry", jdbcEndpoint.getHost(), jdbcEndpoint.getPort()));

        // credentials aligned with 'templates/iot/examples/h2/deploy'
        builder = builder
                .withUsername("registry")
                .withPassword("user12");

        builder = builder
                .withNewDevices()
                .withMode(Mode.TABLE)
                .endDevices()

                .addNewExtension()
                .withNewContainer()
                .withName("ext-add-h2-driver")
                .withImage("quay.io/enmasse/h2-extension:1.4.200-1")
                .withImagePullPolicy("IfNotPresent")
                .addNewVolumeMount()
                .withName("extensions")
                .withMountPath("/ext")
                .endVolumeMount()
                .endContainer()

                .endExtension();

        return builder.build();
    }

    /**
     * Get instance for the default type of registry.
     *
     * @return A new configuration, for a storage which is already deployed.
     * @throws Exception In case the deployment of the backend failed.
     */
    public static DeviceRegistryServiceConfig newDefaultInstance() throws Exception {
        // align with deleteDefaultServer
        return newPostgresTreeBased();
    }

    /**
     * Delete the server which got created by {@link #newDefaultInstance()}.
     */
    public static void deleteDefaultServer() throws Exception {
        // align with newDefaultInstance
        SystemtestsKubernetesApps.deletePostgresqlServer();
    }

    public static DeviceRegistryServiceConfig newInfinispanBased() throws Exception {
        var infinispanEndpoint = SystemtestsKubernetesApps.deployInfinispanServer();
        return new DeviceRegistryServiceConfigBuilder()
                .withNewInfinispan()
                .withNewServer()

                .withExternal(externalServer(infinispanEndpoint))

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

    public static DeviceRegistryServiceConfig newPostgresFlatBased() throws Exception {
        var jdbcEndpoint = SystemtestsKubernetesApps.deployPostgresqlServer(Mode.JSON_FLAT);
        return new DeviceRegistryServiceConfigBuilder()
                .withNewJdbc()
                .withNewServer()

                .withExternal(externalPostgres(jdbcEndpoint, Mode.JSON_FLAT))

                .endServer()
                .endJdbc()
                .build();
    }

    public static DeviceRegistryServiceConfig newPostgresTreeBased() throws Exception {
        var jdbcEndpoint = SystemtestsKubernetesApps.deployPostgresqlServer(Mode.JSON_TREE);
        return new DeviceRegistryServiceConfigBuilder()
                .withNewJdbc()
                .withNewServer()

                .withExternal(externalPostgres(jdbcEndpoint, Mode.JSON_TREE))

                .endServer()
                .endJdbc()
                .build();
    }

    public static DeviceRegistryServiceConfig newH2Based() throws Exception {
        var jdbcEndpoint = SystemtestsKubernetesApps.deployH2Server();
        return new DeviceRegistryServiceConfigBuilder()
                .withNewJdbc()
                .withNewServer()

                .withExternal(externalH2(jdbcEndpoint))

                .endServer()
                .endJdbc()
                .build();
    }

}
