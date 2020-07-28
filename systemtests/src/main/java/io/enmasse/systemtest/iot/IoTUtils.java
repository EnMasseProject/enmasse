/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import io.enmasse.iot.model.v1.AdapterConfig;
import io.enmasse.iot.model.v1.AdaptersConfig;
import io.enmasse.iot.model.v1.DoneableIoTConfig;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigSpec;
import io.enmasse.iot.model.v1.IoTConfigStatus;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectStatus;
import io.enmasse.iot.model.v1.MeshConfig;
import io.enmasse.iot.model.v1.ServiceConfig;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.framework.condition.OpenShiftVersion;
import io.enmasse.systemtest.framework.LoggerUtils;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.Serialization;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.enmasse.systemtest.platform.Kubernetes.getClient;
import static io.enmasse.systemtest.platform.Kubernetes.getInstance;
import static io.enmasse.systemtest.platform.Kubernetes.iotConfigs;
import static io.enmasse.systemtest.platform.Kubernetes.iotTenants;
import static io.enmasse.systemtest.time.TimeMeasuringSystem.Operation.startOperation;
import static io.enmasse.systemtest.utils.Serialization.toJson;
import static io.enmasse.systemtest.utils.TestUtils.TimeoutHandler.explain;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IoTUtils {

    private static final String IOT_AMQP_ADAPTER = "iot-amqp-adapter";
    private static final String IOT_SIGFOX_ADAPTER = "iot-sigfox-adapter";
    private static final String IOT_MQTT_ADAPTER = "iot-mqtt-adapter";
    private static final String IOT_LORAWAN_ADAPTER = "iot-lorawan-adapter";
    private static final String IOT_HTTP_ADAPTER = "iot-http-adapter";
    private static final String IOT_AUTH_SERVICE = "iot-auth-service";
    private static final String IOT_DEVICE_REGISTRY = "iot-device-registry";
    private static final String IOT_DEVICE_REGISTRY_MANAGEMENT = "iot-device-registry-management";
    private static final String IOT_DEVICE_CONNECTION = "iot-device-connection";
    private static final String IOT_TENANT_SERVICE = "iot-tenant-service";
    private static final String IOT_SERVICE_MESH = "iot-service-mesh";

    private static final Map<String, String> IOT_LABELS = Map.of("component", "iot");
    private static final Logger log = LoggerUtils.getLogger();

    // check if a deployment of stateful set is ready
    private static final Condition<Object> ready = new Condition<>(
            o -> ofNullable(o)
                    .map(JsonObject::mapFrom)
                    .map(json -> json.getJsonObject("status"))
                    .map(json -> json.getInteger("readyReplicas", -1) > 0)
                    .orElse(false),
            "ready");

    public static void assertIoTConfigGone(final IoTConfig config, final SoftAssertions softly) {

        var iotPods = getInstance().listPods(config.getMetadata().getNamespace(), IOT_LABELS, Collections.emptyMap());

        softly.assertThat(iotPods)
                .isEmpty();

    }

    public static void assertIoTConfigReady(final IoTConfig config, final SoftAssertions softly) {

        // gather expected deployments

        final String[] expectedDeployments = getExpectedDeploymentsNames(config);
        Arrays.sort(expectedDeployments);
        final String[] expectedStatefulSets = new String[]{IOT_SERVICE_MESH};
        Arrays.sort(expectedStatefulSets);

        /*
         * We are no longer waiting here for anything. The IoT configuration told us it is ready and so we only
         * check for that. This is different than it was in the past, where we had to wait for the individual
         * components of the IoT infrastructure, because the IoTConfig did not track the readiness state of its
         * components.
         */

        // assert deployments

        var deployments = getInstance().listDeployments(IOT_LABELS);
        softly.assertThat(deployments)
                .are(ready)
                .extracting("metadata.name", String.class)
                .containsExactly(expectedDeployments);

        // assert stateful sets

        var statefulSets = getInstance().listStatefulSets(IOT_LABELS);
        softly.assertThat(statefulSets)
                .are(ready)
                .extracting("metadata.name", String.class)
                .containsExactly(expectedStatefulSets);

        // assert number of replicas for command mesh

        var meshStatefulSet = getClient()
                .apps().statefulSets()
                .inNamespace(config.getMetadata().getNamespace())
                .withName(IOT_SERVICE_MESH)
                .get();

        var meshReplicas = Optional.of(config)
                .map(IoTConfig::getSpec)
                .map(IoTConfigSpec::getMesh)
                .map(MeshConfig::getServiceConfig)
                .map(ServiceConfig::getReplicas)
                .orElse(1);

        softly.assertThat(meshStatefulSet.getStatus().getReplicas())
                .isEqualTo(meshReplicas);

    }

    private static String[] getExpectedDeploymentsNames(IoTConfig config) {

        final Collection<String> expectedDeployments = new ArrayList<>();

        // protocol adapters

        addIfEnabled(expectedDeployments, config, AdaptersConfig::getAmqp, IOT_AMQP_ADAPTER);
        addIfEnabled(expectedDeployments, config, AdaptersConfig::getHttp, IOT_HTTP_ADAPTER);
        addIfEnabled(expectedDeployments, config, AdaptersConfig::getLoraWan, IOT_LORAWAN_ADAPTER);
        addIfEnabled(expectedDeployments, config, AdaptersConfig::getMqtt, IOT_MQTT_ADAPTER);
        addIfEnabled(expectedDeployments, config, AdaptersConfig::getSigfox, IOT_SIGFOX_ADAPTER);

        // device registry

        expectedDeployments.add(IOT_DEVICE_REGISTRY);

        if (config.getSpec().getServices() != null &&
                config.getSpec().getServices().getDeviceRegistry() != null &&
                config.getSpec().getServices().getDeviceRegistry().getJdbc() != null &&
                config.getSpec().getServices().getDeviceRegistry().getJdbc().getCommonDeviceRegistry() != null &&
                !config.getSpec().getServices().getDeviceRegistry().getJdbc().getCommonDeviceRegistry().isDisabled() &&
                config.getSpec().getServices().getDeviceRegistry().getJdbc().getServer() != null &&
                config.getSpec().getServices().getDeviceRegistry().getJdbc().getServer().getExternal() != null) {

            var external = config.getSpec().getServices().getDeviceRegistry().getJdbc().getServer().getExternal();
            if (external.getManagement() != null && external.getAdapter() != null) {
                expectedDeployments.add(IOT_DEVICE_REGISTRY_MANAGEMENT);
            }
        }

        // common services

        expectedDeployments.addAll(Arrays.asList(IOT_AUTH_SERVICE, IOT_DEVICE_CONNECTION, IOT_TENANT_SERVICE));

        return expectedDeployments.toArray(String[]::new);
    }

    private static void addIfEnabled(Collection<String> adapters, IoTConfig config, Function<AdaptersConfig, AdapterConfig> adapterGetter, String name) {
        Optional<Boolean> enabled = ofNullable(config.getSpec().getAdapters()).map(adapterGetter).map(AdapterConfig::getEnabled);
        if (enabled.orElse(true)) {
            adapters.add(name);
            log.info("{} is enabled", name);
        } else {
            log.info("{} is disabled", name);
        }
    }

    public static String getTenantId(IoTProject project) {
        return String.format("%s.%s", project.getMetadata().getNamespace(), project.getMetadata().getName());
    }

    public static void assertCorrectDeviceConnectionType(final String type) {
        final Deployment deployment =
                getClient().apps().deployments().inNamespace(getInstance().getInfraNamespace()).withName("iot-device-connection").get();
        assertNotNull(deployment);
        assertEquals(type, deployment.getMetadata().getAnnotations().get("iot.enmasse.io/deviceConnection.type"));
    }

    public static void assertCorrectRegistryType(final String type) {
        final Deployment deployment =
                getClient().apps().deployments().inNamespace(getInstance().getInfraNamespace()).withName("iot-device-registry").get();
        assertNotNull(deployment);
        assertEquals(type, deployment.getMetadata().getAnnotations().get("iot.enmasse.io/registry.type"));
    }

    public static Endpoint getDeviceRegistryManagementEndpoint() {
        if (Kubernetes.isOpenShiftCompatible(OpenShiftVersion.OCP4)) {
            // openshift router
            return getInstance().getExternalEndpoint("device-registry");
        } else {
            // load balancer service
            return getInstance().getExternalEndpoint("iot-device-registry-external");
        }
    }
}
