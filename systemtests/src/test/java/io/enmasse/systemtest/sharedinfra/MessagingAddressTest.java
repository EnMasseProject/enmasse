/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingAddress;
import io.enmasse.api.model.MessagingAddressBuilder;
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpConnectOptions;
import io.enmasse.systemtest.amqp.QueueTerminusFactory;
import io.enmasse.systemtest.amqp.TopicTerminusFactory;
import io.enmasse.systemtest.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.annotations.DefaultMessagingTenant;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.MessagingClientRunner;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.enmasse.systemtest.messaginginfra.resources.MessagingEndpointResourceType.getPort;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DefaultMessagingInfrastructure
@DefaultMessagingTenant
@ExternalClients
public class MessagingAddressTest extends TestBase {

    private MessagingTenant tenant;
    private MessagingEndpoint endpoint;
    private MessagingClientRunner clientRunner = new MessagingClientRunner();
    List<ExternalMessagingClient> messagingClients;
    @BeforeAll
    public void createEndpoint() {
        tenant = resourceManager.getDefaultMessagingTenant();
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
        resourceManager.createResource(endpoint);
    }

    @Test
    public void testAnycast() throws Exception {
        resourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("addr1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewAnycast()
                .endAnycast()
                .endSpec()
                .build());
        messagingClients = clientRunner.sendAndReceive(endpoint, "addr1", "addr1");
        assertDefaultMessaging(messagingClients);
    }

    @Test
    public void testMulticast() throws Exception {
        resourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("multicast1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewMulticast()
                .endMulticast()
                .endSpec()
                .build());
        messagingClients = clientRunner.sendAndReceive(endpoint, true, "multicast1", "multicast1", "multicast1", "multicast1");
        assertDefaultMessaging(messagingClients);
    }

    @Test
    public void testQueue() throws Exception {
        resourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("queue1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewQueue()
                .endQueue()
                .endSpec()
                .build());
        messagingClients = clientRunner.sendAndReceive(endpoint, "queue1", "queue1");
        assertDefaultMessaging(messagingClients);
    }

    @Test
    public void testDeadLetterExpiry() throws Exception {
        resourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("dlq1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewDeadLetter()
                .endDeadLetter()
                .endSpec()
                .build());
        resourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("queue1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewQueue()
                .withExpiryAddress("dlq1")
                .endQueue()
                .endSpec()
                .build());

        messagingClients = clientRunner.sendAndReceive(endpoint, false,
                Collections.singletonMap(ClientArgument.MSG_TTL, "100"),
                null, "queue1", "dlq1");
        assertDefaultMessaging(messagingClients);
    }

    @Test
    public void testDeadLetterConsume() throws Exception {
        MessagingEndpoint ingress = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withName("dlq")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewNodePort()
                .endNodePort()
                .withHost(kubernetes.getHost())
                .addToProtocols("AMQP")
                .endSpec()
                .build();
        resourceManager.createResource(ingress);
        resourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("dlq1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewDeadLetter()
                .endDeadLetter()
                .endSpec()
                .build());
        resourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("queue1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewQueue()
                .withDeadLetterAddress("dlq1")
                .endQueue()
                .endSpec()
                .build());


        AmqpClient client = resourceManager.getAmqpClientFactory().createClient(new AmqpConnectOptions()
                .setSaslMechanism("ANONYMOUS")
                .setQos(ProtonQoS.AT_LEAST_ONCE)
                .setEndpoint(new Endpoint(ingress.getStatus().getHost(), ingress.getStatus().getPorts().get(0).getPort()))
                .setProtonClientOptions(new ProtonClientOptions())
                .setTerminusFactory(new QueueTerminusFactory()));

        assertEquals(1, client.sendMessages("queue1", Collections.singletonList("todeadletter")).get(1, TimeUnit.MINUTES));
        Source source = new Source();
        source.setAddress("queue1");
        AtomicInteger redeliveries = new AtomicInteger(0);
        assertEquals(1, client.recvMessages(source, message -> redeliveries.incrementAndGet() >= 1, Optional.empty(), protonDelivery -> protonDelivery.disposition(new Rejected(), true)).getResult().get(1, TimeUnit.MINUTES).size());
        assertEquals(1, client.recvMessages("dlq1", 1).get(1, TimeUnit.MINUTES).size());
    }

