/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Pod;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.isolated;
import static io.enmasse.systemtest.TestTag.nonPR;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag(isolated)
class CommonTest extends TestBase {
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
        createAddressSpace(standard);

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
        setAddresses(dest);

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

        kubernetes.listPods().forEach(pod -> kubernetes.getContainersFromPod(pod.getMetadata().getName()).forEach(container -> {
            String podName = pod.getMetadata().getName();
            String containerName = container.getName();
            log.info("Getting log from pod: {}, for container: {}", podName, containerName);
            String log = kubernetes.getLog(podName, containerName);
            assertFalse(log.isEmpty(), String.format("Log for pod %s container %s was unexpectedly empty", podName, containerName));
        }));
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
        createAddressSpaceList(standard, brokered);
        createOrUpdateUser(brokered, user);
        createOrUpdateUser(standard, user);

        List<Address> brokeredAddresses = getAllBrokeredAddresses(brokered);
        List<Address> standardAddresses = getAllStandardAddresses(standard);

        setAddresses(brokeredAddresses.toArray(new Address[0]));
        setAddresses(standardAddresses.toArray(new Address[0]));

        assertCanConnect(brokered, user, brokeredAddresses);
        assertCanConnect(standard, user, standardAddresses);

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
            TestUtils.waitForExpectedReadyPods(kubernetes, runningPodsBefore, new TimeoutBudget(10, TimeUnit.MINUTES));
            assertSystemWorks(brokered, standard, user, brokeredAddresses, standardAddresses);
        }

        log.info("Restarting whole enmasse");
        KubeCMDClient.deletePodByLabel("app", kubernetes.getEnmasseAppLabel());
        Thread.sleep(180_000);
        TestUtils.waitForExpectedReadyPods(kubernetes, runningPodsBefore, new TimeoutBudget(10, TimeUnit.MINUTES));
        AddressUtils.waitForDestinationsReady(new TimeoutBudget(10, TimeUnit.MINUTES),
                standardAddresses.toArray(new Address[0]));
        assertSystemWorks(brokered, standard, user, brokeredAddresses, standardAddresses);

        //TODO: Uncomment when #2127 will be fixedy

