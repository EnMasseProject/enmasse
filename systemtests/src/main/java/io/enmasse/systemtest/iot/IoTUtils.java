/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import io.enmasse.iot.model.v1.AdapterConfig;
import io.enmasse.iot.model.v1.AdaptersConfig;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigSpec;
import io.enmasse.iot.model.v1.IoTConfigStatus;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectStatus;
import io.enmasse.iot.model.v1.MeshConfig;
import io.enmasse.iot.model.v1.ServiceConfig;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.framework.LoggerUtils;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.Serialization;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.Condition;
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

    static void waitForIoTConfigReady(IoTConfig config) throws Exception {
        var budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        var iotConfigAccess = iotConfigs(config.getMetadata().getNamespace()).withName(config.getMetadata().getName());
        TestUtils.waitUntilCondition(() -> {
                    var currentState = iotConfigAccess.get();
                    var currentPhase = ofNullable(currentState)
                            .map(IoTConfig::getStatus)
                            .map(IoTConfigStatus::getPhase)
                            .orElse(null);
                    if ("Active".equals(currentPhase)) {
                        log.info("IoTConfig is ready - phase: {} -> {}", currentPhase, toJson(false, currentState.getStatus()));
                        return true;
                    } else {
                        log.info("Waiting until IoTConfig: '{}' will be in ready state", config.getMetadata().getName());
                        return false;
                    }
                },
                budget.remaining(), Duration.ofSeconds(5),
                explain(() -> ofNullable(iotConfigAccess.get())
                        .map(IoTConfig::getStatus)
                        .map(Serialization::toYaml)
                        .orElse("IoTConfig has no status section")));

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

        var deployments = Kubernetes.getInstance().listDeployments(IOT_LABELS);
        assertThat(deployments)
                .are(ready)
                .extracting("metadata.name")
                .containsExactly(expectedDeployments);

        // assert stateful sets

        var statefulSets = Kubernetes.getInstance().listStatefulSets(IOT_LABELS);
        assertThat(statefulSets)
                .are(ready)
                .extracting("metadata.name")
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

        assertEquals(meshReplicas, meshStatefulSet.getStatus().getReplicas());

    }

    static void deleteIoTConfigAndWait(IoTConfig config) throws Exception {
        log.info("Deleting IoTConfig: {} in namespace: {}", config.getMetadata().getName(), config.getMetadata().getNamespace());
        try (var ignored = startOperation(SystemtestsOperation.DELETE_IOT_CONFIG)) {
            iotConfigs(config.getMetadata().getNamespace())
                    .withName(config.getMetadata().getName())
                    .withPropagationPolicy(DeletionPropagation.BACKGROUND)
                    .delete();
            TestUtils.waitForNReplicas(0, false, config.getMetadata().getNamespace(), IOT_LABELS, Collections.emptyMap(), new TimeoutBudget(5, TimeUnit.MINUTES), 5000);
        }
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

    private static void waitForIoTProjectReady(IoTProject project) {
        var budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        var tenantClient = iotTenants(project.getMetadata().getNamespace());
        var tenantAccess = tenantClient.withName(project.getMetadata().getName());

        TestUtils.waitUntilCondition(() -> {
                    var currentState = tenantAccess.get();
                    var currentPhase = ofNullable(currentState)
                            .map(IoTProject::getStatus)
                            .map(IoTProjectStatus::getPhase)
                            .orElse(null);
                    if ("Active".equals(currentPhase)) {
                        log.info("IoTProject is ready - phase: {} -> {}", currentPhase, toJson(false, currentState.getStatus()));
                        return true;
                    } else {
                        log.info("Waiting until IoTProject: '{}' will be in ready state -> {}", project.getMetadata().getName(), currentPhase);
                        return false;
                    }
                },
                budget.remaining(), Duration.ofSeconds(5),
                explain(() -> ofNullable(tenantAccess.get())
                        .map(IoTProject::getStatus)
                        .map(Serialization::toYaml)
                        .orElse("Project has no status section")));

        // refresh
        var actual = tenantAccess.get();

        // the tenant is ready, so we need to check a few things
        assertNotNull(actual.getStatus());
        assertEquals("Active", actual.getStatus().getPhase());
    }

    static void deleteIoTProjectAndWait(IoTProject project) {

        log.info("Deleting IoTProject: {}", project.getMetadata().getName());

        try (var ignored = startOperation(SystemtestsOperation.DELETE_IOT_TENANT)) {

            var tenantClient = iotTenants(project.getMetadata().getNamespace());

            // delete the IoTProject

            tenantClient
                    .withName(project.getMetadata().getName())
                    .withPropagationPolicy(DeletionPropagation.BACKGROUND)
                    .delete();

            // wait until the IoTProject is deleted

            var projectName = project.getMetadata().getNamespace() + "/" + project.getMetadata().getName();
            TestUtils.waitUntilConditionOrFail(() -> {
                var updated = tenantClient.withName(project.getMetadata().getName()).get();
                if (updated != null) {
                    log.info("IoTProject {} still exists -> {}", projectName, updated.getStatus().getPhase());
                }
                return updated == null;
            }, Duration.ofMinutes(5), Duration.ofSeconds(10), () -> "IoT project failed to delete in time");
            log.info("IoTProject {} deleted", projectName);

        }

    }

    static void createIoTConfig(IoTConfig config) throws Exception {
        var name = config.getMetadata().getName();
        log.info("Creating IoTConfig - name: {}", name);
        try (var ignored = startOperation(SystemtestsOperation.CREATE_IOT_CONFIG)) {
            var iotConfigApiClient = iotConfigs(config.getMetadata().getNamespace());
            if (iotConfigApiClient.withName(name).get() != null) {
                log.info("iot config {} already exists", name);
            } else {
                log.info("iot config {} will be created", name);
                iotConfigApiClient.create(config);
            }
            IoTUtils.waitForIoTConfigReady(config);
        }
    }

    static void createIoTProject(IoTProject project, boolean awaitReady) {
        var name = project.getMetadata().getName();
        log.info("Creating IoTProject - name: {}", name);
        try (var ignored = startOperation(SystemtestsOperation.CREATE_IOT_TENANT)) {
            var iotProjectApiClient = iotTenants(project.getMetadata().getNamespace());
            if (iotProjectApiClient.withName(name).get() != null) {
                log.info("iot project {} already exists", name);
            } else {
                log.info("iot project {} will be created", name);
                iotProjectApiClient.create(project);
            }
            if (awaitReady) {
                IoTUtils.waitForIoTProjectReady(project);
            }
        }
    }

    public static String getTenantId(IoTProject project) {
        return String.format("%s.%s", project.getMetadata().getNamespace(), project.getMetadata().getName());
    }

    public static void assertCorrectDeviceConnectionType(final String type) {
        final Deployment deployment =
                getClient().apps().deployments().inNamespace(Kubernetes.getInstance().getInfraNamespace()).withName("iot-device-connection").get();
        assertNotNull(deployment);
        assertEquals(type, deployment.getMetadata().getAnnotations().get("iot.enmasse.io/deviceConnection.type"));
    }

    public static void assertCorrectRegistryType(final String type) {
        final Deployment deployment =
                getClient().apps().deployments().inNamespace(Kubernetes.getInstance().getInfraNamespace()).withName("iot-device-registry").get();
        assertNotNull(deployment);
        assertEquals(type, deployment.getMetadata().getAnnotations().get("iot.enmasse.io/registry.type"));
    }

    public static Endpoint getDeviceRegistryManagementEndpoint() {
        if (Kubernetes.isOpenShiftCompatible(OpenShiftVersion.OCP4)) {
            // openshift router
            return Kubernetes.getInstance().getExternalEndpoint("device-registry");
        } else {
            // load balancer service
            return Kubernetes.getInstance().getExternalEndpoint("iot-device-registry-external");
        }
    }
}
