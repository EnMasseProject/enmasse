/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.bridging;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceSpecConnectorCredentials;
import io.enmasse.address.model.AddressSpaceSpecConnectorTls;
import io.enmasse.address.model.AddressSpecForwarderBuilder;
import io.enmasse.address.model.AddressSpecForwarderDirection;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.bases.bridging.BridgingBase;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExternalClients
class ForwardersTest extends BridgingBase {

    private static Logger log = CustomLogger.getLogger();
    private static final String REMOTE_QUEUE1 = "queue1";

    @Test
    @Tag(ACCEPTANCE)
    void testForwardToRemoteQueue() throws Exception {
        doTestForwarderOut(null, defaultCredentials());
    }

    @Test
    void testForwardFromRemoteQueue() throws Exception {
        doTestForwarderIn(null, defaultCredentials());

    }

    @Test
    void testForwardToUnavailableBroker() throws Exception {

        AddressSpace space = createAddressSpace("forward-to-remote", "*", null, defaultCredentials());
        Address forwarder = new AddressBuilder()
                .withNewMetadata()
                .withName(AddressUtils.generateAddressMetadataName(space, "forwarder-queue1"))
                .withNamespace(remoteBrokerNamespace)
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
        int messagesBatch = 20;
        ExternalMessagingClient localClient = new ExternalMessagingClient()
                .withClientEngine(new ProtonJMSClientSender())
                .withMessagingRoute(Objects.requireNonNull(AddressSpaceUtils.getServiceEndpointByName(space, "messaging", "amqps")))
                .withCount(messagesBatch)
                .withAddress(forwarder.getSpec().getAddress())
                .withCredentials(localUser);

        assertTrue(localClient.run());

        //wake up the broker
        scaleUpBroker();

        //wait until forwarder is ready again
        AddressUtils.waitForDestinationsReady(new TimeoutBudget(30, TimeUnit.SECONDS), forwarder);
        AddressSpaceUtils.waitForAddressSpaceConnectorsReady(space);
        AddressUtils.waitForForwardersReady(new TimeoutBudget(1, TimeUnit.MINUTES), forwarder);

        //check messages where automatically forwarded once broker is back up again
        ExternalMessagingClient clientToRemote = createOnClusterClientToRemoteBroker(new ProtonJMSClientReceiver(), messagesBatch)
                .withAddress(REMOTE_QUEUE1);

        var receivedInRemote = clientToRemote.run();

        assertTrue(receivedInRemote, "Wrong count of messages received from remote queue: " + REMOTE_QUEUE1);
    }

    @Test
    public void testForwarderTLSOut() throws Exception {
        doTestForwarderOut(defaultTls(), defaultCredentials());
    }

    @Test
    public void testForwarderMutualTLSOut() throws Exception {
        doTestForwarderOut(defaultMutualTls(), null);
    }

    @Test
    public void testForwarderTLSIn() throws Exception {
        doTestForwarderIn(defaultTls(), defaultCredentials());
    }

    @Test
    public void testForwarderMutualTLSIn() throws Exception {
        doTestForwarderIn(defaultMutualTls(), null);
    }

    @Test
    void testForwardSecretSettings() throws Exception {
        doTestForwarderOut(tlsInSecret(), credentialsInSecret());
    }

    @Test
    void testForwardToFullQueue() throws Exception {
        AddressSpace space = createAddressSpace("forward-to-full", "*", null, defaultCredentials());
        Address forwarder = new AddressBuilder()
                .withNewMetadata()
                .withName(AddressUtils.generateAddressMetadataName(space, "forwarder-queue1"))
                .withNamespace(remoteBrokerNamespace)
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

        //send until remote broker is full
        AmqpClient clientToRemote = createClientToRemoteBroker();
        byte[] bytes = new byte[1024];
        Random random = new Random();
        Message message = Message.Factory.create();
        random.nextBytes(bytes);
        message.setBody(new AmqpValue(new Data(new Binary(bytes))));
        message.setAddress(REMOTE_QUEUE1);


        Stream<Message> messageStream = Stream.generate(() -> message);
        int messagesSent = clientToRemote.sendMessagesCheckDelivery(REMOTE_QUEUE1, messageStream::iterator, protonDelivery -> protonDelivery.remotelySettled() && protonDelivery.getRemoteState().getType().equals(DeliveryState.DeliveryStateType.Rejected))
                .get(5, TimeUnit.MINUTES);

        int messagesBatch = 20;

        ExternalMessagingClient localClient = new ExternalMessagingClient()
                .withClientEngine(new ProtonJMSClientSender())
                .withMessagingRoute(Objects.requireNonNull(AddressSpaceUtils.getServiceEndpointByName(space, "messaging", "amqps")))
                .withCount(messagesBatch)
                .withAddress(forwarder.getSpec().getAddress())
                .withCredentials(localUser);

        //send to address with forwarder wich will retry forwarding indefinetly until remote broker is available
        assertTrue(localClient.run());

        //receive messages that was causing remote broker to block, and check that we also get the forwarded messages
        var receivedInDLQ = clientToRemote.recvMessages(REMOTE_QUEUE1, messagesSent + messagesBatch);
        assertThat("Wrong count of messages received on remote address after queue is full in remote broker", receivedInDLQ.get(5, TimeUnit.MINUTES).size(), is(messagesSent + messagesBatch));

    }

