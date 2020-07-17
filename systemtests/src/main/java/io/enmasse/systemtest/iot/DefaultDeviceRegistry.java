/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.iot.model.v1.DeviceConnectionServiceConfig;
import io.enmasse.iot.model.v1.DeviceConnectionServiceConfigBuilder;
import io.enmasse.iot.model.v1.DeviceRegistryServiceConfig;
import io.enmasse.iot.model.v1.DeviceRegistryServiceConfigBuilder;
import io.enmasse.iot.model.v1.ExternalInfinispanDeviceConnectionServer;
import io.enmasse.iot.model.v1.ExternalInfinispanDeviceConnectionServerBuilder;
import io.enmasse.iot.model.v1.ExternalInfinispanDeviceRegistryServer;
import io.enmasse.iot.model.v1.ExternalInfinispanDeviceRegistryServerBuilder;
import io.enmasse.iot.model.v1.ExternalJdbcDeviceConnectionServer;
import io.enmasse.iot.model.v1.ExternalJdbcDeviceConnectionServerBuilder;
import io.enmasse.iot.model.v1.ExternalJdbcRegistryServer;
import io.enmasse.iot.model.v1.ExternalJdbcRegistryServerBuilder;
import io.enmasse.iot.model.v1.ServicesConfig;
import io.enmasse.iot.model.v1.ServicesConfigBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.enmasse.systemtest.condition.OpenShiftVersion.OCP4;

