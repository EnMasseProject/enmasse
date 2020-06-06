/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingAddressBuilder;
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingEndpointPort;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.annotations.DefaultMessagingTenant;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedSharedInfra;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.ISOLATED_SHARED_INFRA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(ISOLATED_SHARED_INFRA)
@DefaultMessagingInfrastructure
@DefaultMessagingTenant
@ExternalClients
public class MessagingAddressTest extends TestBase implements ITestIsolatedSharedInfra {

    private MessagingTenant tenant;
    private MessagingEndpoint endpoint;

    @BeforeAll
    public void createEndpoint() {
        tenant = infraResourceManager.getDefaultMessagingTenant();
        endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewTls()
                .editOrNewSelfsigned()
                .endSelfsigned()
                .endTls()
                .editOrNewCluster()
                .endCluster()
                .addToProtocols("AMQP", "AMQPS")
                .endSpec()
                .build();
        infraResourceManager.createResource(endpoint);
    }

    @Test
    public void testAnycast() throws Exception {
        infraResourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("addr1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewAnycast()
                .endAnycast()
                .endSpec()
                .build());
        doTestSendReceive("addr1", "addr1");
    }

    @Test
    public void testMulticast() throws Exception {
        infraResourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("multicast1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewMulticast()
                .endMulticast()
                .endSpec()
                .build());
        doTestSendReceive(true, "multicast1", "multicast1", "multicast1", "multicast1");
    }

    @Test
    public void testQueue() throws Exception {
        infraResourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("queue1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewQueue()
                .endQueue()
                .endSpec()
                .build());
        doTestSendReceive("queue1", "queue1");
    }

    @Test
    public void testTopic() throws Exception {
        infraResourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("topic1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewTopic()
                .endTopic()
                .endSpec()
                .build());
        doTestSendReceive(true, "topic1", "topic1", "topic1", "topic1");
    }

    @Test
    public void testSubscription() throws Exception {
        infraResourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("topic1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewTopic()
                .endTopic()
                .endSpec()
                .build(),
                new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("sub1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewSubscription()
                .withTopic("topic1")
                .endSubscription()
                .endSpec()
                .build(),
                new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("sub2")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewSubscription()
                .withTopic("topic1")
                .endSubscription()
                .endSpec()
                .build());
        doTestSendReceive("topic1", "sub1", "sub2");
    }

    /**
     * Send 10 messages on sender address, and receive 10 messages on each receiver address.
     */
    void doTestSendReceive(boolean waitReceivers, String senderAddress, String ... receiverAddresses) throws Exception {
        int expectedMsgCount = 10;

        Endpoint e = new Endpoint(endpoint.getStatus().getHost(), getPort("AMQP", endpoint));
        ExternalMessagingClient senderClient = new ExternalMessagingClient(false)
                .withClientEngine(new RheaClientSender())
                .withMessagingRoute(e)
                .withAddress(senderAddress)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS")
                .withTimeout(60);

        List<ExternalMessagingClient> receiverClients = new ArrayList<>();
        for (String receiverAddress : receiverAddresses) {
            receiverClients.add(new ExternalMessagingClient(false)
                    .withClientEngine(new RheaClientReceiver())
                    .withMessagingRoute(e)
                    .withAddress(receiverAddress)
                    .withCount(expectedMsgCount)
                    .withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS")
                    .withTimeout(60));
        }

        List<Future<Boolean>> receiverResults = new ArrayList<>();
        for (ExternalMessagingClient receiverClient : receiverClients) {
            receiverResults.add(ForkJoinPool.commonPool().submit((Callable<Boolean>) receiverClient::run));
        }

        if (waitReceivers) {
            // To ensure receivers are attached and ready
            Thread.sleep(10_000);
        }

        Future<Boolean> senderResult = ForkJoinPool.commonPool().submit((Callable<Boolean>) senderClient::run);

        assertTrue(senderResult.get(1, TimeUnit.MINUTES), "Sender failed, expected return code 0");
        for (Future<Boolean> receiverResult : receiverResults) {
            assertTrue(receiverResult.get(1, TimeUnit.MINUTES), "Receiver failed, expected return code 0");
        }

        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        for (ExternalMessagingClient receiverClient : receiverClients) {
            assertEquals(expectedMsgCount, receiverClient.getMessages().size(),
                    String.format("Expected %d received messages", expectedMsgCount));
        }
    }

    void doTestSendReceive(String senderAddress, String ... receiverAddresses) throws Exception {
        doTestSendReceive(false, senderAddress, receiverAddresses);
    }

    private static int getPort(String protocol, MessagingEndpoint endpoint) {
        for (MessagingEndpointPort port : endpoint.getStatus().getPorts()) {
            if (protocol.equals(port.getProtocol()))  {
                return port.getPort();
            }
        }
        return 0;
    }
}
