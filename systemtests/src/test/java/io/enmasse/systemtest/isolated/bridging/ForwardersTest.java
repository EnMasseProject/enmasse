/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.bridging;

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
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ForwardersTest extends BridgingBase {

    private static final String REMOTE_QUEUE1 = "queue1";

    @Test
    @Tag(ACCEPTANCE)
    void testForwardToRemoteQueue() throws Exception {
        doTestForwarderOut(false, false);
    }

    @Test
    void testForwardFromRemoteQueue() throws Exception {
        doTestForwarderIn(false, false);

    }

    @Test
    void testForwardToUnavailableBroker() throws Exception {

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

        //check connector and address forwarder is not ready
        AddressSpaceUtils.waitForAddressSpaceConnectorsNotReady(space);
        TestUtils.waitUntilCondition("Forwarders not ready", phase -> {
            try {
                AddressUtils.waitForForwardersReady(new TimeoutBudget(20, TimeUnit.SECONDS), forwarder);
                return false;
            } catch (Exception ex) {
                return ex instanceof IllegalStateException;
            }
        }, new TimeoutBudget(3, TimeUnit.MINUTES));
        //however address should be still ready
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
        AddressSpaceUtils.waitForAddressSpaceConnectorsReady(space);
        AddressUtils.waitForForwardersReady(new TimeoutBudget(1, TimeUnit.MINUTES), forwarder);

        //check messages where automatically forwarded once broker is back up again
        AmqpClient clientToRemote = createClientToRemoteBroker();

        var receivedInRemote = clientToRemote.recvMessages(REMOTE_QUEUE1, messagesBatch);

        assertThat("Wrong count of messages received from remote queue: " + REMOTE_QUEUE1, receivedInRemote.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
    }

    @Test
    public void testForwarderTLSOut() throws Exception {
        doTestForwarderOut(true, false);
    }

    @Test
    public void testForwarderMutualTLSOut() throws Exception {
        doTestForwarderOut(true, true);
    }

    @Test
    public void testForwarderTLSIn() throws Exception {
        doTestForwarderIn(true, false);
    }

    @Test
    public void testForwarderMutualTLSIn() throws Exception {
        doTestForwarderIn(true, true);
    }

    private void doTestForwarderOut(boolean tls, boolean mutualTls) throws Exception {
        AddressSpace space = createAddressSpace("forward-to-remote", "*", tls, mutualTls);
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

    private void doTestForwarderIn(boolean tls, boolean mutualTls) throws Exception {
        AddressSpace space = createAddressSpace("forward-from-remote", "*", tls, mutualTls);
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

        doTestReceiveInForwarder(space, forwarder, localUser, REMOTE_QUEUE1, messagesBatch);
    }

    private void doTestSendToForwarder(AddressSpace space, Address forwarder, UserCredentials localUser, String remoteAddress, int messagesBatch) throws Exception {
        //send to address with forwarder

        AmqpClient localClient = getAmqpClientFactory().createQueueClient(space);
        localClient.getConnectOptions().setCredentials(localUser);

        localClient.sendMessages(forwarder.getSpec().getAddress(), TestUtils.generateMessages(messagesBatch));

        //receive in remote broker

        AmqpClient clientToRemote = createClientToRemoteBroker();

        var receivedInRemote = clientToRemote.recvMessages(remoteAddress, messagesBatch);

        assertThat("Wrong count of messages received from remote queue: " + remoteAddress, receivedInRemote.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
    }

    private void doTestReceiveInForwarder(AddressSpace space, Address forwarder, UserCredentials localUser, String remoteAddress, int messagesBatch) throws Exception {
        //send to remote broker

        AmqpClient clientToRemote = createClientToRemoteBroker();

        clientToRemote.sendMessages(remoteAddress, TestUtils.generateMessages(messagesBatch));

        //receive in address with forwarder

        AmqpClient localClient = getAmqpClientFactory().createQueueClient(space);
        localClient.getConnectOptions().setCredentials(localUser);

        var receivedInRemote = localClient.recvMessages(forwarder.getSpec().getAddress(), messagesBatch);

        assertThat("Wrong count of messages received in local address: " + forwarder.getSpec().getAddress(), receivedInRemote.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
    }


}