public final class DefaultDeviceRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultDeviceRegistry.class);

    private DefaultDeviceRegistry() {}

    public static ExternalInfinispanDeviceConnectionServer externalInfinispanConnectionServer(final Endpoint infinispanEndpoint) {
        var builder = new ExternalInfinispanDeviceConnectionServerBuilder()
                .withNewServer()
                .withHost(infinispanEndpoint.getHost())
                .withPort(infinispanEndpoint.getPort())
                .endServer();

        // credentials aligned with 'templates/iot/examples/infinispan/manual'
        builder = builder
                .editOrNewServer()
                .withUsername("app")
                .withPassword("test12")
                .withSaslRealm("ApplicationRealm")
                .withSaslServerName("hotrod")
                .endServer();

        return builder.build();
    }

    public static ExternalInfinispanDeviceRegistryServer externalInfinispanRegistryServer(final Endpoint infinispanEndpoint) {
        var builder = new ExternalInfinispanDeviceRegistryServerBuilder()
                .withNewServer()
                .withHost(infinispanEndpoint.getHost())
                .withPort(infinispanEndpoint.getPort())
                .endServer();

        // credentials aligned with 'templates/iot/examples/infinispan/manual'
        builder = builder
                .editOrNewServer()
                .withUsername("app")
                .withPassword("test12")
                .withSaslRealm("ApplicationRealm")
                .withSaslServerName("hotrod")
                .endServer();

        return builder.build();
    }

    public static ExternalJdbcRegistryServer externalPostgresRegistryServer(final Endpoint jdbcEndpoint, final boolean split) {
        var builder = new ExternalJdbcRegistryServerBuilder()
                .withNewManagement()
                .withNewConnection()
                .withUrl(String.format("jdbc:postgresql://%s:%s/device-registry", jdbcEndpoint.getHost(), jdbcEndpoint.getPort()))
                .endConnection()
                .endManagement();

        // credentials aligned with 'templates/iot/examples/postgresql/deploy'
        builder = builder
                .editOrNewManagement()
                .editOrNewConnection()
                .withUsername("registry")
                .withPassword("user12")
                .endConnection()
                .endManagement();

        if (split) {
            builder = builder.withNewAdapter()
                    .withConnection(builder.build().getManagement().getConnection())
                    .endAdapter();
        }

        return builder.build();
    }

    public static ExternalJdbcRegistryServer externalH2RegistryServer(final Endpoint jdbcEndpoint) {
        var builder = new ExternalJdbcRegistryServerBuilder()
                .withNewManagement()
                .withNewConnection()
                .withUrl(String.format("jdbc:h2:tcp://%s:%s//data/device-registry", jdbcEndpoint.getHost(), jdbcEndpoint.getPort()))
                .endConnection()
                .endManagement();

        // credentials aligned with 'templates/iot/examples/h2/deploy'
        builder = builder
                .editOrNewManagement()
                .editOrNewConnection()
                .withUsername("registry")
                .withPassword("user12")
                .endConnection()
                .endManagement();

        builder = builder
                .addNewExtension()
                .withNewContainer()
                .withName("ext-add-h2-driver")
                .withImage("quay.io/enmasse/h2-extension:1.4.200-3")
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
    public static ServicesConfig newDefaultInstance() throws Exception {
        return newMixed();
    }

    /**
     * Delete the server which got created by {@link #newDefaultInstance()}.
     */
    public static void deleteDefaultServer() throws Exception {

        log.info("Deleting mixed mode device registry");

        // align with newDefaultInstance
        SystemtestsKubernetesApps.deletePostgresqlServer();
        SystemtestsKubernetesApps.deleteInfinispanServer();

    }

    public static DeviceRegistryServiceConfigBuilder applyDeviceRegistryEndpoint(final DeviceRegistryServiceConfigBuilder registry) {

        if (!Kubernetes.isOpenShiftCompatible(OCP4)) {
            return registry
                    .editOrNewManagement()
                    .withNewEndpoint()
                    .withNewSecretNameStrategy("systemtests-iot-device-registry-tls")
                    .endEndpoint()
                    .endManagement();
        } else {
            return registry;
        }

    }

    public static ServicesConfig newPostgresBased(final boolean split) throws Exception {
        var jdbcEndpoint = SystemtestsKubernetesApps.deployPostgresqlServer();

        return new ServicesConfigBuilder()
                .withDeviceConnection(newPostgresBasedConnection(jdbcEndpoint))
                .withDeviceRegistry(newPostgresBasedRegistry(jdbcEndpoint, split))
                .build();
    }

    public static ServicesConfig newH2Based() throws Exception {
        var jdbcEndpoint = SystemtestsKubernetesApps.deployH2Server();

        return new ServicesConfigBuilder()
                .withDeviceConnection(newH2BasedConnection(jdbcEndpoint))
                .withDeviceRegistry(newH2BasedRegistry(jdbcEndpoint))
                .build();
    }

    public static ServicesConfig newInfinispanBased() throws Exception {
        var infinispanEndpoint = SystemtestsKubernetesApps.deployInfinispanServer();

        return new ServicesConfigBuilder()
                .withDeviceConnection(newInfinispanDeviceConnectionService(infinispanEndpoint))
                .withDeviceRegistry(newInfinispanDeviceRegistryService(infinispanEndpoint))
                .build();
    }

    public static ServicesConfig newMixed() throws Exception {
        log.info("Installing mixed mode device registry");

        var jdbcEndpoint = SystemtestsKubernetesApps.deployPostgresqlServer();
        var infinispanEndpoint = SystemtestsKubernetesApps.deployInfinispanServer();

        return new ServicesConfigBuilder()
                .withDeviceConnection(DefaultDeviceRegistry.newInfinispanDeviceConnectionService(infinispanEndpoint))
                .withDeviceRegistry(newPostgresBasedRegistry(jdbcEndpoint, false))
                .build();
    }

    public static DeviceConnectionServiceConfig newInfinispanDeviceConnectionService(final Endpoint infinispanEndpoint) {
        return new DeviceConnectionServiceConfigBuilder()
                .withNewInfinispan()
                .withNewServer()

                .withExternal(externalInfinispanConnectionServer(infinispanEndpoint))

                .endServer()
                .endInfinispan()
                .build();
    }

    public static DeviceRegistryServiceConfig newInfinispanDeviceRegistryService(final Endpoint infinispanEndpoint) {

        var builder = new DeviceRegistryServiceConfigBuilder()
                .withNewInfinispan()
                .withNewServer()

                .withExternal(externalInfinispanRegistryServer(infinispanEndpoint))

                .endServer()
                .endInfinispan();

        return applyDeviceRegistryEndpoint(builder).build();

    }

    public static DeviceRegistryServiceConfig newPostgresBasedRegistry(final Endpoint jdbcEndpoint, final boolean split) throws Exception {

        var builder = new DeviceRegistryServiceConfigBuilder()
                .withNewJdbc()
                .withNewServer()

                .withExternal(externalPostgresRegistryServer(jdbcEndpoint, split))

                .endServer()
                .endJdbc();

        return applyDeviceRegistryEndpoint(builder).build();

    }

    public static DeviceRegistryServiceConfig newH2BasedRegistry(final Endpoint jdbcEndpoint) throws Exception {

        var builder = new DeviceRegistryServiceConfigBuilder()
                .withNewJdbc()
                .withNewServer()

                .withExternal(externalH2RegistryServer(jdbcEndpoint))

                .endServer()
                .endJdbc();

        return applyDeviceRegistryEndpoint(builder).build();
    }

    private static ExternalJdbcDeviceConnectionServer externalPostgresConnectionServer(final Endpoint jdbcEndpoint) {
        var builder = new ExternalJdbcDeviceConnectionServerBuilder()
                .withUrl(String.format("jdbc:postgresql://%s:%s/device-registry", jdbcEndpoint.getHost(), jdbcEndpoint.getPort()));

        // credentials aligned with 'templates/iot/examples/postgresql/deploy'
        builder = builder
                .withUsername("registry")
                .withPassword("user12");

        return builder.build();
    }

    private static ExternalJdbcDeviceConnectionServer externalH2ConnectionServer(final Endpoint jdbcEndpoint) {
        var builder = new ExternalJdbcDeviceConnectionServerBuilder()
                .withUrl(String.format("jdbc:h2:tcp://%s:%s//data/device-registry", jdbcEndpoint.getHost(), jdbcEndpoint.getPort()));

        // credentials aligned with 'templates/iot/examples/h2/deploy'
        builder = builder
                .withUsername("registry")
                .withPassword("user12");

        builder = builder

                .addNewExtension()
                .withNewContainer()
                .withName("ext-add-h2-driver")
                .withImage("quay.io/enmasse/h2-extension:1.4.200-3")
                .withImagePullPolicy("IfNotPresent")
                .addNewVolumeMount()
                .withName("extensions")
                .withMountPath("/ext")
                .endVolumeMount()
                .endContainer()

                .endExtension();

        return builder.build();
    }

    public static DeviceConnectionServiceConfig newPostgresBasedConnection(final Endpoint jdbcEndpoint) throws Exception {
        return new DeviceConnectionServiceConfigBuilder()
                .withNewJdbc()
                .withNewServer()

                .withExternal(externalPostgresConnectionServer(jdbcEndpoint))

                .endServer()
                .endJdbc()
                .build();
    }

    public static DeviceConnectionServiceConfig newH2BasedConnection(final Endpoint jdbcEndpoint) throws Exception {
        return new DeviceConnectionServiceConfigBuilder()
                .withNewJdbc()
                .withNewServer()

                .withExternal(externalH2ConnectionServer(jdbcEndpoint))

                .endServer()
                .endJdbc()
                .build();
    }

}
