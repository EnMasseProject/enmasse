/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.bridging;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorAddressRuleBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorCredentialsBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorEndpointBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.bridging.BridgingBase;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ConnectorsTest extends BridgingBase {

    private static final String BASIC_QUEUE1 = "basic1";
    private static final String BASIC_QUEUE2 = "basic2";
    private static final String SLASHED_QUEUE1 = "dummy/foo";
    private static final String SLASHED_QUEUE2 = "dummy/baz";
    private static final String BASIC_QUEUES_PATTERN = "*";
    private static final String SLASHED_QUEUES_PATTERN = "dummy/*";

    @Test
    void testSendThroughConnector1() throws Exception {
        doTestSendThroughConnector(BASIC_QUEUES_PATTERN, new String[] {BASIC_QUEUE1, BASIC_QUEUE2});
    }

    @Test
    void testSendThroughConnector2() throws Exception {
        doTestSendThroughConnector(SLASHED_QUEUES_PATTERN, new String [] {SLASHED_QUEUE1, SLASHED_QUEUE2});
    }

    @Test
    void testReceiveThroughConnector1() throws Exception {
        doTestReceiveThroughConnector(BASIC_QUEUES_PATTERN, new String[] {BASIC_QUEUE1, BASIC_QUEUE2});
    }

    @Test
    void testReceiveThroughConnector2() throws Exception {
        doTestReceiveThroughConnector(SLASHED_QUEUES_PATTERN, new String [] {SLASHED_QUEUE1, SLASHED_QUEUE2});
    }

    @Test
    void testNonExsistingHost() throws Exception {
        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName("send-to-connector")
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withConnectors(new AddressSpaceSpecConnectorBuilder()
                        .withName(REMOTE_NAME)
                        .addToEndpointHosts(new AddressSpaceSpecConnectorEndpointBuilder()
                                .withHost("nonexistinghost.jeje.hola")
                                .withPort(8080)
                                .build())
                        .withCredentials(new AddressSpaceSpecConnectorCredentialsBuilder()
                                .withNewUsername()
                                    .withValue("dummy")
                                    .endUsername()
                                .withNewPassword()
                                    .withValue("dummy")
                                    .endPassword()
                                .build())
                        .addToAddresses(new AddressSpaceSpecConnectorAddressRuleBuilder()
                                .withName("queuesrule")
                                .withPattern(BASIC_QUEUES_PATTERN)
                                .build())
                        .build())
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(space);
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AddressSpaceUtils.waitForAddressSpaceConnectorsReady(space, new TimeoutBudget(1, TimeUnit.MINUTES));
        });
    }

    @Test
    void testInvalidConnectorName() throws Exception {
        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName("invalid-connector-name")
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withConnectors(new AddressSpaceSpecConnectorBuilder()
                        .withName("/ect/dhcp")
                        .addToEndpointHosts(new AddressSpaceSpecConnectorEndpointBuilder()
                                .withHost(remoteBrokerEndpoint.getHost())
                                .withPort(remoteBrokerEndpoint.getPort())
                                .build())
                        .withCredentials(new AddressSpaceSpecConnectorCredentialsBuilder()
                                .withNewUsername()
                                .withValue(remoteBrokerUsername)
                                .endUsername()
                                .withNewPassword()
                                .withValue(remoteBrokerPassword)
                                .endPassword()
                                .build())
                        .addToAddresses(new AddressSpaceSpecConnectorAddressRuleBuilder()
                                .withName("queuesrule")
                                .withPattern("*")
                                .build())
                        .build())
                .endSpec()
                .build();

        resourcesManager.createAddressSpace(space, false);
        TimeoutBudget budget = new TimeoutBudget(2, TimeUnit.MINUTES);
        AddressSpaceUtils.waitForAddressSpaceStatusMessage(space, "Invalid address space connector name", budget);
    }

    @Test
    void testInvalidAddressRulePattern() throws Exception {
        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName("invalid-connector-name")
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withConnectors(new AddressSpaceSpecConnectorBuilder()
                        .withName(REMOTE_NAME)
                        .addToEndpointHosts(new AddressSpaceSpecConnectorEndpointBuilder()
                                .withHost(remoteBrokerEndpoint.getHost())
                                .withPort(remoteBrokerEndpoint.getPort())
                                .build())
                        .withCredentials(new AddressSpaceSpecConnectorCredentialsBuilder()
                                .withNewUsername()
                                    .withValue(remoteBrokerUsername)
                                    .endUsername()
                                .withNewPassword()
                                    .withValue(remoteBrokerPassword)
                                    .endPassword()
                                .build())
                        .addToAddresses(new AddressSpaceSpecConnectorAddressRuleBuilder()
                                .withName("queuesrule")
                                .withPattern("queue*")
                                .build())
                        .build())
                .endSpec()
                .build();

        resourcesManager.createAddressSpace(space, false);
        TimeoutBudget budget = new TimeoutBudget(2, TimeUnit.MINUTES);
        AddressSpaceUtils.waitForAddressSpaceStatusMessage(space, "Invalid address space connector address rule", budget);
    }

    @Test
    void testRestartBroker() throws Exception {
        AddressSpace space = createAddressSpace("restart-broker", BASIC_QUEUES_PATTERN, null, defaultCredentials());

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        int messagesBatch = 20;
        String[] remoteQueues = new String [] {BASIC_QUEUE1};
        sendToConnectorReceiveInBroker(space, localUser, remoteQueues, messagesBatch);

        scaleDownBroker();
        AddressSpaceUtils.waitForAddressSpaceConnectorsNotReady(space);

        scaleUpBroker();
        AddressSpaceUtils.waitForAddressSpaceConnectorsReady(space);

        sendToConnectorReceiveInBroker(space, localUser, remoteQueues, messagesBatch);
        sendToBrokerReceiveInConnector(space, localUser, remoteQueues, messagesBatch);
    }

    @Test
    @Tag(ACCEPTANCE)
    public void testConnectorTLS() throws Exception {
        AddressSpace space = createAddressSpace("tls-test", BASIC_QUEUES_PATTERN, defaultTls(), defaultCredentials());

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        int messagesBatch = 20;
        String[] remoteQueues = new String [] {BASIC_QUEUE1};

        sendToConnectorReceiveInBroker(space, localUser, remoteQueues, messagesBatch);
        sendToBrokerReceiveInConnector(space, localUser, remoteQueues, messagesBatch);
    }

    @Test
    public void testConnectorMutualTLS() throws Exception {
        AddressSpace space = createAddressSpace("tls-test", BASIC_QUEUES_PATTERN, defaultMutualTls(), null);

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        int messagesBatch = 20;
        String[] remoteQueues = new String [] {BASIC_QUEUE1};

        sendToConnectorReceiveInBroker(space, localUser, remoteQueues, messagesBatch);
        sendToBrokerReceiveInConnector(space, localUser, remoteQueues, messagesBatch);
    }

    private void doTestSendThroughConnector(String addressRule, String[] remoteQueues) throws Exception, InterruptedException, ExecutionException, TimeoutException {
        AddressSpace space = createAddressSpace("send-to-connector", addressRule, null, defaultCredentials());

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        int messagesBatch = 20;

        sendToConnectorReceiveInBroker(space, localUser, remoteQueues, messagesBatch);
    }

    private void doTestReceiveThroughConnector(String addressRule, String[] remoteQueues) throws Exception {
        AddressSpace space = createAddressSpace("receive-from-connector", addressRule, null, defaultCredentials());

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        int messagesBatch = 20;

        sendToBrokerReceiveInConnector(space, localUser, remoteQueues, messagesBatch);
    }

    private void sendToConnectorReceiveInBroker(AddressSpace space, UserCredentials localUser, String[] remoteQueues, int messagesBatch) throws Exception {
        //send through connector
        AmqpClient localClient = getAmqpClientFactory().createQueueClient(space);
        localClient.getConnectOptions().setCredentials(localUser);

        for(String remoteQueue : remoteQueues) {
            String connectorQueue = getRemoteName(remoteQueue);
            localClient.sendMessages(connectorQueue, TestUtils.generateMessages(messagesBatch));
        }

        //receive in remote broker
        AmqpClient clientToRemote = createClientToRemoteBroker();

        for(String remoteQueue : remoteQueues) {
            var receivedFromQueue = clientToRemote.recvMessages(remoteQueue, messagesBatch);
            assertThat("Wrong count of messages received from queue: "+remoteQueue, receivedFromQueue.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
        }
    }

    private void sendToBrokerReceiveInConnector(AddressSpace space, UserCredentials localUser, String[] remoteQueues, int messagesBatch) throws Exception {
        //send to remote broker
        AmqpClient clientToRemote = createClientToRemoteBroker();

        for(String remoteQueue : remoteQueues) {
            clientToRemote.sendMessages(remoteQueue, TestUtils.generateMessages(messagesBatch));
        }

        //receive through connector
        AmqpClient localClient = getAmqpClientFactory().createQueueClient(space);
        localClient.getConnectOptions().setCredentials(localUser);

        for(String remoteQueue : remoteQueues) {
            String connectorQueue = getRemoteName(remoteQueue);
            var receivedFromQueue = localClient.recvMessages(connectorQueue, messagesBatch);
            assertThat("Wrong count of messages received from connector queue: "+connectorQueue, receivedFromQueue.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
        }
    }

    private String getRemoteName(String remoteQueue) {
        return REMOTE_NAME + "/" + remoteQueue;
    }

}
