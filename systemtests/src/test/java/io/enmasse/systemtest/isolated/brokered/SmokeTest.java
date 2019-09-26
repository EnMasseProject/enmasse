/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.brokered;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedBrokered;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.shared.standard.QueueTest;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.NON_PR;
import static io.enmasse.systemtest.TestTag.SMOKE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Tag(NON_PR)
@Tag(SMOKE)
@Tag(ACCEPTANCE)
class SmokeTest extends TestBase implements ITestIsolatedBrokered {

    @Test
    void testAddressTypes() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("smoke-space-brokered")
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

        Address queueA = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "brokeredqueuea"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("brokeredqueueq")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);
        resourcesManager.setAddresses(queueA);
        UserCredentials cred = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        AmqpClient amqpQueueCli = getAmqpClientFactory().createQueueClient(addressSpace);
        amqpQueueCli.getConnectOptions().setCredentials(cred);
        QueueTest.runQueueTest(amqpQueueCli, queueA);
        amqpQueueCli.close();

        Address topicB = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "brokeredtopicb"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("brokeredtopicb")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();
        resourcesManager.setAddresses(topicB);

        AmqpClient amqpTopicCli = getAmqpClientFactory().createTopicClient(addressSpace);
        amqpTopicCli.getConnectOptions().setCredentials(cred);
        List<Future<List<Message>>> recvResults = Arrays.asList(
                amqpTopicCli.recvMessages(topicB.getSpec().getAddress(), 1000),
                amqpTopicCli.recvMessages(topicB.getSpec().getAddress(), 1000));

        List<String> msgsBatch = TestUtils.generateMessages(600);
        List<String> msgsBatch2 = TestUtils.generateMessages(400);

        assertAll("All senders should send all messages",
                () -> assertThat("Wrong count of messages sent: batch1",
                        amqpTopicCli.sendMessages(topicB.getSpec().getAddress(), msgsBatch).get(3, TimeUnit.MINUTES), is(msgsBatch.size())),
                () -> assertThat("Wrong count of messages sent: batch2",
                        amqpTopicCli.sendMessages(topicB.getSpec().getAddress(), msgsBatch2).get(3, TimeUnit.MINUTES), is(msgsBatch2.size())));

        assertAll("All receivers should receive all messages",
                () -> assertThat("Wrong count of messages received",
                        recvResults.get(0).get(3, TimeUnit.MINUTES).size(), is(msgsBatch.size() + msgsBatch2.size())),
                () -> assertThat("Wrong count of messages received",
                        recvResults.get(1).get(3, TimeUnit.MINUTES).size(), is(msgsBatch.size() + msgsBatch2.size())));
    }

    @Test
    @Tag(ACCEPTANCE)
    void testCreateDeleteAddressSpace() throws Exception {
        AddressSpace addressSpaceA = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("smoke-space-brokered-a")
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

        AddressSpace addressSpaceB = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("smoke-space-brokered-b")
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
        commonResourcesManager.createAddressSpaceList(addressSpaceA, addressSpaceB);

        Address queueA = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpaceA.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpaceA, "queue-a"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-a")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        Address queueB = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpaceB.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpaceB, "queue-b"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-b")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.setAddresses(queueA, queueB);
        UserCredentials user = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(addressSpaceA, user);

        AmqpClient amqpQueueCliA = getAmqpClientFactory().createQueueClient(addressSpaceA);
        amqpQueueCliA.getConnectOptions().setCredentials(user);
        QueueTest.runQueueTest(amqpQueueCliA, queueA);
        amqpQueueCliA.close();

        resourcesManager.createOrUpdateUser(addressSpaceB, user);

        AmqpClient amqpQueueCliC = getAmqpClientFactory().createQueueClient(addressSpaceB);
        amqpQueueCliC.getConnectOptions().setCredentials(user);
        QueueTest.runQueueTest(amqpQueueCliC, queueB);
        amqpQueueCliC.close();

        resourcesManager.deleteAddressSpace(addressSpaceA);

        QueueTest.runQueueTest(amqpQueueCliC, queueB);
    }
}
