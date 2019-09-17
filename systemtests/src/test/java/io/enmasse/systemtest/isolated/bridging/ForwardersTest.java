/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.bridging;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpecForwarderBuilder;
import io.enmasse.address.model.AddressSpecForwarderDirection;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.bridging.BridgingBase;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;

public class ForwardersTest extends BridgingBase {

    //tested usecases
    //Forwarding messages from a local queue in a local address space to a destination on a remote AMQP endpoint
    //Forwarding messages to a local queue in a local address space from a destination on a remote AMQP endpoint

    //forward to FULL remote queue
    //forward from FULL remote queue

    private static final String REMOTE_QUEUE1 = "queue1";
    private static final String REMOTE_QUEUE2 = "queue2";

    @Override
    protected String[] remoteBrokerQueues() {
        return new String[] {REMOTE_QUEUE1, REMOTE_QUEUE2};
    }

    @Test
    public void testForwardToRemoteQueue() throws Exception {
        AddressSpace space = createAddressSpace("forward-to-remote", "*");
        Address forwarder = new AddressBuilder()
                .withNewMetadata()
                .withName(AddressUtils.generateAddressMetadataName(space, "forwarder-queue1"))
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withAddress("forwarder-queue1")
                .withType(AddressType.QUEUE.toString())
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .addToForwarders(new AddressSpecForwarderBuilder()
                        .withName("forwarder1")
                        .withRemoteAddress(REMOTE_NAME + "/" + REMOTE_QUEUE1)
                        .withDirection(AddressSpecForwarderDirection.out)
                        .build())
                .endSpec()
                .build();
        resourcesManager.setAddresses(forwarder);
        AddressUtils.waitForForwardersReady(new TimeoutBudget(1, TimeUnit.MINUTES), forwarder);

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        int messagesBatch = 50;

        doTestSendToForwarder(space, forwarder, localUser, REMOTE_QUEUE1, messagesBatch);

    }

    @Test
    public void testForwardFromRemoteQueue() throws Exception {
        AddressSpace space = createAddressSpace("forward-from-remote", "*");
        Address forwarder = new AddressBuilder()
                .withNewMetadata()
                .withName(AddressUtils.generateAddressMetadataName(space, "forwarder-queue1"))
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withAddress("forwarder-queue1")
                .withType(AddressType.QUEUE.toString())
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .addToForwarders(new AddressSpecForwarderBuilder()
                        .withName("forwarder1")
                        .withRemoteAddress(REMOTE_NAME + "/" + REMOTE_QUEUE1)
                        .withDirection(AddressSpecForwarderDirection.in)
                        .build())
                .endSpec()
                .build();
        resourcesManager.setAddresses(forwarder);
        AddressUtils.waitForForwardersReady(new TimeoutBudget(1, TimeUnit.MINUTES), forwarder);

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        int messagesBatch = 50;

        //send to remote broker

        AmqpClient clientToRemote = createClientToRemoteBroker();

        clientToRemote.sendMessages(REMOTE_QUEUE1, TestUtils.generateMessages(messagesBatch));

        //receive in address with forwarder

        AmqpClient localClient = getAmqpClientFactory().createQueueClient(space);
        localClient.getConnectOptions().setCredentials(localUser);

        var receivedInRemote = localClient.recvMessages(forwarder.getSpec().getAddress(), messagesBatch);

        assertThat("Wrong count of messages received in local address: "+forwarder.getSpec().getAddress(), receivedInRemote.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));

    }

    @Test
    public void testForwardToUnavailableBroker() throws Exception {

        AddressSpace space = createAddressSpace("forward-to-remote", "*");
        Address forwarder = new AddressBuilder()
                .withNewMetadata()
                .withName(AddressUtils.generateAddressMetadataName(space, "forwarder-queue1"))
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withAddress("forwarder-queue1")
                .withType(AddressType.QUEUE.toString())
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .addToForwarders(new AddressSpecForwarderBuilder()
                        .withName("forwarder1")
                        .withRemoteAddress(REMOTE_NAME + "/" + REMOTE_QUEUE1)
                        .withDirection(AddressSpecForwarderDirection.out)
                        .build())
                .endSpec()
                .build();
        resourcesManager.setAddresses(forwarder);
        AddressUtils.waitForForwardersReady(new TimeoutBudget(1, TimeUnit.MINUTES), forwarder);

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        doTestSendToForwarder(space, forwarder, localUser, REMOTE_QUEUE1, 5);

        //make broker unavailable
        scaleDownBroker();

        //check address forwarder is not ready
//        Assertions.assertThrows(IllegalStateException.class, () -> {
//            AddressUtils.waitForForwardersReady(new TimeoutBudget(30, TimeUnit.SECONDS), forwarder);
//        });
        AddressUtils.waitForDestinationsReady(new TimeoutBudget(30, TimeUnit.SECONDS), forwarder);

        //send to forwarder
        int messagesBatch = 50;
        AmqpClient localClient = getAmqpClientFactory().createQueueClient(space);
        localClient.getConnectOptions().setCredentials(localUser);

        localClient.sendMessages(forwarder.getSpec().getAddress(), TestUtils.generateMessages(messagesBatch));

        //wake up the broker
        scaleUpBroker();

        //wait until forwarder is ready again
        AddressUtils.waitForDestinationsReady(new TimeoutBudget(30, TimeUnit.SECONDS), forwarder);
        AddressUtils.waitForForwardersReady(new TimeoutBudget(1, TimeUnit.MINUTES), forwarder);

        //wait a bit for the forwarding to happen
        Thread.sleep(10000);

        //check messages where automatically forwarded once broker is back up again
        AmqpClient clientToRemote = createClientToRemoteBroker();

        var receivedInRemote = clientToRemote.recvMessages(REMOTE_QUEUE1, messagesBatch);

        assertThat("Wrong count of messages received from remote queue: "+REMOTE_QUEUE1, receivedInRemote.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
    }

    private void doTestSendToForwarder(AddressSpace space, Address forwarder, UserCredentials localUser, String rometeAddress, int messagesBatch) throws Exception {
        //send to address with forwarder

        AmqpClient localClient = getAmqpClientFactory().createQueueClient(space);
        localClient.getConnectOptions().setCredentials(localUser);

        localClient.sendMessages(forwarder.getSpec().getAddress(), TestUtils.generateMessages(messagesBatch));

        //receive in remote broker

        AmqpClient clientToRemote = createClientToRemoteBroker();

        var receivedInRemote = clientToRemote.recvMessages(rometeAddress, messagesBatch);

        assertThat("Wrong count of messages received from remote queue: "+rometeAddress, receivedInRemote.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
    }

}
