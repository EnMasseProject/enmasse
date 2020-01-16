/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.AdapterConfig;
import io.enmasse.iot.model.v1.AdaptersConfig;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTCrd;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageType;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import static io.enmasse.systemtest.platform.KubeCMDClient.patchCR;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;

import org.slf4j.Logger;

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

import static io.enmasse.systemtest.apiclients.Predicates.any;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class IoTUtils {

    private static Kubernetes kubernetes = Kubernetes.getInstance();

    private static final String IOT_SIGFOX_ADAPTER = "iot-sigfox-adapter";
    private static final String IOT_MQTT_ADAPTER = "iot-mqtt-adapter";
    private static final String IOT_LORAWAN_ADAPTER = "iot-lorawan-adapter";
    private static final String IOT_HTTP_ADAPTER = "iot-http-adapter";
    private static final String IOT_AUTH_SERVICE = "iot-auth-service";
    private static final String IOT_DEVICE_REGISTRY = "iot-device-registry";
    private static final String IOT_OPERATOR = "iot-operator";
    private static final String IOT_TENANT_SERVICE = "iot-tenant-service";


    private static final Map<String, String> IOT_LABELS = Map.of("component", "iot");
    private static Logger log = CustomLogger.getLogger();

    public static void waitForIoTConfigReady(Kubernetes kubernetes, IoTConfig config) throws Exception {
        boolean isReady = false;
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        var iotConfigClient = kubernetes.getIoTConfigClient();
        while (budget.timeLeft() >= 0 && !isReady) {
            config = iotConfigClient.withName(config.getMetadata().getName()).get();
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

        String[] expectedDeployments = getExpectedDeploymentsNames(config);

        TestUtils.waitUntilCondition("IoT Config to deploy", (phase) -> allDeploymentsPresent(kubernetes, expectedDeployments), budget);
        TestUtils.waitForNReplicas(expectedDeployments.length, IOT_LABELS, budget);
    }

    public static void deleteIoTConfigAndWait(Kubernetes kubernetes, IoTConfig config) throws Exception {
        log.info("Deleting IoTConfig: {} in namespace: {}", config.getMetadata().getName(), config.getMetadata().getNamespace());
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_IOT_CONFIG);
        kubernetes.getIoTConfigClient(config.getMetadata().getNamespace()).withName(config.getMetadata().getName()).cascading(true).delete();
        waitForIoTConfigDeleted(kubernetes);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    private static void waitForIoTConfigDeleted(Kubernetes kubernetes) throws Exception {
        TestUtils.waitForNReplicas(0, false, IOT_LABELS, Collections.emptyMap(), new TimeoutBudget(5, TimeUnit.MINUTES), 5000);
    }

    private static boolean allDeploymentsPresent(Kubernetes kubernetes, String[] expectedDeployments) {
        final String[] deployments = kubernetes.listDeployments(IOT_LABELS).stream()
                .map(deployment -> deployment.getMetadata().getName())
                .toArray(String[]::new);
        Arrays.sort(deployments);
        Arrays.sort(expectedDeployments);

        return Arrays.equals(deployments, expectedDeployments);
    }

    private static String[] getExpectedDeploymentsNames(IoTConfig config) {
        Collection<String> expectedDeployments = new ArrayList<>();
        addIfEnabled(expectedDeployments, config, AdaptersConfig::getHttp, IOT_HTTP_ADAPTER);
        addIfEnabled(expectedDeployments, config, AdaptersConfig::getLoraWan, IOT_LORAWAN_ADAPTER);
        addIfEnabled(expectedDeployments, config, AdaptersConfig::getMqtt, IOT_MQTT_ADAPTER);
        addIfEnabled(expectedDeployments, config, AdaptersConfig::getSigfox, IOT_SIGFOX_ADAPTER);
        expectedDeployments.addAll(Arrays.asList(IOT_AUTH_SERVICE, IOT_DEVICE_REGISTRY, IOT_OPERATOR, IOT_TENANT_SERVICE));
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
                log.info("Waiting until IoTProject: '{}' will be in ready state -> {}", project.getMetadata().getName(), project.getStatus() != null ? project.getStatus().getPhase() : null);
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
                && project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName() != null
        ) {
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

    private static void waitForIoTProjectDeleted(Kubernetes kubernetes, IoTProject project) throws Exception {
        if (project.getSpec().getDownstreamStrategy().getManagedStrategy() != null) {
            String addressSpaceName = project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
            AddressSpace addressSpace = kubernetes.getAddressSpaceClient(project.getMetadata().getNamespace()).withName(addressSpaceName).get();
            if (addressSpace != null) {
                AddressSpaceUtils.waitForAddressSpaceDeleted(addressSpace);
            }
        }
        var client = kubernetes.getIoTProjectClient(project.getMetadata().getNamespace());
        TestUtils.waitUntilConditionOrFail(() -> {
            var updated = client.withName(project.getMetadata().getName()).get();
            if (updated != null) {
                log.info("IoTProject {}/{} still exists -> {}", project.getMetadata().getNamespace(), project.getMetadata().getName(), updated.getStatus().getPhase());
            }
            return updated == null;
        }, Duration.ofMinutes(5), Duration.ofSeconds(10), () -> "IoT project failed to delete in time");
        log.info("IoTProject {}/{} deleted", project.getMetadata().getNamespace(), project.getMetadata().getName());
    }

    public static boolean isIoTInstalled(Kubernetes kubernetes) {
        return kubernetes.getCRD(IoTCrd.project().getMetadata().getName()) != null;
    }

    public static void deleteIoTProjectAndWait(Kubernetes kubernetes, IoTProject project) throws Exception {
        log.info("Deleting IoTProject: {}", project.getMetadata().getName());
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_IOT_PROJECT);
        kubernetes.getIoTProjectClient(project.getMetadata().getNamespace()).withName(project.getMetadata().getName()).cascading(true).delete();
        try {
            IoTUtils.waitForIoTProjectDeleted(kubernetes, project);
        } catch (Exception e) {
            log.warn("IoT project '{}' failed to delete. Removing finalizers!", project.getMetadata().getName(), e);
            assertTrue(patchCR(project.getKind().toLowerCase(), project.getMetadata().getName(), "'{\"metadata\":{\"finalizers\": []}}'").getRetCode());
            IoTUtils.waitForIoTProjectDeleted(kubernetes, project);
            throw e;
        }
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
        Kubernetes kubernetes = Kubernetes.getInstance();
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_IOT_CONFIG);
        var iotConfigApiClient = kubernetes.getIoTConfigClient();
        if (iotConfigApiClient.withName(config.getMetadata().getName()).get() != null) {
            log.info("iot config {} already exists", config.getMetadata().getName());
        } else {
            log.info("iot config {} will be created", config.getMetadata().getName());
            iotConfigApiClient.create(config);
        }
        IoTUtils.waitForIoTConfigReady(kubernetes, config);
        IoTUtils.syncIoTConfig(kubernetes, config);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void createIoTProject(IoTProject project) throws Exception {
        Kubernetes kubernetes = Kubernetes.getInstance();
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_IOT_PROJECT);
        var iotProjectApiClient = kubernetes.getIoTProjectClient(project.getMetadata().getNamespace());
        if (iotProjectApiClient.withName(project.getMetadata().getName()).get() != null) {
            log.info("iot project {} already exists", project.getMetadata().getName());
        } else {
            log.info("iot project {} will be created", project.getMetadata().getName());
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
                        var response = adapterClient.sendEvent(json, any());
                        logResponseIfLastTryFailed(phase, response, message);
                        return response.statusCode() == HTTP_ACCEPTED;
                    }
                    case TELEMETRY: {
                        var response = adapterClient.sendTelemetry(json, any());
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

    public static void assertCorrectRegistryType(final String type) {
        final Deployment deployment = Kubernetes.getInstance().getClient().apps().deployments().inNamespace(Kubernetes.getInstance().getInfraNamespace()).withName("iot-device-registry").get();
        assertNotNull(deployment);
        assertEquals(type, deployment.getMetadata().getAnnotations().get("iot.enmasse.io/registry.type"));
    }

    public static void checkCredentials(String authId, String password, boolean authFail, Endpoint httpAdapterEndpoint, AmqpClient iotAmqpClient, IoTProject ioTProject) throws Exception {
        String tenantID = getTenantId(ioTProject);
        try (var httpAdapterClient = new HttpAdapterClient(httpAdapterEndpoint, authId, tenantID, password)) {

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
}
