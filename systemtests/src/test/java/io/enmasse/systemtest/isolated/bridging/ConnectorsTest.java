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

    private static Logger log = CustomLogger.getLogger();

    private final String remoteBrokerNamespace = "systemtests-external-broker";
    private final String remoteBrokerUsername = "test-user";
    private final String remoteBrokerPassword = "test-password";
    private Endpoint remoteBrokerEndpoint;
    private Endpoint remoteBrokerEndpointNoSSL;

    @BeforeEach
    public void deployBroker() throws Exception {
        SystemtestsKubernetesApps.deployAMQBroker(remoteBrokerNamespace, remoteBrokerUsername, remoteBrokerPassword);
        remoteBrokerEndpoint = SystemtestsKubernetesApps.getAMQBrokerSSLEndpoint(remoteBrokerNamespace);
        remoteBrokerEndpointNoSSL = SystemtestsKubernetesApps.getAMQBrokerEndpoint(remoteBrokerNamespace);
        log.info("Endpoints to remote broker:");
        log.info("Route with SSL: {}", remoteBrokerEndpoint);
        log.info("Service without SSL: {}", remoteBrokerEndpointNoSSL);
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
                .withAddress("queue1")
                .endSpec()
                .build();
        QueueTest.runQueueTest(client, testQueue);
        client.close();
    }

    @Test
    public void testBrokerDeploymentNonSSL() throws Exception {
        AmqpClient client = createClientToRemoteBrokerNoSSL();

        Address testQueue = new AddressBuilder()
                .withNewMetadata()
                .withName("testtt")
                .endMetadata()
                .withNewSpec()
                .withAddress("queue1")
                .endSpec()
                .build();
        QueueTest.runQueueTest(client, testQueue);
        client.close();
    }

    @Test
    public void testSendThroughConnector1() throws Exception {
        doTestSendThroughConnector("*");
    }

    @Test
    public void testSendThroughConnector2() throws Exception {
        doTestSendThroughConnector("queue/*");
    }

    @Test
    public void testReceiveThroughConnector1() throws Exception {
        doTestReceiveThroughConnector("*");
    }

    @Test
    public void testReceiveThroughConnector2() throws Exception {
        doTestReceiveThroughConnector("queue/*");
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
                        .withName("remote1")
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
                                .withPattern("*")
                                .build())
                        .build())
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(space);
        Assertions.assertThrows(IllegalStateException.class, () -> {
            AddressSpaceUtils.waitForAddressSpaceConnectorsReady(space, new TimeoutBudget(1, TimeUnit.MINUTES));
        });
    }

    private void doTestSendThroughConnector(String addressRule) throws Exception, InterruptedException, ExecutionException, TimeoutException {
        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName("send-to-connector")
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withConnectors(new AddressSpaceSpecConnectorBuilder()
                        .withName("remote1")
//                        .withNewTls()
//                        .endTls()
                        .addToEndpointHosts(new AddressSpaceSpecConnectorEndpointBuilder()
                                .withHost(remoteBrokerEndpointNoSSL.getHost())
                                .withPort(remoteBrokerEndpointNoSSL.getPort())
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

        Thread.sleep(30000);

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        int messagesBatch = 50;

        //send through connector

        AmqpClient localClient = getAmqpClientFactory().createQueueClient(space);
        localClient.getConnectOptions().setCredentials(localUser);

        localClient.sendMessages("remote1/queue1", TestUtils.generateMessages(messagesBatch));

        localClient.sendMessages("remote1/queue2", TestUtils.generateMessages(messagesBatch));

        //receive in remote broker

        AmqpClient clientToRemote = createClientToRemoteBroker();

        var receivedFromQueue1 = clientToRemote.recvMessages("queue1", messagesBatch);
        var receivedFromQueue2 = clientToRemote.recvMessages("queue2", messagesBatch);

        assertThat("Wrong count of messages received from queue1", receivedFromQueue1.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
        assertThat("Wrong count of messages received from queue2", receivedFromQueue2.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
    }

    private void doTestReceiveThroughConnector(String addressRule) throws Exception, InterruptedException, ExecutionException, TimeoutException {
        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName("receive-from-connector")
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withConnectors(new AddressSpaceSpecConnectorBuilder()
                        .withName("remote1")
//                        .withNewTls()
//                        .endTls()
                        .addToEndpointHosts(new AddressSpaceSpecConnectorEndpointBuilder()
                                .withHost(remoteBrokerEndpointNoSSL.getHost())
                                .withPort(remoteBrokerEndpointNoSSL.getPort())
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

        Thread.sleep(30000);

        UserCredentials localUser = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(space, localUser);

        int messagesBatch = 50;

        //send to remote broker

        AmqpClient clientToRemote = createClientToRemoteBroker();

        clientToRemote.sendMessages("remote1/queue1", TestUtils.generateMessages(messagesBatch));

        clientToRemote.sendMessages("remote1/queue2", TestUtils.generateMessages(messagesBatch));

        //receive through connector

        AmqpClient localClient = getAmqpClientFactory().createQueueClient(space);
        localClient.getConnectOptions().setCredentials(localUser);

        var receivedFromQueue1 = localClient.recvMessages("queue1", messagesBatch);
        var receivedFromQueue2 = localClient.recvMessages("queue2", messagesBatch);

        assertThat("Wrong count of messages received from queue1", receivedFromQueue1.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
        assertThat("Wrong count of messages received from queue2", receivedFromQueue2.get(1, TimeUnit.MINUTES).size(), is(messagesBatch));
    }

    private AmqpClient createClientToRemoteBroker() {
        ProtonClientOptions clientOptions = new ProtonClientOptions();
        clientOptions.setSsl(true);
        clientOptions.setTrustAll(true);
        clientOptions.setHostnameVerificationAlgorithm("");

        AmqpConnectOptions connectOptions = new AmqpConnectOptions()
                .setTerminusFactory(new QueueTerminusFactory())
                .setEndpoint(remoteBrokerEndpoint)
                .setProtonClientOptions(clientOptions)
                .setQos(ProtonQoS.AT_LEAST_ONCE)
                .setUsername(remoteBrokerUsername)
                .setPassword(remoteBrokerPassword);

        return getAmqpClientFactory().createClient(connectOptions);
    }

    private AmqpClient createClientToRemoteBrokerNoSSL() {

        ProtonClientOptions clientOptions = new ProtonClientOptions();
        clientOptions.setSsl(false);
        clientOptions.setTrustAll(true);
        clientOptions.setHostnameVerificationAlgorithm("");

        AmqpConnectOptions connectOptions = new AmqpConnectOptions()
                .setTerminusFactory(new QueueTerminusFactory())
                .setEndpoint(remoteBrokerEndpointNoSSL)
                .setProtonClientOptions(clientOptions)
                .setQos(ProtonQoS.AT_LEAST_ONCE)
                .setUsername(remoteBrokerUsername)
                .setPassword(remoteBrokerPassword);

        return getAmqpClientFactory().createClient(connectOptions);
    }

}
