/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingAddress;
import io.enmasse.api.model.MessagingAddressBuilder;
import io.enmasse.api.model.MessagingAddressCondition;
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingEndpointCondition;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpConnectOptions;
import io.enmasse.systemtest.amqp.QueueTerminusFactory;
import io.enmasse.systemtest.annotations.DefaultMessagingInfra;
import io.enmasse.systemtest.annotations.DefaultMessagingTenant;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedSharedInfra;
import io.enmasse.systemtest.condition.Kubernetes;
import io.enmasse.systemtest.condition.OpenShift;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.messaginginfra.resources.MessagingAddressResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingEndpointResourceType;
import io.enmasse.systemtest.platform.cluster.ClusterType;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DefaultMessagingInfra
public class MessagingEndpointTest extends TestBase implements ITestIsolatedSharedInfra {

    @Test
    @DefaultMessagingTenant
    @Kubernetes(type = ClusterType.MINIKUBE)
    public void testNodePortMinikube() throws InterruptedException, ExecutionException, TimeoutException {
        testNodePort();
    }

    @Test
    @DefaultMessagingTenant
    @OpenShift(type = ClusterType.CRC)
    public void testNodePortCRC() throws InterruptedException, ExecutionException, TimeoutException {
        testNodePort();
    }

    private void testNodePort() throws ExecutionException, InterruptedException, TimeoutException {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();

        MessagingAddress address = new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("queue1")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewQueue()
                .endQueue()
                .endSpec()
                .build();

        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
                .endMetadata()
                .editOrNewSpec()
                .withHost(kubernetes.getHost())
                .addToProtocols("AMQP")
                .editOrNewNodePort()
                .endNodePort()
                .endSpec()
                .build();

        infraResourceManager.createResource(address, endpoint);

        address = MessagingAddressResourceType.getOperation().inNamespace(address.getMetadata().getNamespace()).withName(address.getMetadata().getName()).get();
        MessagingAddressCondition addressCondition = MessagingAddressResourceType.getCondition(address.getStatus().getConditions(), "Ready");
        assertNotNull(addressCondition);
        assertEquals("True", addressCondition.getStatus());

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        MessagingEndpointCondition endpointCondition = MessagingEndpointResourceType.getCondition(endpoint.getStatus().getConditions(), "Ready");
        assertNotNull(endpointCondition);
        assertEquals("True", endpointCondition.getStatus());

        AmqpClient client = infraResourceManager.getAmqpClientFactory().createClient(new AmqpConnectOptions()
                .setSaslMechanism("ANONYMOUS")
                .setQos(ProtonQoS.AT_LEAST_ONCE)
                .setEndpoint(new Endpoint(endpoint.getStatus().getHost(), endpoint.getStatus().getPorts().get(0).getPort()))
                .setProtonClientOptions(new ProtonClientOptions().setSsl(false))
                .setTerminusFactory(new QueueTerminusFactory()));

        assertEquals(1, client.sendMessages("queue1", Collections.singletonList("hello")).get(1, TimeUnit.MINUTES));
        var result = client.recvMessages("queue1", 1).get();
        assertEquals(1, result.size());
        assertEquals("hello", ((AmqpValue) result.get(0).getBody()).getValue());
    }

    @Test
    @ExternalClients
    @DefaultMessagingTenant
    public void testClusterIp() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();

        MessagingAddress address = new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("queue1")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewQueue()
                .endQueue()
                .endSpec()
                .build();

        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewCluster()
                .endCluster()
                .addToProtocols("AMQP")
                .endSpec()
                .build();

        infraResourceManager.createResource(address, endpoint);

        address = MessagingAddressResourceType.getOperation().inNamespace(address.getMetadata().getNamespace()).withName(address.getMetadata().getName()).get();
        MessagingAddressCondition addressCondition = MessagingAddressResourceType.getCondition(address.getStatus().getConditions(), "Ready");
        assertNotNull(addressCondition);
        assertEquals("True", addressCondition.getStatus());

        endpoint = MessagingEndpointResourceType.getOperation().inNamespace(endpoint.getMetadata().getNamespace()).withName(endpoint.getMetadata().getName()).get();
        MessagingEndpointCondition endpointCondition = MessagingEndpointResourceType.getCondition(endpoint.getStatus().getConditions(), "Ready");
        assertNotNull(endpointCondition);
        assertEquals("True", endpointCondition.getStatus());

        doTestSendReceiveAddress(endpoint.getStatus().getHost(), endpoint.getStatus().getPorts().get(0).getPort(), address.getMetadata().getName());
    }

    static void doTestSendReceiveAddress(String host, int port, String address) throws Exception {
        int expectedMsgCount = 10;
        Endpoint e = new Endpoint(host, port);
        ExternalMessagingClient senderClient = new ExternalMessagingClient(false)
                .withClientEngine(new ProtonJMSClientSender())
                .withMessagingRoute(e)
                .withAddress(address)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS")
                .withTimeout(30);

        ExternalMessagingClient receiverClient = new ExternalMessagingClient(false)
                .withClientEngine(new ProtonJMSClientReceiver())
                .withMessagingRoute(e)
                .withAddress(address)
                .withCount(expectedMsgCount)
                .withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS")
                .withTimeout(30);


        List<Future<Boolean>> results = ForkJoinPool.commonPool().invokeAll(List.of(senderClient::run, receiverClient::run));

        assertTrue(results.get(0).get(1, TimeUnit.MINUTES), "Sender failed, expected return code 0");
        assertTrue(results.get(1).get(1, TimeUnit.MINUTES), "Receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiverClient.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
    }
}
