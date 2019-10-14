/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestBaseIsolated;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.enmasse.systemtest.TestTag.ISOLATED;
import static io.enmasse.systemtest.TestTag.NON_PR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(ISOLATED)
class CommonTest extends TestBase implements ITestBaseIsolated {
    private static Logger log = CustomLogger.getLogger();

    @Test
    void testAccessLogs() throws Exception {
        AddressSpace standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("mystandard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(standard);

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(standard.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(standard, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();
        resourcesManager.setAddresses(dest);

        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        List<Pod> unready;
        do {
            unready = new ArrayList<>(kubernetes.listPods());
            unready.removeIf(p -> TestUtils.isPodReady(p, true));

            if (!unready.isEmpty()) {
                Thread.sleep(1000L);
            }
        } while (!unready.isEmpty() && budget.timeLeft() > 0);

        if (!unready.isEmpty()) {
            fail(String.format(" %d pod(s) still unready", unready.size()));
        }

        List<Map.Entry<String, String>> podsContainersWithNoLog = new ArrayList<>();

        kubernetes.listPods().forEach(pod -> kubernetes.getContainersFromPod(pod.getMetadata().getName()).forEach(container -> {
            String podName = pod.getMetadata().getName();
            String containerName = container.getName();
            log.info("Getting log from pod: {}, for container: {}", podName, containerName);
            String podlog = kubernetes.getLog(podName, containerName);

            // Retry - diagnostic code to help understand a sporadic Ci failure.
            if (podlog.isEmpty()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                log.info("(Retry) Getting log from pod: {}, for container: {}", podName, containerName);
                podlog = kubernetes.getLog(podName, containerName);
            }

            if (podlog.isEmpty()) {
                podsContainersWithNoLog.add(of(podName, containerName));
            }

        }));

        if (!podsContainersWithNoLog.isEmpty()) {
            String podContainerNames = podsContainersWithNoLog.stream().map(e -> String.format("%s-%s", e.getKey(), e.getValue())).collect(Collectors.joining(","));
            fail(String.format("%d pod container(s) had unexpectedly empty logs : %s ", podsContainersWithNoLog.size(), podContainerNames));
        }
    }

    private <K, V> Map.Entry<K, V> of(K k, V v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    @Test
    void testRestartComponents() throws Exception {
        List<Label> labels = new LinkedList<>();
        labels.add(new Label("component", "api-server"));
        labels.add(new Label("name", "standard-authservice"));
        labels.add(new Label("name", "address-space-controller"));
        labels.add(new Label("name", "enmasse-operator"));

        UserCredentials user = new UserCredentials("frantisek", "dobrota");
        AddressSpace brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("space-restart-brokered")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        AddressSpace standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("space-restart-standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpaceList(standard, brokered);
        resourcesManager.createOrUpdateUser(brokered, user);
        resourcesManager.createOrUpdateUser(standard, user);

        List<Address> brokeredAddresses = getAllBrokeredAddresses(brokered);
        List<Address> standardAddresses = getAllStandardAddresses(standard);

        resourcesManager.setAddresses(brokeredAddresses.toArray(new Address[0]));
        resourcesManager.setAddresses(standardAddresses.toArray(new Address[0]));

        getClientUtils().assertCanConnect(brokered, user, brokeredAddresses, resourcesManager);
        getClientUtils().assertCanConnect(standard, user, standardAddresses, resourcesManager);

        log.info("------------------------------------------------------------");
        log.info("------------------- Start with restating -------------------");
        log.info("------------------------------------------------------------");

        List<Pod> pods = kubernetes.listPods();
        int runningPodsBefore = pods.size();
        log.info("Number of running pods before restarting any: {}", runningPodsBefore);

        for (Label label : labels) {
            log.info("Restarting {}", label.labelValue);
            KubeCMDClient.deletePodByLabel(label.getLabelName(), label.getLabelValue());
            Thread.sleep(30_000);
            TestUtils.waitForExpectedReadyPods(kubernetes, kubernetes.getInfraNamespace(), runningPodsBefore, new TimeoutBudget(10, TimeUnit.MINUTES));
            assertSystemWorks(brokered, standard, user, brokeredAddresses, standardAddresses);
        }

        log.info("Restarting whole enmasse");
        KubeCMDClient.deletePodByLabel("app", kubernetes.getEnmasseAppLabel());
        Thread.sleep(180_000);
        TestUtils.waitForExpectedReadyPods(kubernetes, kubernetes.getInfraNamespace(), runningPodsBefore, new TimeoutBudget(10, TimeUnit.MINUTES));
        AddressUtils.waitForDestinationsReady(new TimeoutBudget(10, TimeUnit.MINUTES),
                standardAddresses.toArray(new Address[0]));
        assertSystemWorks(brokered, standard, user, brokeredAddresses, standardAddresses);

        //TODO: Uncomment when #2127 will be fixedy

//        Pod qdrouter = pods.stream().filter(pod -> pod.getMetadata().getName().contains("qdrouter")).collect(Collectors.toList()).get(0);
//        kubernetes.deletePod(environment.namespace(), qdrouter.getMetadata().getName());
//        assertSystemWorks(brokered, standard, user, brokeredAddresses, standardAddresses);
    }

    //https://github.com/EnMasseProject/enmasse/issues/3098
    @Test
    void testRestartAdminComponent() throws Exception {
        List<Label> labels = new LinkedList<>();
        labels.add(new Label("name", "admin"));

        UserCredentials user = new UserCredentials("frantisek", "dobrota");
        AddressSpace brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("space-restart-brokered")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        AddressSpace standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("space-restart-standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpaceList(standard, brokered);
        resourcesManager.createOrUpdateUser(brokered, user);
        resourcesManager.createOrUpdateUser(standard, user);

        List<Address> brokeredAddresses = Arrays.asList(
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(brokered.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(brokered, "test-queue"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue")
                        .withPlan(DestinationPlan.BROKERED_QUEUE)
                        .endSpec()
                        .build());

        List<Address> standardAddresses = Arrays.asList(
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(standard.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(standard, "test-queue"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue")
                        .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(standard.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(standard, "test-queue-sharded"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue-sharded")
                        .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                        .endSpec()
                        .build());


        resourcesManager.setAddresses(brokeredAddresses.toArray(new Address[0]));
        resourcesManager.setAddresses(standardAddresses.toArray(new Address[0]));

        getClientUtils().assertCanConnect(brokered, user, brokeredAddresses, resourcesManager);
        getClientUtils().assertCanConnect(standard, user, standardAddresses, resourcesManager);

        log.info("Sending messages before admin pod restart");

        for (Address addr : brokeredAddresses) {
            getClientUtils().sendDurableMessages(resourcesManager, brokered, addr, user, 15);
        }

        for (Address addr : standardAddresses) {
            getClientUtils().sendDurableMessages(resourcesManager, standard, addr, user, 15);
        }

        log.info("------------------------------------------------------------");
        log.info("------------------- Start with restating -------------------");
        log.info("------------------------------------------------------------");

        List<Pod> pods = kubernetes.listPods();
        int runningPodsBefore = pods.size();
        log.info("Number of running pods before restarting any: {}", runningPodsBefore);

        for (Label label : labels) {
            log.info("Restarting {}", label.labelValue);
            KubeCMDClient.deletePodByLabel(label.getLabelName(), label.getLabelValue());
            Thread.sleep(30_000);
            TestUtils.waitForExpectedReadyPods(kubernetes, kubernetes.getInfraNamespace(), runningPodsBefore, new TimeoutBudget(10, TimeUnit.MINUTES));
            assertSystemWorks(brokered, standard, user, brokeredAddresses, standardAddresses);
        }

        log.info("Receiving messages after admin pod restart");

        for (Address addr : brokeredAddresses) {
            getClientUtils().receiveDurableMessages(resourcesManager, brokered, addr, user, 15);
        }

        for (Address addr : standardAddresses) {
            getClientUtils().receiveDurableMessages(resourcesManager, standard, addr, user, 15);
        }

    }

    @Test
    void testMonitoringTools() throws Exception {
        AddressSpace standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-space-monitor")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(standard);
        resourcesManager.createOrUpdateUser(standard, new UserCredentials("jenda", "cenda"));
        resourcesManager.setAddresses(getAllStandardAddresses(standard).toArray(new Address[0]));

        String qdRouterName = TestUtils.listRunningPods(kubernetes, standard).stream()
                .filter(pod -> pod.getMetadata().getName().contains("qdrouter"))
                .collect(Collectors.toList()).get(0).getMetadata().getName();
        assertTrue(KubeCMDClient.runQDstat(qdRouterName, "-c", "--sasl-username=jenda", "--sasl-password=cenda").getRetCode());
        assertTrue(KubeCMDClient.runQDstat(qdRouterName, "-a", "--sasl-username=jenda", "--sasl-password=cenda").getRetCode());
        assertTrue(KubeCMDClient.runQDstat(qdRouterName, "-l", "--sasl-username=jenda", "--sasl-password=cenda").getRetCode());
    }

    @Test
    @Tag(NON_PR)
    void testMessagingDuringRestartComponents() throws Exception {
        List<Label> labels = new LinkedList<>();
        labels.add(new Label("component", "api-server"));
        labels.add(new Label("name", "address-space-controller"));
        labels.add(new Label("name", "enmasse-operator"));

        UserCredentials user = new UserCredentials("frantisek", "dobrota");
        AddressSpace standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("addr-space-restart-standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        AddressSpace brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("addr-space-restart-brokered")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpaceList(standard, brokered);
        resourcesManager.createOrUpdateUser(brokered, user);
        resourcesManager.createOrUpdateUser(standard, user);

        List<Address> brokeredAddresses = getAllBrokeredAddresses(brokered);
        List<Address> standardAddresses = getAllStandardAddresses(standard);

        resourcesManager.setAddresses(brokeredAddresses.toArray(new Address[0]));
        resourcesManager.setAddresses(standardAddresses.toArray(new Address[0]));

        getClientUtils().assertCanConnect(brokered, user, brokeredAddresses, resourcesManager);
        getClientUtils().assertCanConnect(standard, user, standardAddresses, resourcesManager);

        log.info("------------------------------------------------------------");
        log.info("------------------- Start with restating -------------------");
        log.info("------------------------------------------------------------");

        List<Pod> pods = kubernetes.listPods();
        int runningPodsBefore = pods.size();
        log.info("Number of running pods before restarting any: {}", runningPodsBefore);

        try {
            for (Address addr : brokeredAddresses) {
                log.info("Starting messaging in address {} and address space {}", addr.getSpec().getAddress(), brokered.getMetadata().getName());
                for (Label label : labels) {
                    getClientUtils().assertCanConnect(brokered, user, brokeredAddresses, resourcesManager);
                    doMessagingDuringRestart(label, runningPodsBefore, user, brokered, addr);
                    getClientUtils().assertCanConnect(brokered, user, brokeredAddresses, resourcesManager);
                }
            }

            for (Address addr : standardAddresses) {
                log.info("Starting messaging in address {} and address space {}", addr.getSpec().getAddress(), standard.getMetadata().getName());
                for (Label label : labels) {
                    getClientUtils().assertCanConnect(standard, user, standardAddresses, resourcesManager);
                    doMessagingDuringRestart(label, runningPodsBefore, user, standard, addr);
                    getClientUtils().assertCanConnect(standard, user, standardAddresses, resourcesManager);
                }
            }

        } finally {
            // Ensure that EnMasse's API services are finished re-registering (after api-server restart) before ending
            // the test otherwise test clean-up will fail.
            assertWaitForValue(true, () -> KubeCMDClient.getApiServices("v1beta1.enmasse.io").getRetCode(), new TimeoutBudget(90, TimeUnit.SECONDS));
        }

    }

    /////////////////////////////////////////////////////////////////////
    // help methods
    /////////////////////////////////////////////////////////////////////

    private void assertSystemWorks(AddressSpace brokered, AddressSpace standard, UserCredentials existingUser,
                                   List<Address> brAddresses, List<Address> stAddresses) throws Exception {
        log.info("Check if system works");
        getClientUtils().assertCanConnect(standard, existingUser, stAddresses, resourcesManager);
        getClientUtils().assertCanConnect(brokered, existingUser, brAddresses, resourcesManager);
        resourcesManager.getAddressSpace(brokered.getMetadata().getName());
        resourcesManager.getAddressSpace(standard.getMetadata().getName());
        resourcesManager.createOrUpdateUser(brokered, new UserCredentials("jenda", "cenda"));
        resourcesManager.createOrUpdateUser(standard, new UserCredentials("jura", "fura"));
    }

    private void doMessagingDuringRestart(Label label, int runningPodsBefore, UserCredentials user, AddressSpace space, Address addr) throws Exception {
        log.info("Starting messaging");
        AddressType addressType = AddressType.getEnum(addr.getSpec().getType());
        AmqpClient client = getAmqpClientFactory().createAddressClient(space, addressType);
        client.getConnectOptions().setCredentials(user);

        var restart = new CompletableFuture<Object>();

        var recvFut = client.recvMessages(addr.getSpec().getAddress(), msg -> {
            log.info("Message received");
            if (restart.isDone()) {
                return true;
            }
            return false;
        });


        var sendFut = client.sendMessages(addr.getSpec().getAddress(),
                Stream.generate(() -> UUID.randomUUID().toString()).limit(1000).collect(Collectors.toList()),
                msg -> {
                    if (restart.isDone()) {
                        return true;
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        log.error("Error waiting between sends", e);
                        restart.completeExceptionally(e);
                        return true;
                    }
                    return false;
                });

        log.info("Restarting {}", label.labelValue);
        KubeCMDClient.deletePodByLabel(label.getLabelName(), label.getLabelValue());
        TestUtils.waitForExpectedReadyPods(kubernetes, kubernetes.getInfraNamespace(), runningPodsBefore, new TimeoutBudget(10, TimeUnit.MINUTES));
        if (restart.isCompletedExceptionally()) {
            restart.get();
        }
        restart.complete(new Object());

        int received = recvFut.get(10, TimeUnit.SECONDS).size();
        int sent = sendFut.get(10, TimeUnit.SECONDS);
        assertEquals(sent, received, "Missmatch between messages sent and received");
    }

    private class Label {
        String labelName;
        String labelValue;

        Label(String labelName, String labelValue) {
            this.labelName = labelName;
            this.labelValue = labelValue;
        }

        String getLabelName() {
            return labelName;
        }

        String getLabelValue() {
            return labelValue;
        }
    }

}

