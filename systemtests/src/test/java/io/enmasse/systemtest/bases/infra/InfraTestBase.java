/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.infra;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.AdminResourcesManager;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.ability.ITestBase;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class InfraTestBase extends TestBase implements ITestBase {

    private static Logger log = CustomLogger.getLogger();

    private static final List<String> resizingStorageProvisioners = Arrays.asList("kubernetes.io/aws-ebs", "kubernetes.io/gce-pd",
            "kubernetes.io/azure-file", "kubernetes.io/azure-disk", "kubernetes.io/glusterfs", "kubernetes.io/cinder",
            "kubernetes.io/portworx-volume", "kubernetes.io/rbd");

    protected static final AdminResourcesManager adminManager = new AdminResourcesManager();

    protected InfraConfig testInfra;
    protected AddressPlan exampleAddressPlan;
    protected AddressSpace exampleAddressSpace;

    @BeforeEach
    void setUp() throws Exception {
        adminManager.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        adminManager.tearDown();
    }

    protected void assertBroker(String brokerMemory, String brokerStorage, PodTemplateSpec templateSpec) {
        log.info("Checking broker infra");
        List<Pod> brokerPods = TestUtils.listBrokerPods(kubernetes, exampleAddressSpace);
        assertEquals(1, brokerPods.size());

        Pod broker = brokerPods.stream().findFirst().get();
        String actualBrokerMemory = broker.getSpec().getContainers().stream()
                .filter(container -> container.getName().equals("broker")).findFirst()
                .map(Container::getResources)
                .map(ResourceRequirements::getLimits)
                .get().get("memory").getAmount();
        assertEquals(brokerMemory, actualBrokerMemory, "Broker memory limit incorrect");

        if (brokerStorage != null) {
            PersistentVolumeClaim brokerVolumeClaim = getBrokerPVCData(broker);
            assertEquals(brokerStorage, brokerVolumeClaim.getSpec().getResources().getRequests().get("storage").getAmount(),
                    "Broker data storage request incorrect");
        }

        if (templateSpec != null) {
            assertTemplateSpec(broker, templateSpec);
        }
    }

    protected void assertTemplateSpec(Pod pod, PodTemplateSpec templateSpec) {
        if (templateSpec.getMetadata().getLabels() != null) {
            for (Map.Entry<String, String> labelPair : templateSpec.getMetadata().getLabels().entrySet()) {
                assertEquals(labelPair.getValue(), pod.getMetadata().getLabels().get(labelPair.getKey()), "Labels do not match");
            }
        }

        if (templateSpec.getSpec().getAffinity() != null) {
            assertEquals(templateSpec.getSpec().getAffinity(), pod.getSpec().getAffinity(), "Affinity rules do not match");
        }

        if (templateSpec.getSpec().getPriorityClassName() != null) {
            assertEquals(templateSpec.getSpec().getPriorityClassName(), pod.getSpec().getPriorityClassName(), "Priority class names do not match");
        }

        if (templateSpec.getSpec().getTolerations() != null) {
            for (Toleration expected : templateSpec.getSpec().getTolerations()) {
                boolean found = false;
                for (Toleration actual : pod.getSpec().getTolerations()) {
                    if (actual.equals(expected)) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found, "Did not find expected toleration " + expected);
            }
        }

        for (Container expectedContainer : templateSpec.getSpec().getContainers()) {
            for (Container actualContainer : pod.getSpec().getContainers()) {
                if (expectedContainer.getName().equals(actualContainer.getName())) {
                    assertEquals(expectedContainer.getResources(), actualContainer.getResources());
                }
            }
        }
    }

    protected void assertAdminConsole(String adminMemory, PodTemplateSpec templateSpec) {
        log.info("Checking admin console infra");
        List<Pod> adminPods = TestUtils.listAdminConsolePods(kubernetes, exampleAddressSpace);
        assertEquals(1, adminPods.size());

        List<ResourceRequirements> adminResources = adminPods.stream().findFirst().get().getSpec().getContainers()
                .stream().map(Container::getResources).collect(Collectors.toList());

        for (ResourceRequirements requirements : adminResources) {
            assertEquals(adminMemory, requirements.getLimits().get("memory").getAmount(),
                    "Admin console memory limit incorrect");
            assertEquals(adminMemory, requirements.getRequests().get("memory").getAmount(),
                    "Admin console memory requests incorrect");
        }

        if (templateSpec != null) {
            assertTemplateSpec(adminPods.get(0), templateSpec);
        }
    }

    protected void waitUntilInfraReady(Supplier<Boolean> assertCall, TimeoutBudget timeout) throws InterruptedException {
        log.info("Start waiting for infra ready");
        AssertionFailedError lastException = null;
        while (!timeout.timeoutExpired()) {
            try {
                assertCall.get();
                log.info("assert infra ready succeed");
                return;
            } catch (AssertionFailedError e) {
                lastException = e;
            }
            log.debug("next iteration, remaining time: {}", timeout.timeLeft());
            Thread.sleep(5000);
        }
        log.error("Timeout assert infra expired");
        if (lastException != null) {
            throw lastException;
        }
    }

    protected PersistentVolumeClaim getBrokerPVCData(Pod broker) {
        String brokerVolumeClaimName = broker.getSpec().getVolumes().stream()
                .filter(volume -> volume.getName().equals("data"))
                .findFirst().get()
                .getPersistentVolumeClaim().getClaimName();
        PersistentVolumeClaim brokerVolumeClaim = TestUtils.listPersistentVolumeClaims(kubernetes, exampleAddressSpace).stream()
                .filter(pvc -> pvc.getMetadata().getName().equals(brokerVolumeClaimName))
                .findFirst().get();
        return brokerVolumeClaim;
    }

    protected Boolean volumeResizingSupported() throws Exception {
        List<Pod> brokerPods = TestUtils.listBrokerPods(kubernetes, exampleAddressSpace);
        assertEquals(1, brokerPods.size());
        Pod broker = brokerPods.stream().findFirst().get();
        PersistentVolumeClaim brokerVolumeClaim = getBrokerPVCData(broker);
        String brokerStorageClassName = brokerVolumeClaim.getSpec().getStorageClassName();
        if (brokerStorageClassName != null) {
            StorageClass brokerStorageClass = kubernetes.getStorageClass(brokerStorageClassName);
            if (resizingStorageProvisioners.contains(brokerStorageClass.getProvisioner())) {
                if (brokerStorageClass.getAllowVolumeExpansion() != null && brokerStorageClass.getAllowVolumeExpansion()) {
                    log.info("Testing broker volume resize because of {}:{}", brokerStorageClassName, brokerStorageClass.getProvisioner());
                    return true;
                } else {
                    log.info("Skipping broker volume resize due to allowVolumeExpansion in StorageClass {} disabled", brokerStorageClassName);
                }
            } else {
                log.info("Skipping broker volume resize due to provisioner: {}", brokerStorageClass.getProvisioner());
            }
        } else {
            log.info("Skipping broker volume resize due to missing StorageClass name in PVC {}", brokerVolumeClaim.getMetadata().getName());
        }
        return false;
    }

}