    private void doTestForwarderOut(AddressSpaceSpecConnectorTls tlsSettings, AddressSpaceSpecConnectorCredentials credentials) throws Exception {
        AddressSpace space = createAddressSpace("forward-to-remote", "*", tlsSettings, credentials);
        Address forwarder = new AddressBuilder()
                .withNewMetadata()
                .withName(AddressUtils.generateAddressMetadataName(space, "forwarder-queue1"))
                .withNamespace(remoteBrokerNamespace)
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

        int messagesBatch = 20;

        doTestSendToForwarder(space, forwarder, localUser, REMOTE_QUEUE1, messagesBatch);
    }

    private void doTestForwarderIn(AddressSpaceSpecConnectorTls tlsSettings, AddressSpaceSpecConnectorCredentials credentials) throws Exception {
        AddressSpace space = createAddressSpace("forward-from-remote", "*", tlsSettings, credentials);
        Address forwarder = new AddressBuilder()
                .withNewMetadata()
                .withName(AddressUtils.generateAddressMetadataName(space, "forwarder-queue1"))
                .withNamespace(remoteBrokerNamespace)
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

        int messagesBatch = 20;

        doTestReceiveInForwarder(space, forwarder, localUser, REMOTE_QUEUE1, messagesBatch);
    }

    private void doTestSendToForwarder(AddressSpace space, Address forwarder, UserCredentials localUser, String remoteAddress, int messagesBatch) throws Exception {
        //send to address with forwarder

        ExternalMessagingClient localClient = new ExternalMessagingClient()
                .withClientEngine(new ProtonJMSClientSender())
                .withMessagingRoute(Objects.requireNonNull(AddressSpaceUtils.getServiceEndpointByName(space, "messaging", "amqps")))
                .withCount(messagesBatch)
                .withAddress(forwarder.getSpec().getAddress())
                .withCredentials(localUser);

        assertTrue(localClient.run());

        //receive in remote broker

        ExternalMessagingClient clientToRemote = createOnClusterClientToRemoteBroker(new ProtonJMSClientReceiver(), messagesBatch)
                .withAddress(remoteAddress);

        var receivedInRemote = clientToRemote.run();

        assertTrue(receivedInRemote, "Wrong count of messages received from remote queue: " + remoteAddress);
    }

    private void doTestReceiveInForwarder(AddressSpace space, Address forwarder, UserCredentials localUser, String remoteAddress, int messagesBatch) throws Exception {
        //send to remote broker

        ExternalMessagingClient clientToRemote = createOnClusterClientToRemoteBroker(new ProtonJMSClientSender(), messagesBatch)
                .withAddress(remoteAddress);

        assertTrue(clientToRemote.run());

        //receive in address with forwarder

        ExternalMessagingClient localClient = new ExternalMessagingClient()
                .withClientEngine(new ProtonJMSClientSender())
                .withMessagingRoute(Objects.requireNonNull(AddressSpaceUtils.getServiceEndpointByName(space, "messaging", "amqps")))
                .withCount(messagesBatch)
                .withAddress(forwarder.getSpec().getAddress())
                .withCredentials(localUser);

        var receivedInRemote = localClient.run();

        assertTrue(receivedInRemote, "Wrong count of messages received in local address: " + forwarder.getSpec().getAddress());
    }


}
