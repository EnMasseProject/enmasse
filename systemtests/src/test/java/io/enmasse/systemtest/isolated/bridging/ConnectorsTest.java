/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.bridging;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorAddressRuleBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorCredentialsBuilder;
import io.enmasse.address.model.AddressSpaceSpecConnectorEndpointBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpConnectOptions;
import io.enmasse.systemtest.amqp.QueueTerminusFactory;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.shared.standard.QueueTest;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;

@Tag(TestTag.ISOLATED)
public class ConnectorsTest extends TestBase implements ITestIsolatedStandard{

    //tested usecases
    //Sending messages to a remote AMQP endpoint via a local address space - by creating a connector and using prefixing
    //Receiving messages from a remote AMQP endpoint via a local address space - by creating a connector and using prefixing
    //If I config a connector to refer to a host that does not exist, I'd expect the addressspace overall to report ready true, whereas the connector's status should report the failure.

    private static final String REMOTE_NAME = "remote1";
    private static final String BASIC_QUEUE1 = "basic1";
    private static final String BASIC_QUEUE2 = "basic2";
    private static final String SLASHED_QUEUE1 = "dummy/foo";
    private static final String SLASHED_QUEUE2 = "dummy/baz";
    private static final String BASIC_QUEUES_PATTERN = "*";
    private static final String SLASHED_QUEUES_PATTERN = "dummy/*";

    private static Logger log = CustomLogger.getLogger();

    private final String remoteBrokerNamespace = "systemtests-external-broker";
    private final String remoteBrokerUsername = "test-user";
    private final String remoteBrokerPassword = "test-password";
    private Endpoint remoteBrokerEndpoint;

    @BeforeEach
    public void deployBroker() throws Exception {
        SystemtestsKubernetesApps.deployAMQBroker(remoteBrokerNamespace, remoteBrokerUsername, remoteBrokerPassword, SLASHED_QUEUE1, SLASHED_QUEUE2, BASIC_QUEUE1, BASIC_QUEUE2);
        remoteBrokerEndpoint = SystemtestsKubernetesApps.getAMQBrokerEndpoint(remoteBrokerNamespace);
        log.info("Broker endpoint: {}", remoteBrokerEndpoint);
    }

    @AfterEach
    public void undeployBroker(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectLogsOfPodsInNamespace(remoteBrokerNamespace);
            logCollector.collectEvents(remoteBrokerNamespace);
        }
        SystemtestsKubernetesApps.deleteAMQBroker(remoteBrokerNamespace);
    }

    @Test
    public void testBrokerDeployment() throws Exception {
        AmqpClient client = createClientToRemoteBroker();

        Address testQueue = new AddressBuilder()
                .withNewMetadata()
                .withName("testtt")
                .endMetadata()
                .withNewSpec()
                .withAddress(SLASHED_QUEUE1)
                .endSpec()
                .build();
        QueueTest.runQueueTest(client, testQueue);
        client.close();
    }

    @Test
    public void testSendThroughConnector1() throws Exception {
        doTestSendThroughConnector(BASIC_QUEUES_PATTERN, new String[] {BASIC_QUEUE1, BASIC_QUEUE2});
    }

    @Test
    public void testSendThroughConnector2() throws Exception {
        doTestSendThroughConnector(SLASHED_QUEUES_PATTERN, new String [] {SLASHED_QUEUE1, SLASHED_QUEUE2});
    }

    @Test
    public void testReceiveThroughConnector1() throws Exception {
        doTestReceiveThroughConnector(BASIC_QUEUES_PATTERN, new String[] {BASIC_QUEUE1, BASIC_QUEUE2});
    }

    @Test
    public void testReceiveThroughConnector2() throws Exception {
        doTestReceiveThroughConnector(SLASHED_QUEUES_PATTERN, new String [] {SLASHED_QUEUE1, SLASHED_QUEUE2});
    }

    @Test
    public void testBadConfiguration() throws Exception {
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

    private void doTestSendThroughConnector(String addressRule, String[] remoteQueues) throws Exception, InterruptedException, ExecutionException, TimeoutException {
        AddressSpace space = createAddressSpace("send-to-connector", addressRule);

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        int messagesBatch = 50;

        //send through connector

        AmqpClient localClient = getAmqpClientFactory().createQueueClient(space);
        localClient.getConnectOptions().setCredentials(localUser);

        for(String remoteQueue : remoteQueues) {
            String connectorQueue = REMOTE_NAME + "/" + remoteQueue;
            localClient.sendMessages(connectorQueue, TestUtils.generateMessages(messagesBatch));
        }

        //receive in remote broker

        AmqpClient clientToRemote = createClientToRemoteBroker();

        for(String remoteQueue : remoteQueues) {
            var receivedFromQueue = clientToRemote.recvMessages(remoteQueue, messagesBatch);
            assertThat("Wrong count of messages received from queue: "+remoteQueue, receivedFromQueue.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
        }

    }

    private void doTestReceiveThroughConnector(String addressRule, String[] remoteQueues) throws Exception, InterruptedException, ExecutionException, TimeoutException {
        AddressSpace space = createAddressSpace("receive-from-connector", addressRule);

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        int messagesBatch = 50;

        //send to remote broker

        AmqpClient clientToRemote = createClientToRemoteBroker();

        for(String remoteQueue : remoteQueues) {
            clientToRemote.sendMessages(remoteQueue, TestUtils.generateMessages(messagesBatch));
        }

        //receive through connector

        AmqpClient localClient = getAmqpClientFactory().createQueueClient(space);
        localClient.getConnectOptions().setCredentials(localUser);

        for(String remoteQueue : remoteQueues) {
            String connectorQueue = REMOTE_NAME + "/" + remoteQueue;
            var receivedFromQueue = localClient.recvMessages(connectorQueue, messagesBatch);
            assertThat("Wrong count of messages received from connector queue: "+connectorQueue, receivedFromQueue.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
        }

    }

    private AddressSpace createAddressSpace(String name, String addressRule) throws Exception {
        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(name)
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
                                .withPattern(addressRule)
                                .build())
                        .build())
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(space);
        AddressSpaceUtils.waitForAddressSpaceConnectorsReady(space);
        return space;
    }

    private AmqpClient createClientToRemoteBroker() {

        ProtonClientOptions clientOptions = new ProtonClientOptions();
        clientOptions.setSsl(false);

        AmqpConnectOptions connectOptions = new AmqpConnectOptions()
                .setTerminusFactory(new QueueTerminusFactory())
                .setEndpoint(remoteBrokerEndpoint)
                .setProtonClientOptions(clientOptions)
                .setQos(ProtonQoS.AT_LEAST_ONCE)
                .setUsername(remoteBrokerUsername)
                .setPassword(remoteBrokerPassword);

        return getAmqpClientFactory().createClient(connectOptions);
    }

}
