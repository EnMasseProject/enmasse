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
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.apiclients.IoTConfigApiClient;
import io.enmasse.systemtest.apiclients.IoTProjectApiClient;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.timemeasuring.SystemtestsOperation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import org.slf4j.Logger;

import static io.enmasse.systemtest.utils.AddressSpaceUtils.jsonToAdressSpace;

import java.util.Arrays;
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

    public static void waitForIoTConfigReady(IoTConfigApiClient apiClient, Kubernetes kubernetes, IoTConfig config) throws Exception {
        boolean isReady = false;
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        while (budget.timeLeft() >= 0 && !isReady) {
            config = apiClient.getIoTConfig(config.getMetadata().getName());
            isReady = config.getStatus() != null && config.getStatus().isInitialized();
            if (!isReady) {
                log.info("Waiting until IoTConfig: '{}' will be in ready state", config.getMetadata().getName());
                Thread.sleep(10000);
            }
        }
        if (!isReady) {
            String jsonStatus = config != null ? config.getStatus().getState() : "";
            throw new IllegalStateException("IoTConfig " + Objects.requireNonNull(config).getMetadata().getName() + " is not in Ready state within timeout: " + jsonStatus);
        }

        TestUtils.waitUntilCondition("IoT Config to deploy", (phase)->allDeploymentsPresent(kubernetes), budget);
        TestUtils.waitForNReplicas(kubernetes, EXPECTED_DEPLOYMENTS.length, IOT_LABELS, budget);
    }

    private static boolean allDeploymentsPresent(Kubernetes kubernetes) {
        final String[] deployments = kubernetes.listDeployments(IOT_LABELS).stream()
                .map(deployment -> deployment.getMetadata().getName())
                .toArray(String[]::new);
        Arrays.sort(deployments);
        return Arrays.equals(deployments, EXPECTED_DEPLOYMENTS);
    }

    public static void waitForIoTProjectReady(IoTProjectApiClient apiClient, AddressApiClient addressSpaceApiClient, IoTProject project) throws Exception {
        boolean isReady = false;
        TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
        while (budget.timeLeft() >= 0 && !isReady) {
            project = apiClient.getIoTProject(project.getMetadata().getName());
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

        if ( project.getSpec().getDownstreamStrategy() != null
                && project.getSpec().getDownstreamStrategy().getManagedStrategy() != null
                && project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace() != null
                && project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName() != null
                ) {
            var addressSpaceName = project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
            AddressSpaceUtils.waitForAddressSpaceReady(addressSpaceApiClient, jsonToAdressSpace(addressSpaceApiClient.getAddressSpace(addressSpaceName)), budget);
        }
    }

    private static void waitForIoTProjectDeleted(Kubernetes kubernetes, AddressApiClient addressApiClient, IoTProject project) throws Exception {
        if (project.getSpec().getDownstreamStrategy().getManagedStrategy() != null) {
            String addressSpaceName = project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
            AddressSpace addressSpace = AddressSpaceUtils.getAddressSpacesObjects(addressApiClient)
                    .stream()
                    .filter(space -> space.getMetadata().getName().equals(addressSpaceName))
                    .findFirst()
                    .orElse(null);
            if (addressSpace != null) {
                AddressSpaceUtils.waitForAddressSpaceDeleted(kubernetes, addressSpace);
            }
        }
    }

    public static boolean isIoTInstalled(Kubernetes kubernetes) {
        return kubernetes.getCRD("iotprojects.iot.enmasse.io") != null;
    }

    public static void deleteIoTProjectAndWait(Kubernetes kubernetes, IoTProjectApiClient iotProjectApiClient, IoTProject project, AddressApiClient addressApiClient) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_IOT_PROJECT);
        iotProjectApiClient.deleteIoTProject(project.getMetadata().getName());
        IoTUtils.waitForIoTProjectDeleted(kubernetes, addressApiClient, project);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void syncIoTProject(IoTProject project, IoTProjectApiClient apiClient) throws Exception {
        IoTProject result = apiClient.getIoTProject(project.getMetadata().getName());
        project.setMetadata(result.getMetadata());
        project.setSpec(result.getSpec());
        project.setStatus(result.getStatus());
    }

    public static void syncIoTConfig(IoTConfig config, IoTConfigApiClient apiClient) throws Exception {
        IoTConfig result = apiClient.getIoTConfig(config.getMetadata().getName());
        config.setMetadata(result.getMetadata());
        config.setSpec(result.getSpec());
        config.setStatus(result.getStatus());
    }

    public static IoTProject getBasicIoTProjectObject(String name, String addressSpaceName) {
        return new IoTProjectBuilder()
                .withNewMetadata()
                .withName(name)
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