//        Pod qdrouter = pods.stream().filter(pod -> pod.getMetadata().getName().contains("qdrouter")).collect(Collectors.toList()).get(0);
//        kubernetes.deletePod(environment.namespace(), qdrouter.getMetadata().getName());
//        assertSystemWorks(brokered, standard, user, brokeredAddresses, standardAddresses);
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
        createAddressSpace(standard);
        createOrUpdateUser(standard, new UserCredentials("jenda", "cenda"));
        setAddresses(getAllStandardAddresses(standard).toArray(new Address[0]));

        String qdRouterName = TestUtils.listRunningPods(kubernetes, standard).stream()
                .filter(pod -> pod.getMetadata().getName().contains("qdrouter"))
                .collect(Collectors.toList()).get(0).getMetadata().getName();
        assertTrue(KubeCMDClient.runQDstat(qdRouterName, "-c", "--sasl-username=jenda", "--sasl-password=cenda").getRetCode());
        assertTrue(KubeCMDClient.runQDstat(qdRouterName, "-a", "--sasl-username=jenda", "--sasl-password=cenda").getRetCode());
        assertTrue(KubeCMDClient.runQDstat(qdRouterName, "-l", "--sasl-username=jenda", "--sasl-password=cenda").getRetCode());
    }

    @Test
    @Tag(nonPR)
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
        createAddressSpaceList(standard, brokered);
        createOrUpdateUser(brokered, user);
        createOrUpdateUser(standard, user);

        List<Address> brokeredAddresses = getAllBrokeredAddresses(brokered);
        List<Address> standardAddresses = getAllStandardAddresses(standard);

        setAddresses(brokeredAddresses.toArray(new Address[0]));
        setAddresses(standardAddresses.toArray(new Address[0]));

        assertCanConnect(brokered, user, brokeredAddresses);
        assertCanConnect(standard, user, standardAddresses);

        log.info("------------------------------------------------------------");
        log.info("------------------- Start with restating -------------------");
        log.info("------------------------------------------------------------");

        List<Pod> pods = kubernetes.listPods();
        int runningPodsBefore = pods.size();
        log.info("Number of running pods before restarting any: {}", runningPodsBefore);

        for (Address addr : brokeredAddresses) {
            log.info("Starting messaging in address {} and address space {}", addr.getSpec().getAddress(), brokered.getMetadata().getName());
            for (Label label : labels) {
                doMessagingDuringRestart(label, runningPodsBefore, user, brokered, addr);
            }
        }

        for (Address addr : standardAddresses) {
            log.info("Starting messaging in address {} and address space {}", addr.getSpec().getAddress(), standard.getMetadata().getName());
            for (Label label : labels) {
                doMessagingDuringRestart(label, runningPodsBefore, user, standard, addr);
            }
        }

    }

    @Test
    void testMessagePersistent() throws Exception {
        UserCredentials user = new UserCredentials("frantisek", "dobrota");
        AddressSpace standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("space-standard-persistent")
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
                .withName("space-brokered-persistent")
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

        createAddressSpaceList(standard, brokered);
        createOrUpdateUser(brokered, user);
        createOrUpdateUser(standard, user);

        Address brokeredQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(standard.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(brokered, "test-queue-brokered"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-brokered")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();

        Address standardQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(standard.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(standard, "test-queue-standard"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-standard")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();

        Address standardLargeQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(standard.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(standard, "test-large-queue-standard"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-large-queue-standard")
                .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                .endSpec()
                .build();

        Address standardXLargeQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(standard.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(standard, "test-xlarge-queue-standard"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-xlarge-queue-standard")
                .withPlan(DestinationPlan.STANDARD_XLARGE_QUEUE)
                .endSpec()
                .build();

        Address standardTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(standard.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(standard, "test-topic-standard"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic-standard")
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();

        Address standardSub = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(standard.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(standard, "test-sub-standard"))
                .endMetadata()
                .withNewSpec()
                .withType("subscription")
                .withAddress("test-sub-standard")
                .withTopic(standardTopic.getSpec().getAddress())
                .withPlan(DestinationPlan.STANDARD_SMALL_SUBSCRIPTION)
                .endSpec()
                .build();

        setAddresses(brokeredQueue, standardQueue, standardLargeQueue, standardXLargeQueue, standardTopic, standardSub);

        int podCount = kubernetes.listPods().size();

        sendDurableMessages(brokered, brokeredQueue, user, 100);
        sendDurableMessages(standard, standardQueue, user, 30);
        sendDurableMessages(standard, standardLargeQueue, user, 30);
        sendDurableMessages(standard, standardXLargeQueue, user, 30);

        AmqpClient client = amqpClientFactory.createTopicClient(standard);
        client.getConnectOptions().setCredentials(user);

        log.info("Subscribe first receiver");
        Future<List<Message>> recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(standardSub), 30);
        sendDurableMessages(standard, AddressUtils.getQualifiedSubscriptionAddress(standardSub), user, 30);
        assertThat("Wrong messages received: ", recvResults.get(1, TimeUnit.MINUTES).size(), is(30));
        sendDurableMessages(standard, AddressUtils.getQualifiedSubscriptionAddress(standardSub), user, 30);

        kubernetes.deletePod(kubernetes.getInfraNamespace(), Collections.singletonMap("role", "broker"));
        Thread.sleep(20_000);
        TestUtils.waitForExpectedReadyPods(kubernetes, podCount, new TimeoutBudget(10, TimeUnit.MINUTES));
        log.info("Broker pods restarted");

        // Seems that the service/route can sometimes not be immediately available despite the pod being Ready.
        assertConnectable(brokered, user);
        assertConnectable(standard, user);
        receiveDurableMessages(brokered, brokeredQueue, user, 100);
        receiveDurableMessages(standard, standardQueue, user, 30);
        receiveDurableMessages(standard, standardLargeQueue, user, 30);
        receiveDurableMessages(standard, standardXLargeQueue, user, 30);

        log.info("Subscribe receiver again");
        recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(standardSub), 30);
        assertThat("Wrong messages received: batch2", recvResults.get(1, TimeUnit.MINUTES).size(), is(30));
    }

    /////////////////////////////////////////////////////////////////////
    // help methods
    /////////////////////////////////////////////////////////////////////

    private void sendMessage(AmqpClient client, String address, AtomicInteger counter) throws Exception {
        counter.incrementAndGet();
        Future<Integer> sent = client.sendMessages(address, Collections.singletonList(UUID.randomUUID().toString()));
        log.info("Message sent");
        assertEquals(1, sent.get(15, TimeUnit.SECONDS));
    }

    private void assertSystemWorks(AddressSpace brokered, AddressSpace standard, UserCredentials existingUser,
                                   List<Address> brAddresses, List<Address> stAddresses) throws Exception {
        log.info("Check if system works");
        assertCanConnect(standard, existingUser, stAddresses);
        assertCanConnect(brokered, existingUser, brAddresses);
        getAddressSpace(brokered.getMetadata().getName());
        getAddressSpace(standard.getMetadata().getName());
        createOrUpdateUser(brokered, new UserCredentials("jenda", "cenda"));
        createOrUpdateUser(standard, new UserCredentials("jura", "fura"));
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

    private void doMessagingDuringRestart(Label label, int runningPodsBefore, UserCredentials user, AddressSpace brokered, Address addr) throws Exception {
        log.info("Starting messaging");
        AddressType addressType = AddressType.getEnum(addr.getSpec().getType());
        AmqpClient client = amqpClientFactory.createAddressClient(brokered, addressType);
        client.getConnectOptions().setCredentials(user);
        AtomicInteger counter = new AtomicInteger(0);
        CompletableFuture<Object> future = doConcurrentMessaging(client, addr.getSpec().getAddress(), counter);
        log.info("Restarting {}", label.labelValue);
        KubeCMDClient.deletePodByLabel(label.getLabelName(), label.getLabelValue());
        TestUtils.waitForExpectedReadyPods(kubernetes, runningPodsBefore, new TimeoutBudget(10, TimeUnit.MINUTES));
        if (future.isCompletedExceptionally()) {
            future.get();
        }
        future.complete(new Object());
        assertTrue(counter.get() > 1, "receive messages did not work");
    }

    private CompletableFuture<Object> doConcurrentMessaging(AmqpClient client, String address, AtomicInteger counter) {

        CompletableFuture<Object> resultPromise = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            client.recvMessages(address, msg -> {
                log.info("Message received");
                if (resultPromise.isDone()) {
                    return true;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    log.error("Error waiting between sends", e);
                    resultPromise.completeExceptionally(e);
                    return true;
                }
                CompletableFuture.runAsync(() -> {
                    try {
                        sendMessage(client, address, counter);
                    } catch (Exception e) {
                        log.error("Error sending message", e);
                        resultPromise.completeExceptionally(e);
                    }
                }, runnable -> new Thread(runnable).start());
                return false;
            });
            try {
                sendMessage(client, address, counter);
            } catch (Exception e) {
                log.error("Error sending message", e);
                resultPromise.completeExceptionally(e);
            }
        }, runnable -> new Thread(runnable).start());

        return resultPromise;
    }

    private void assertConnectable(AddressSpace space, UserCredentials user) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(1, TimeUnit.MINUTES);
        String name = space.getMetadata().getName();
        do {
            try {
                connectAddressSpace(space, user);
                log.info("Successfully connected to address space : {}", name);
                return;
            } catch (IOException e) {
                log.info("Failed to connect to address space: {} - {}", name, e.getMessage());
            }
            Thread.sleep(1000);
        } while (!budget.timeoutExpired());

        fail(String.format("Failed to assert address space %s connectable within timeout", name));
    }
}

