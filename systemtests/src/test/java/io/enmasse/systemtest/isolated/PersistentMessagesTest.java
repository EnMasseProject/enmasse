/*
 * Copyright 2019, EnMasse authors.
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
import io.enmasse.systemtest.utils.MessagingUtils;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class PersistentMessagesTest extends TestBase implements ITestBaseIsolated {
    private static Logger log = CustomLogger.getLogger();
    private MessagingUtils clientUtils = new MessagingUtils();
    private UserCredentials credentials = new UserCredentials("test", "test");

    @Test
    @Tag(ACCEPTANCE)
    void testBrokeredPersistentMessages() throws Exception {
        AddressSpace brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("brokered-persistent")
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
        resourcesManager.createAddressSpace(brokered);
        resourcesManager.createOrUpdateUser(brokered, credentials);


        Address brokeredQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(brokered.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(brokered, "test-queue-brokered"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-brokered")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        doTestQueuePersistentMessages(brokered, brokeredQueue, 100);
    }

    @Test
    @Tag(ACCEPTANCE)
    void testStandardPersistentMessages() throws Exception {
        AddressSpace standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-persistent")
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
        resourcesManager.createOrUpdateUser(standard, credentials);

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
        doTestQueuePersistentMessages(standard, standardQueue, 30);

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
        doTestQueuePersistentMessages(standard, standardLargeQueue, 30);

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
        doTestQueuePersistentMessages(standard, standardXLargeQueue, 30);

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
        doTestTopicPersistentMessages(standard, standardTopic, standardSub);
    }

    private void doTestQueuePersistentMessages(AddressSpace addSpace, Address address, int messagesBatch) throws Exception {
        resourcesManager.setAddresses(address);

        // Wait for the first console pod to be terminated
        TestUtils.waitForConsoleRollingUpdate(kubernetes.getInfraNamespace());
        TestUtils.waitUntilDeployed(kubernetes.getInfraNamespace());

        int podCount = kubernetes.listPods().size();
        clientUtils.sendDurableMessages(resourcesManager, addSpace, address, credentials, messagesBatch);
        restartBrokers(podCount);

        // Seems that the service/route can sometimes not be immediately available despite the pod being Ready.
        assertConnectable(addSpace, credentials);
        clientUtils.receiveDurableMessages(resourcesManager, addSpace, address, credentials, messagesBatch);

    }

    private void doTestTopicPersistentMessages(AddressSpace addSpace, Address topic, Address subscription) throws Exception {
        resourcesManager.setAddresses(topic, subscription);

        int podCount = kubernetes.listPods().size();

        AmqpClient client = resourcesManager.getAmqpClientFactory().createTopicClient(addSpace);
        client.getConnectOptions().setCredentials(credentials);

        log.info("Subscribe first receiver");
        Future<List<Message>> recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription), 30);
        clientUtils.sendDurableMessages(resourcesManager, addSpace, topic, credentials, 30);
        assertThat("Wrong messages received: ", recvResults.get(1, TimeUnit.MINUTES).size(), is(30));
        clientUtils.sendDurableMessages(resourcesManager, addSpace, topic, credentials, 30);

        restartBrokers(podCount);

        // Seems that the service/route can sometimes not be immediately available despite the pod being Ready.
        assertConnectable(addSpace, credentials);

        log.info("Subscribe receiver again");
        recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription), 30);
        assertThat("Wrong messages received: batch2", recvResults.get(1, TimeUnit.MINUTES).size(), is(30));
    }

    private void restartBrokers(int podCount) throws Exception {
        Map<String, String> labelSelector = Collections.singletonMap("role", "broker");
        List<String> uids = kubernetes.listPods(kubernetes.getInfraNamespace(), labelSelector).stream().map(pod -> pod.getMetadata().getUid()).collect(Collectors.toList());

        kubernetes.deletePod(kubernetes.getInfraNamespace(), labelSelector);

        waitForPodsToTerminate(uids);
        TestUtils.waitForExpectedReadyPods(kubernetes, kubernetes.getInfraNamespace(), podCount, new TimeoutBudget(10, TimeUnit.MINUTES));
        log.info("Broker pods restarted");
    }

    private void assertConnectable(AddressSpace space, UserCredentials user) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(1, TimeUnit.MINUTES);
        String name = space.getMetadata().getName();
        do {
            try {
                clientUtils.connectAddressSpace(resourcesManager, space, user);
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
