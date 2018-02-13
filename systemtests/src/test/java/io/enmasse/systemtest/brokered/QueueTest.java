package io.enmasse.systemtest.brokered;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.BrokeredTestBase;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressResource;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import io.enmasse.systemtest.resources.AddressSpaceResource;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class QueueTest extends BrokeredTestBase {

    /**
     * related github issue: #387
     */
    @Test
    public void messageGroupTest() throws Exception {
        Destination dest = Destination.queue("messageGroupQueue", getDefaultPlan(AddressType.QUEUE));
        setAddresses(sharedAddressSpace, dest);

        AmqpClient client = amqpClientFactory.createQueueClient(sharedAddressSpace);

        int msgsCount = 20;
        int msgCountGroupA = 15;
        int msgCountGroupB = 5;
        List<Message> listOfMessages = new ArrayList<>();
        for (int i = 0; i < msgsCount; i++) {
            Message msg = Message.Factory.create();
            msg.setAddress(dest.getAddress());
            msg.setBody(new AmqpValue(dest.getAddress()));
            msg.setSubject("subject");
            msg.setGroupId(((i + 1) % 4 != 0) ? "group A" : "group B");
            listOfMessages.add(msg);
        }

        Future<List<Message>> receivedGroupA = client.recvMessages(dest.getAddress(), msgCountGroupA);
        Future<List<Message>> receivedGroupB = client.recvMessages(dest.getAddress(), msgCountGroupB);
        Thread.sleep(2000);

        Future<Integer> sent = client.sendMessages(dest.getAddress(),
                listOfMessages.toArray(new Message[listOfMessages.size()]));

        assertThat("Wrong count of messages sent", sent.get(1, TimeUnit.MINUTES), is(msgsCount));
        assertThat("Wrong count of messages received from group A",
                receivedGroupA.get(1, TimeUnit.MINUTES).size(), is(msgCountGroupA));
        assertThat("Wrong count of messages received from group A",
                receivedGroupB.get(1, TimeUnit.MINUTES).size(), is(msgCountGroupB));

        for (Message m : receivedGroupA.get()) {
            assertEquals("Group id is different", m.getGroupId(), "group A");
        }

        for (Message m : receivedGroupB.get()) {
            assertEquals("Group id is different", m.getGroupId(), "group B");
        }
    }

    //@Test disabled because new address-space plans are not accepted yet
    public void testCreateAddressSpacePlan() throws Exception {
        AddressPlan weakQueue = null;
        AddressPlan weakTopic = null;
        AddressSpacePlan weakPlan = null;
        try {
            //define address plans
            List<AddressResource> addressResources = Arrays.asList(new AddressResource("broker", 0.0));
            weakQueue = new AddressPlan("brokered-queue-weak", AddressType.QUEUE, addressResources);
            weakTopic = new AddressPlan("brokered-topic-weak", AddressType.TOPIC, addressResources);

            createAddressPlanConfig(weakQueue);
            createAddressPlanConfig(weakTopic);

            //define address space plan
            List<AddressSpaceResource> resources = Arrays.asList(new AddressSpaceResource("broker", 2.0, 9.0));
            List<AddressPlan> addressPlans = Arrays.asList(weakQueue, weakTopic);
            weakPlan = new AddressSpacePlan("weak-plan", "weak", "brokered-space", AddressSpaceType.BROKERED, resources, addressPlans);
            createAddressSpacePlanConfig(weakPlan);

            //create address space plan with new plan
            AddressSpace weakAddressSpace = new AddressSpace("weak-address-space", AddressSpaceType.BROKERED, weakPlan.getName());
            createAddressSpace(weakAddressSpace, AuthService.STANDARD.toString());

            final String queueName = "weak-queue";
            final String topicName = "weak-topic";
            setAddresses(weakAddressSpace,
                    Destination.queue(queueName, "brokered-queue-weak"),
                    Destination.topic(topicName, "brokered-topic-weak"));

            Future<List<Address>> getWeakQueue = getAddressesObjects(Optional.of(weakQueue.getName()));
            Future<List<Address>> getWeakTopic = getAddressesObjects(Optional.of(weakTopic.getName()));

            String assertMessage = "Queue plan wasn't set properly";

            assertEquals(assertMessage, getWeakQueue.get(20, TimeUnit.SECONDS).get(0).getPlan(), weakQueue.getName());
            assertEquals(assertMessage, getWeakTopic.get(20, TimeUnit.SECONDS).get(0).getPlan(), weakTopic.getName());
        } catch (Exception ex) {
            throw ex;
        } finally {
            //TODO: create new test base for tests with newly defined plans with After method for removing all
            // address(space) plans configs and appended plans from already existing address-space configs

            //removeAddressPlanConfig(weakQueue.getName()); TODO not implemented yet
            //removeAddressPlanConfig(weakTopic.getName()); TODO not implemented yet
            //removeAddressSpacePlanConfig(weakPlan.getConfigName()); TODO not implemented yet
        }

    }

    @Test
    public void testAppendNewAddressPlan() throws Exception {
        AddressPlan weakQueuePlan = null;
        AddressSpacePlan brokeredPlan = null;
        try {
            List<AddressResource> addressResources = Arrays.asList(new AddressResource("broker", 0.0));
            weakQueuePlan = new AddressPlan("brokered-queue-weak", AddressType.QUEUE, addressResources);
            createAddressPlanConfig(weakQueuePlan);

            brokeredPlan = getAddressSpacePlanConfig("brokered");
            appendAddressPlan(weakQueuePlan, brokeredPlan);

            setAddresses(Destination.queue("weak-queue", weakQueuePlan.getName()));

            Future<List<Address>> brokeredAddresses = getAddressesObjects(Optional.of("weak-queue"));
            assertThat("Queue plan wasn't set properly",
                    brokeredAddresses.get(20, TimeUnit.SECONDS).get(0).getPlan(), is(weakQueuePlan.getName()));
        } catch (Exception ex) {
            throw ex;
        } finally {
            //TODO: create new test base for tests with newly defined plans with After method for removing all
            // address(space) plans configs and appended plans from already existing address-space configs
            if (weakQueuePlan != null && brokeredPlan != null) {
                removeAddressPlan(weakQueuePlan, brokeredPlan);
                removeAddressPlanConfig(weakQueuePlan);
            }
        }

    }
}