    @Test
    public void testTopic() throws Exception {
        resourceManager.createResource(new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withName("topic1")
                .withNamespace(tenant.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewTopic()
                .endTopic()
                .endSpec()
                .build());
        messagingClients = clientRunner.sendAndReceive(endpoint, true, "topic1", "topic1", "topic1", "topic1");
        assertDefaultMessaging(messagingClients);
    }

    @Test
    @Disabled("Topic support not yet implemented")
    void testTopicWildcards() throws Exception {
        MessagingEndpoint nodePort = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app-nodeport")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewNodePort()
                .endNodePort()
                .withHost(kubernetes.getHost())
                .addToProtocols("AMQP")
                .endSpec()
                .build();
        MessagingAddress t0 = new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("topic-wild")
                .endMetadata()
                .withNewSpec()
                .withAddress("topic-wild")
                .editOrNewTopic()
                .endTopic()
                .endSpec()
                .build();

        resourceManager.createResource(nodePort, t0);

        AmqpClient amqpClient = resourceManager.getAmqpClientFactory().createClient(new AmqpConnectOptions()
                .setSaslMechanism("ANONYMOUS")
                .setQos(ProtonQoS.AT_LEAST_ONCE)
                .setEndpoint(new Endpoint(nodePort.getStatus().getHost(), getPort("AMQP", nodePort)))
                .setProtonClientOptions(new ProtonClientOptions())
                .setTerminusFactory(new TopicTerminusFactory()));

        List<String> msgs = Arrays.asList("foo", "bar", "baz", "qux");

        Future<List<Message>> recvResults = amqpClient.recvMessages("topic-wild/#", msgs.size() * 3);

        amqpClient.sendMessages(t0.getSpec().getAddress() + "/foo", msgs);
        amqpClient.sendMessages(t0.getSpec().getAddress() + "/bar", msgs);
        amqpClient.sendMessages(t0.getSpec().getAddress() + "/baz/foobar", msgs);

        assertThat("Wrong count of messages received",
                recvResults.get(1, TimeUnit.MINUTES).size(), is(msgs.size() * 3));

        recvResults = amqpClient.recvMessages("topic-wild/world/+", msgs.size() * 2);

        amqpClient.sendMessages(t0.getSpec().getAddress() + "/world/africa", msgs);
        amqpClient.sendMessages(t0.getSpec().getAddress() + "/world/europe", msgs);
        amqpClient.sendMessages(t0.getSpec().getAddress() + "/world/asia/maldives", msgs);

        assertThat("Wrong count of messages received",
                recvResults.get(1, TimeUnit.MINUTES).size(), is(msgs.size() * 2));
    }

    @Test
    public void testSubscription() throws Exception {
        resourceManager.createResource(new MessagingAddressBuilder()
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

        messagingClients =clientRunner.sendAndReceive(endpoint,"topic1", "sub1", "sub2");
        assertDefaultMessaging(messagingClients);
    }

    private void assertDefaultMessaging(List<ExternalMessagingClient> clients) throws InterruptedException {
        int expectedMsgCount = 10;
        try {
            for (ExternalMessagingClient client : clients) {
                if (client.isSender()) {
                    assertEquals(expectedMsgCount, client.getMessages().size(),
                            String.format("Expected %d sent messages", expectedMsgCount));
                } else {
                    assertEquals(expectedMsgCount, client.getMessages().size(),
                            String.format("Expected %d received messages", expectedMsgCount));
                }
            }
        } finally {
            Logger LOGGER = CustomLogger.getLogger();
            clients.clear();
            clientRunner.shutdown_clients();
        }
    }
}
