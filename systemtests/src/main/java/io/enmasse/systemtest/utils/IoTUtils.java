/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectBuilder;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.timemeasuring.SystemtestsOperation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class IoTUtils {

    private static Logger log = CustomLogger.getLogger();

    private static final String[] EXPECTED_DEPLOYMENTS = new String[]{
            "iot-auth-service",
            "iot-device-registry",
            "iot-gc",
            "iot-http-adapter",
            "iot-mqtt-adapter",
            "iot-tenant-service",
    };


    private static final Map<String, String> IOT_LABELS = Map.of("component", "iot");

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

    public static void deleteIoTConfigAndWait(Kubernetes kubernetes, IoTConfig config) throws Exception{
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_IOT_CONFIG);
        kubernetes.getIoTConfigClient().withName(config.getMetadata().getName()).cascading(true).delete();
        waitForIoTConfigDeleted(kubernetes);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    private static void waitForIoTConfigDeleted(Kubernetes kubernetes) throws Exception{
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
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED)
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

}
