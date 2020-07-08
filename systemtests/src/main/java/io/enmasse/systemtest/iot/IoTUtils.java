/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import static io.enmasse.systemtest.platform.Kubernetes.getClient;
import static io.enmasse.systemtest.platform.Kubernetes.iotConfigs;
import static io.enmasse.systemtest.platform.Kubernetes.iotTenants;
import static io.enmasse.systemtest.time.TimeMeasuringSystem.Operation.startOperation;
import static io.enmasse.systemtest.utils.TestUtils.TimeoutHandler.explain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;

import io.enmasse.iot.model.v1.AdapterConfig;
import io.enmasse.iot.model.v1.AdaptersConfig;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigSpec;
import io.enmasse.iot.model.v1.IoTConfigStatus;
import io.enmasse.iot.model.v1.IoTCrd;
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
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;

public class IoTUtils {

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

    public static void waitForIoTConfigReady(IoTConfig config) throws Exception {
        var budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        var iotConfigAccess = iotConfigs(config.getMetadata().getNamespace()).withName(config.getMetadata().getName());
        TestUtils.waitUntilCondition(() -> {
                    var currentState = iotConfigAccess.get();
                    var currentPhase = Optional.ofNullable(currentState)
                            .map(IoTConfig::getStatus)
                            .map(IoTConfigStatus::getPhase)
                            .orElse(null);
                    if ("Active".equals(currentPhase)) {
                        log.info("IoTConfig is ready - phase: {} -> {}", currentPhase, Serialization.asJson(currentState.getStatus()));
                        return true;
                    } else {
                        log.info("Waiting until IoTConfig: '{}' will be in ready state", config.getMetadata().getName());
                        return false;
                    }
                },
                budget.remaining(), Duration.ofSeconds(5),
                explain(() -> Optional.ofNullable(iotConfigAccess.get())
                        .map(IoTConfig::getStatus)
                        .map(Serialization::asJson)
                        .orElse("IoTConfig has no status section")));

        final String[] expectedDeployments = getExpectedDeploymentsNames(config);
        final String[] expectedStatefulSets = new String[]{IOT_SERVICE_MESH};

        final int meshReplicas = Optional.of(config)
                .map(IoTConfig::getSpec)
                .map(IoTConfigSpec::getMesh)
                .map(MeshConfig::getServiceConfig)
                .map(ServiceConfig::getReplicas)
                .orElse(1);

        TestUtils.waitUntilCondition("IoT Config to deploy", (phase) -> allThingsReadyPresent(expectedDeployments, expectedStatefulSets), budget);
        TestUtils.waitForNReplicas(expectedDeployments.length + meshReplicas, config.getMetadata().getNamespace(), IOT_LABELS, budget);
    }

    public static void deleteIoTConfigAndWait(IoTConfig config) throws Exception {
        log.info("Deleting IoTConfig: {} in namespace: {}", config.getMetadata().getName(), config.getMetadata().getNamespace());
        try ( var ignored = startOperation(SystemtestsOperation.DELETE_IOT_CONFIG)) {
            iotConfigs(config.getMetadata().getNamespace())
                    .withName(config.getMetadata().getName())
                    .withPropagationPolicy("Background")
                    .delete();
            TestUtils.waitForNReplicas(0, false, config.getMetadata().getNamespace(), IOT_LABELS, Collections.emptyMap(), new TimeoutBudget(5, TimeUnit.MINUTES), 5000);
        }
    }

    private static boolean allThingsReadyPresent(final String[] expectedDeployments, final String[] expectedStatefulSets) {

        // get deployments

        final String[] deployments = Kubernetes.getInstance().listDeployments(IOT_LABELS).stream()
                .map(deployment -> deployment.getMetadata().getName())
                .toArray(String[]::new);
        Arrays.sort(deployments);
        Arrays.sort(expectedDeployments);

        // get stateful sets

        final String[] statefulSets = Kubernetes.getInstance().listStatefulSets(IOT_LABELS).stream()
                .map(statefulSet -> statefulSet.getMetadata().getName())
                .toArray(String[]::new);
        Arrays.sort(statefulSets);
        Arrays.sort(expectedStatefulSets);

        // compare

        return Arrays.equals(deployments, expectedDeployments)
                && Arrays.equals(statefulSets, expectedStatefulSets);

    }

    private static String[] getExpectedDeploymentsNames(IoTConfig config) {

        final Collection<String> expectedDeployments = new ArrayList<>();

        // protocol adapters

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
            if ( external.getManagement() != null && external.getAdapter() != null ) {
                expectedDeployments.add(IOT_DEVICE_REGISTRY_MANAGEMENT);
            }
        }

        // common services

        expectedDeployments.addAll(Arrays.asList(IOT_AUTH_SERVICE, IOT_DEVICE_CONNECTION, IOT_TENANT_SERVICE));

        return expectedDeployments.toArray(String[]::new);
    }

    private static void addIfEnabled(Collection<String> adapters, IoTConfig config, Function<AdaptersConfig, AdapterConfig> adapterGetter, String name) {
        Optional<Boolean> enabled = Optional.ofNullable(config.getSpec().getAdapters()).map(adapterGetter).map(AdapterConfig::getEnabled);
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
                    var currentPhase = Optional.ofNullable(currentState)
                            .map(IoTProject::getStatus)
                            .map(IoTProjectStatus::getPhase)
                            .orElse(null);
                    if ("Active".equals(currentPhase)) {
                        log.info("IoTProject is ready - phase: {} -> {}", currentPhase, Serialization.asJson(currentState.getStatus()));
                        return true;
                    } else {
                        log.info("Waiting until IoTProject: '{}' will be in ready state -> {}", project.getMetadata().getName(), currentPhase);
                        return false;
                    }
                },
                budget.remaining(), Duration.ofSeconds(5),
                explain(() -> Optional.ofNullable(tenantAccess.get())
                        .map(IoTProject::getStatus)
                        .map(Serialization::asJson)
                        .orElse("Project doesn't have status")));

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
                    .withPropagationPolicy("Background")
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

    static void createIoTProject(IoTProject project) {
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
            IoTUtils.waitForIoTProjectReady(project);
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
