/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageSendTester;
import io.enmasse.systemtest.iot.MessageType;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.apache.qpid.proton.amqp.transport.End;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.apiclients.Predicates.any;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class IoTUtils {

    private static Kubernetes kubernetes = Kubernetes.getInstance();

    private static final String[] EXPECTED_DEPLOYMENTS = new String[]{
            "iot-auth-service",
            "iot-device-registry",
            "iot-http-adapter",
            "iot-lorawan-adapter",
            "iot-mqtt-adapter",
            "iot-operator",
            "iot-sigfox-adapter",
            "iot-tenant-service",
    };

    private static final Map<String, String> IOT_LABELS = Map.of("component", "iot");
    private static Logger log = CustomLogger.getLogger();

    public static void waitForIoTConfigReady(Kubernetes kubernetes, IoTConfig config) throws Exception {
        boolean isReady = false;
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        var iotConfigClient = kubernetes.getIoTConfigClient();
        while (budget.timeLeft() >= 0 && !isReady) {
            config = iotConfigClient.withName(config.getMetadata().getName()).get();
            isReady = config.getStatus() != null && config.getStatus().isInitialized();
            if (!isReady) {
                log.info("Waiting until IoTConfig: '{}' will be in ready state", config.getMetadata().getName());
                Thread.sleep(10000);
            }
        }
        if (!isReady) {
            String jsonStatus = config != null && config.getStatus() != null ? config.getStatus().getState() : "";
            throw new IllegalStateException("IoTConfig " + Objects.requireNonNull(config).getMetadata().getName() + " is not in Ready state within timeout: " + jsonStatus);
        }

        TestUtils.waitUntilCondition("IoT Config to deploy", (phase) -> allDeploymentsPresent(kubernetes), budget);
        TestUtils.waitForNReplicas(EXPECTED_DEPLOYMENTS.length, IOT_LABELS, budget);
    }

    public static void deleteIoTConfigAndWait(Kubernetes kubernetes, IoTConfig config) throws Exception {
        log.info("Deleting IoTConfig: {}", config.getMetadata().getName());
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_IOT_CONFIG);
        kubernetes.getIoTConfigClient(config.getMetadata().getNamespace()).withName(config.getMetadata().getName()).cascading(true).delete();
        waitForIoTConfigDeleted(kubernetes);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    private static void waitForIoTConfigDeleted(Kubernetes kubernetes) throws Exception {
        TestUtils.waitForNReplicas(0, false, IOT_LABELS, Collections.emptyMap(), new TimeoutBudget(2, TimeUnit.MINUTES), 5000);
    }

    private static boolean allDeploymentsPresent(Kubernetes kubernetes) {
        final String[] deployments = kubernetes.listDeployments(IOT_LABELS).stream()
                .map(deployment -> deployment.getMetadata().getName())
                .toArray(String[]::new);
        Arrays.sort(deployments);
        return Arrays.equals(deployments, EXPECTED_DEPLOYMENTS);
    }

    public static void waitForIoTProjectReady(Kubernetes kubernetes, IoTProject project) throws Exception {
        boolean isReady = false;
        TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
        var iotProjectClient = kubernetes.getIoTProjectClient(project.getMetadata().getNamespace());
        while (budget.timeLeft() >= 0 && !isReady) {
            project = iotProjectClient.withName(project.getMetadata().getName()).get();
            isReady = project.getStatus() != null && project.getStatus().isReady();
            if (!isReady) {
                log.info("Waiting until IoTProject: '{}' will be in ready state", project.getMetadata().getName());
                Thread.sleep(10000);
            }
        }
        if (!isReady) {
            String jsonStatus = project != null && project.getStatus() != null ? project.getStatus().toString() : "Project doesn't have status";
            throw new IllegalStateException("IoTProject " + project.getMetadata().getName() + " is not in Ready state within timeout: " + jsonStatus);
        }

        if (project.getSpec().getDownstreamStrategy() != null
                && project.getSpec().getDownstreamStrategy().getManagedStrategy() != null
                && project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace() != null
                && project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName() != null
        ) {
            var addressSpaceName = project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
            AddressSpaceUtils.waitForAddressSpaceReady(Kubernetes.getInstance().getAddressSpaceClient(project.getMetadata().getNamespace()).withName(addressSpaceName).get(), budget);
        }
    }

    private static void waitForIoTProjectDeleted(Kubernetes kubernetes, IoTProject project) throws Exception {
        if (project.getSpec().getDownstreamStrategy().getManagedStrategy() != null) {
            String addressSpaceName = project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
            AddressSpace addressSpace = kubernetes.getAddressSpaceClient(project.getMetadata().getNamespace()).withName(addressSpaceName).get();
            if (addressSpace != null) {
                AddressSpaceUtils.waitForAddressSpaceDeleted(addressSpace);
            }
        }
    }

    public static boolean isIoTInstalled(Kubernetes kubernetes) {
        return kubernetes.getCRD("iotprojects.iot.enmasse.io") != null;
    }

    public static void deleteIoTProjectAndWait(Kubernetes kubernetes, IoTProject project) throws Exception {
        log.info("Deleting IoTProject: {}", project.getMetadata().getName());
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_IOT_PROJECT);
        kubernetes.getIoTProjectClient(project.getMetadata().getNamespace()).withName(project.getMetadata().getName()).delete();
        IoTUtils.waitForIoTProjectDeleted(kubernetes, project);
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

    public static IoTProject getBasicIoTProjectObject(String name, String addressSpaceName, String namespace) {
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
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
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
        AddressSpace addressSpace = kubernetes.getAddressSpaceClient().inNamespace(project.getMetadata().getNamespace())
                .list().getItems().stream().filter(addressSpace1 -> addressSpace1.getMetadata().getName()
                        .equals(project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName()))
                .collect(Collectors.toList()).get(0);
    }

    public static String getTenantID(IoTProject project) {
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
        String tenantID = getTenantID(ioTProject);
        try (var httpAdapterClient = new HttpAdapterClient(kubernetes, httpAdapterEndpoint, authId, tenantID, password)) {

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
