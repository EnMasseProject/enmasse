/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import static io.enmasse.systemtest.utils.Predicates.any;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.AdapterConfig;
import io.enmasse.iot.model.v1.AdaptersConfig;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigSpec;
import io.enmasse.iot.model.v1.IoTCrd;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectBuilder;
import io.enmasse.iot.model.v1.MeshConfig;
import io.enmasse.iot.model.v1.ServiceConfig;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageType;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;

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
    private static final Logger log = CustomLogger.getLogger();

    public static void waitForIoTConfigReady(Kubernetes kubernetes, IoTConfig config) throws Exception {
        boolean isReady = false;
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        var iotConfigAccess = kubernetes.getIoTConfigClient(config.getMetadata().getNamespace()).withName(config.getMetadata().getName());
        while (budget.timeLeft() >= 0 && !isReady) {
            config = iotConfigAccess.get();
            isReady = config.getStatus() != null && "Active".equals(config.getStatus().getPhase());
            if (!isReady) {
                log.info("Waiting until IoTConfig: '{}' will be in ready state", config.getMetadata().getName());
                Thread.sleep(10000);
            }
        }
        if (!isReady) {
            String jsonStatus = config != null && config.getStatus() != null ? config.getStatus().getPhase() : "";
            throw new IllegalStateException("IoTConfig " + Objects.requireNonNull(config).getMetadata().getName() + " is not in Ready state within timeout: " + jsonStatus);
        }

        final String[] expectedDeployments = getExpectedDeploymentsNames(config);
        final String[] expectedStatefulSets = new String[] {IOT_SERVICE_MESH};

        final int meshReplicas = Optional.of(config)
                .map(IoTConfig::getSpec)
                .map(IoTConfigSpec::getMesh)
                .map(MeshConfig::getServiceConfig)
                .map(ServiceConfig::getReplicas)
                .orElse(1);

        TestUtils.waitUntilCondition("IoT Config to deploy", (phase) -> allThingsReadyPresent(kubernetes, expectedDeployments, expectedStatefulSets), budget);
        TestUtils.waitForNReplicas(expectedDeployments.length + meshReplicas, config.getMetadata().getNamespace(), IOT_LABELS, budget);
    }

    public static void deleteIoTConfigAndWait(Kubernetes kubernetes, IoTConfig config) throws Exception {
        log.info("Deleting IoTConfig: {} in namespace: {}", config.getMetadata().getName(), config.getMetadata().getNamespace());
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_IOT_CONFIG);
        kubernetes
                .getIoTConfigClient(config.getMetadata().getNamespace())
                .withName(config.getMetadata().getName())
                .withPropagationPolicy("Background")
                .delete();
        TestUtils.waitForNReplicas(0, false, config.getMetadata().getNamespace(), IOT_LABELS, Collections.emptyMap(), new TimeoutBudget(5, TimeUnit.MINUTES), 5000);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    private static boolean allThingsReadyPresent(final Kubernetes kubernetes, final String[] expectedDeployments, final String[] expectedStatefulSets) {

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

    public static void waitForIoTProjectReady(Kubernetes kubernetes, IoTProject project) throws Exception {
        boolean isReady = false;
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        var iotProjectClient = kubernetes.getIoTProjectClient(project.getMetadata().getNamespace());
        while (budget.timeLeft() >= 0 && !isReady) {
            project = iotProjectClient.withName(project.getMetadata().getName()).get();
            isReady = project.getStatus() != null && "Active".equals(project.getStatus().getPhase());
            if (!isReady) {
                log.info("Waiting until IoTProject: '{}' will be in ready state -> {}", project.getMetadata().getName(),
                        project.getStatus() != null ? project.getStatus().getPhase() : null);
                Thread.sleep(10000);
            }
        }

        final String jsonStatus = project.getStatus() != null ? Serialization.asJson(project.getStatus()) : "Project doesn't have status";
        if (!isReady) {
            throw new IllegalStateException("IoTProject " + project.getMetadata().getName() + " is not in Ready state within timeout: " + jsonStatus);
        }

        // refresh
        log.info("IoTProject is ready - phase: {} -> {}", project.getStatus().getPhase(), jsonStatus);

        if (project.getSpec().getDownstreamStrategy() != null
                && project.getSpec().getDownstreamStrategy().getManagedStrategy() != null
                && project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace() != null
                && project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName() != null) {
            var addressSpaceName = project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
            var addressSpace = Kubernetes.getInstance().getAddressSpaceClient(project.getMetadata().getNamespace()).withName(addressSpaceName).get();
            Objects.requireNonNull(addressSpace, () -> String.format("Unable to find addressSpace: %s", addressSpaceName));
            assertNotNull(addressSpace);
            assertNotNull(addressSpace.getStatus());
            assertTrue(addressSpace.getStatus().isReady());
        }

        // the project is ready, so we need to check a few things

        assertNotNull(project.getStatus());
        assertNotNull(project.getStatus().getDownstreamEndpoint());
        assertThat(project.getStatus().getDownstreamEndpoint().getHost(), not(emptyOrNullString()));
        assertThat(project.getStatus().getDownstreamEndpoint().getUsername(), not(emptyOrNullString()));
        assertThat(project.getStatus().getDownstreamEndpoint().getPassword(), not(emptyOrNullString()));
        assertThat(project.getStatus().getDownstreamEndpoint().getPort(), not(is(0)));
    }

    public static boolean isIoTInstalled(Kubernetes kubernetes) {
        return kubernetes.getCRD(IoTCrd.project().getMetadata().getName()) != null;
    }

    public static void deleteIoTProjectAndWait(IoTProject project) throws Exception {
        deleteIoTProjectAndWait(Kubernetes.getInstance(), project);
    }

    public static void deleteIoTProjectAndWait(Kubernetes kubernetes, IoTProject project) throws Exception {

        log.info("Deleting IoTProject: {}", project.getMetadata().getName());

        final String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_IOT_PROJECT);

        var projectClient = kubernetes.getIoTProjectClient(project.getMetadata().getNamespace());
        var asClient = kubernetes.getAddressSpaceClient(project.getMetadata().getNamespace());

        // get initial address spaces

        var initialAddressSpaces = projectClient
                .list().getItems().stream()
                .flatMap(p -> {
                    var managed = p.getSpec().getDownstreamStrategy().getManagedStrategy();
                    if (managed == null) {
                        return Stream.empty();
                    } else {
                        return Stream.ofNullable(managed.getAddressSpace().getName());
                    }
                })
                .collect(Collectors.toSet());

        log.info("Address spaces requested by IoT projects: {}", initialAddressSpaces);

        // pre-fetch address spaces for later use

        var deletedAddressSpaces = initialAddressSpaces.stream()
                .map(asName -> asClient.withName(asName).get())
                .filter(e -> e != null)
                .collect(Collectors.toMap(
                        e -> e.getMetadata().getName(),
                        e -> e));

        log.info("Address spaces which are expected, and actually exist: {}", deletedAddressSpaces.keySet());

        // delete the IoTProject

        kubernetes.getIoTProjectClient(project.getMetadata().getNamespace())
                .withName(project.getMetadata().getName())
                .withPropagationPolicy("Background")
                .delete();

        // wait until the IoTProject is deleted

        var projectName = project.getMetadata().getNamespace() + "/" + project.getMetadata().getName();
        TestUtils.waitUntilConditionOrFail(() -> {
            var updated = projectClient.withName(project.getMetadata().getName()).get();
            if (updated != null) {
                log.info("IoTProject {} still exists -> {}", projectName, updated.getStatus().getPhase());
            }
            return updated == null;
        }, Duration.ofMinutes(5), Duration.ofSeconds(10), () -> "IoT project failed to delete in time");
        log.info("IoTProject {} deleted", projectName);

        // now verify that the address spaces had all been created

        assertEquals(initialAddressSpaces, deletedAddressSpaces.keySet());

        // get the expected and actual address spaces

        var expectedAddressSpaces = projectClient
                .list().getItems().stream()
                .flatMap(p -> {
                    var managed = p.getSpec().getDownstreamStrategy().getManagedStrategy();
                    if (managed == null) {
                        return Stream.empty();
                    } else {
                        return Stream.ofNullable(managed.getAddressSpace().getName());
                    }
                })
                .collect(Collectors.toSet());

        // retain only the addresses spaces which are expected to be deleted

        deletedAddressSpaces.keySet().removeAll(expectedAddressSpaces);

        // verify the destruction of the address spaces

        for (final Map.Entry<String, AddressSpace> deleted : deletedAddressSpaces.entrySet()) {
            log.info("Verify destruction of address space: {}", deleted.getKey());
            AddressSpaceUtils.waitForAddressSpaceDeleted(deleted.getValue());
        }

        var actualAddressSpaces = asClient
                .list().getItems().stream()
                .map(as -> as.getMetadata().getName())
                .collect(Collectors.toSet());

        // verify that we only have expected address spaces remaining

        log.info("Address Spaces - expected: {}, actual: {}", expectedAddressSpaces, actualAddressSpaces);
        assertEquals(expectedAddressSpaces, actualAddressSpaces);

        // stop measuring time

        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void syncIoTProject(Kubernetes kubernetes, IoTProject project) throws Exception {
        IoTProject result = kubernetes.getIoTProjectClient(project.getMetadata().getNamespace()).withName(project.getMetadata().getName()).get();
        project.setMetadata(result.getMetadata());
        project.setSpec(result.getSpec());
        project.setStatus(result.getStatus());
    }

    public static void syncIoTConfig(Kubernetes kubernetes, IoTConfig config) throws Exception {
        IoTConfig result = kubernetes.getIoTConfigClient().withName(config.getMetadata().getName()).get();
        config.setMetadata(result.getMetadata());
        config.setSpec(result.getSpec());
        config.setStatus(result.getStatus());
    }

    public static IoTProject getBasicIoTProjectObject(String name, String addressSpaceName, String namespace, String addressSpacePlan) {
        return new IoTProjectBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withNewDownstreamStrategy()
                .withNewManagedStrategy()
                .withNewAddressSpace()
                .withName(addressSpaceName)
                .withPlan(addressSpacePlan)
                .endAddressSpace()
                .withNewAddresses()
                .withNewTelemetry()
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endTelemetry()
                .withNewEvent()
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endEvent()
                .withNewCommand()
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endCommand()
                .endAddresses()
                .endManagedStrategy()
                .endDownstreamStrategy()
                .endSpec()
                .build();
    }

    public static void createIoTConfig(IoTConfig config) throws Exception {
        var name = config.getMetadata().getName();
        log.info("Creating IoTConfig - name: {}", name);
        Kubernetes kubernetes = Kubernetes.getInstance();
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_IOT_CONFIG);
        var iotConfigApiClient = kubernetes.getIoTConfigClient(config.getMetadata().getNamespace());
        if (iotConfigApiClient.withName(name).get() != null) {
            log.info("iot config {} already exists", name);
        } else {
            log.info("iot config {} will be created", name);
            iotConfigApiClient.create(config);
        }
        IoTUtils.waitForIoTConfigReady(kubernetes, config);
        IoTUtils.syncIoTConfig(kubernetes, config);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void createIoTProject(IoTProject project) throws Exception {
        var name = project.getMetadata().getName();
        log.info("Creating IoTProject - name: {}", name);
        Kubernetes kubernetes = Kubernetes.getInstance();
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_IOT_PROJECT);
        var iotProjectApiClient = kubernetes.getIoTProjectClient(project.getMetadata().getNamespace());
        if (iotProjectApiClient.withName(name).get() != null) {
            log.info("iot project {} already exists", name);
        } else {
            log.info("iot project {} will be created", name);
            iotProjectApiClient.create(project);
        }
        IoTUtils.waitForIoTProjectReady(kubernetes, project);
        IoTUtils.syncIoTProject(kubernetes, project);
        TimeMeasuringSystem.stopOperation(operationID);

    }

    public static String getTenantId(IoTProject project) {
        return String.format("%s.%s", project.getMetadata().getNamespace(), project.getMetadata().getName());
    }

    public static void waitForFirstSuccess(HttpAdapterClient adapterClient, MessageType type) throws Exception {
        JsonObject json = new JsonObject(Map.of("a", "b"));
        String message = "First successful " + type.name().toLowerCase() + " message";
        TestUtils.waitUntilCondition(message, (phase) -> {
            try {
                switch (type) {
                    case EVENT: {
                        var response = adapterClient.sendEvent(json.toBuffer(), any());
                        logResponseIfLastTryFailed(phase, response, message);
                        return response.statusCode() == HTTP_ACCEPTED;
                    }
                    case TELEMETRY: {
                        var response = adapterClient.sendTelemetry(json.toBuffer(), any());
                        logResponseIfLastTryFailed(phase, response, message);
                        return response.statusCode() == HTTP_ACCEPTED;
                    }
                    default:
                        return true;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, new TimeoutBudget(3, TimeUnit.MINUTES));

        log.info("First {} message accepted", type.name().toLowerCase());
    }

    private static void logResponseIfLastTryFailed(WaitPhase phase, HttpResponse<?> response, String warnMessage) {
        if (phase == WaitPhase.LAST_TRY && response.statusCode() != HTTP_ACCEPTED) {
            log.error("expected-code: {}, response-code: {}, body: {}, op: {}", HTTP_ACCEPTED, response.statusCode(), response.body(), warnMessage);
        }
    }

    public static void assertCorrectDeviceConnectionType(final String type) {
        final Deployment deployment =
                Kubernetes.getInstance().getClient().apps().deployments().inNamespace(Kubernetes.getInstance().getInfraNamespace()).withName("iot-device-connection").get();
        assertNotNull(deployment);
        assertEquals(type, deployment.getMetadata().getAnnotations().get("iot.enmasse.io/deviceConnection.type"));
    }

    public static void assertCorrectRegistryType(final String type) {
        final Deployment deployment =
                Kubernetes.getInstance().getClient().apps().deployments().inNamespace(Kubernetes.getInstance().getInfraNamespace()).withName("iot-device-registry").get();
        assertNotNull(deployment);
        assertEquals(type, deployment.getMetadata().getAnnotations().get("iot.enmasse.io/registry.type"));
    }

    public static void checkCredentials(String authId, String password, boolean authFail, Endpoint httpAdapterEndpoint, AmqpClient iotAmqpClient, IoTProject ioTProject)
            throws Exception {
        String tenantID = getTenantId(ioTProject);
        try (var httpAdapterClient = new HttpAdapterClient(null, httpAdapterEndpoint, authId, tenantID, password)) {

            try {
                new MessageSendTester()
                        .type(MessageSendTester.Type.TELEMETRY)
                        .amount(1)
                        .consumerFactory(MessageSendTester.ConsumerFactory.of(iotAmqpClient, tenantID))
                        .sender(httpAdapterClient::send)
                        .execute();
                if (authFail) {
                    fail("Expected to fail telemetry test");
                }
            } catch (TimeoutException e) {
                if (!authFail) {
                    throw e;
                }
            }

            try {
                new MessageSendTester()
                        .type(MessageSendTester.Type.EVENT)
                        .amount(1)
                        .consumerFactory(MessageSendTester.ConsumerFactory.of(iotAmqpClient, tenantID))
                        .sender(httpAdapterClient::send)
                        .execute();
                if (authFail) {
                    fail("Expected to fail telemetry test");
                }
            } catch (TimeoutException e) {
                if (!authFail) {
                    throw e;
                }
            }

        }
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
