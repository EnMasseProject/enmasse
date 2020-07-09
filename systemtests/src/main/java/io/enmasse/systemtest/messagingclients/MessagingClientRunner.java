/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messagingclients;

import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.amqp.TerminusFactory;
import io.enmasse.systemtest.framework.LoggerUtils;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpConnectOptions;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.messaginginfra.ResourceManager;
import io.enmasse.systemtest.messaginginfra.resources.MessagingEndpointResourceType;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessagingClientRunner {
    Logger LOGGER;
    ExecutorService executor;

    private List<ExternalMessagingClient> clients = new ArrayList<>();

    public MessagingClientRunner() {
        LOGGER = LoggerUtils.getLogger();
    }

    public void sendAndReceive(MessagingEndpoint endpoint, boolean waitReceivers, String senderAddress, String ... receiverAddresses) throws Exception {
        sendAndReceive(endpoint, waitReceivers, null, null, senderAddress, receiverAddresses);
    }

    public void sendAndReceive(MessagingEndpoint endpoint, String senderAddress, String ... receiverAddresses) throws Exception {
        sendAndReceive(endpoint, false, null, null, senderAddress, receiverAddresses);
    }

    public void send(MessagingEndpoint endpoint, String address) throws Exception {
        sendAndReceive(endpoint, address);
    }

    public void receive(MessagingEndpoint endpoint, String address) throws Exception {
        Endpoint e = new Endpoint(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP", endpoint));
        try (ExternalMessagingClient receiverClient = new ExternalMessagingClient(false)
                .withClientEngine(new RheaClientReceiver())
                    .withMessagingRoute(e)
                    .withAddress(address)
                    .withCount(10)
                    .withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS")
                    .withTimeout(60)) {
            clients.add(receiverClient);
            assertTrue(receiverClient.run());
        }
    }

    /**
     * Send 10 messages on sender address, and receive 10 messages on each receiver address.
     */
    public void sendAndReceive(MessagingEndpoint endpoint, boolean waitReceivers, Map<ClientArgument, Object> extraSenderArgs,
                           Map<ClientArgument, Object> extraReceiverArgs, String senderAddress, String ... receiverAddresses) throws InterruptedException {
        int expectedMsgCount = 10;

        executor = Executors.newFixedThreadPool(1 + receiverAddresses.length);
        try {
            Endpoint e = new Endpoint(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP", endpoint));
            ExternalMessagingClient senderClient = new ExternalMessagingClient(false)
                    .withClientEngine(new RheaClientSender())
                    .withMessagingRoute(e)
                    .withAddress(senderAddress)
                    .withCount(expectedMsgCount)
                    .withMessageBody("msg no. %d")
                    .withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS")
                    .withTimeout(60);

            if (extraSenderArgs != null) {
                for (Map.Entry<ClientArgument, Object> arg : extraSenderArgs.entrySet()) {
                    senderClient.withAdditionalArgument(arg.getKey(), arg.getValue());
                }
            }

            for (String receiverAddress : receiverAddresses) {
                ExternalMessagingClient receiverClient = new ExternalMessagingClient(false)
                        .withClientEngine(new RheaClientReceiver())
                        .withMessagingRoute(e)
                        .withAddress(receiverAddress)
                        .withCount(expectedMsgCount)
                        .withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS")
                        .withTimeout(60);

                if (extraReceiverArgs != null) {
                    for (Map.Entry<ClientArgument, Object> arg : extraReceiverArgs.entrySet()) {
                        receiverClient.withAdditionalArgument(arg.getKey(), arg.getValue());
                    }
                }

                clients.add(receiverClient);
            }

            List<Future<Boolean>> receiverResults = new ArrayList<>();
            for (ExternalMessagingClient receiverClient : clients) {
                receiverResults.add(executor.submit((Callable<Boolean>) receiverClient::run));
            }

            if (waitReceivers) {
                // To ensure receivers are attached and ready
                Thread.sleep(10_000);
            }

            Future<Boolean> senderResult = executor.submit((Callable<Boolean>) senderClient::run);
            clients.add(senderClient);
            assertTrue(senderResult.get(1, TimeUnit.MINUTES), "Sender failed, expected return code 0");
            for (Future<Boolean> receiverResult : receiverResults) {
                assertTrue(receiverResult.get(1, TimeUnit.MINUTES), "Receiver failed, expected return code 0");
            }
        } catch (Exception e) {
            cleanClients();
        }
    }

    public void sendAndReceiveOnCluster(String host, int port, String address, boolean enableTls, boolean websockets) throws InterruptedException {
        assertTrue(port > 0);
        int expectedMsgCount = 10;
        executor = Executors.newFixedThreadPool(2);
        try {
            Endpoint e = new Endpoint(host, port);
            ExternalMessagingClient senderClient = new ExternalMessagingClient(enableTls)
                    .withClientEngine(websockets ? new RheaClientSender() : new ProtonJMSClientSender())
                    .withMessagingRoute(e)
                    .withAddress(address)
                    .withCount(expectedMsgCount)
                    .withMessageBody("msg no. %d")
                    .withTimeout(30);

            if (!websockets) {
                senderClient.withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS");
            }

            ExternalMessagingClient receiverClient = new ExternalMessagingClient(enableTls)
                    .withClientEngine(websockets ? new RheaClientReceiver() : new ProtonJMSClientReceiver())
                    .withMessagingRoute(e)
                    .withAddress(address)
                    .withCount(expectedMsgCount)
                    .withTimeout(30);
            if (!websockets) {
                receiverClient.withAdditionalArgument(ClientArgument.CONN_AUTH_MECHANISM, "ANONYMOUS");
            }

            /*if (enableTls) {
                senderClient.withAdditionalArgument(ClientArgument.CONN_SSL_VERIFY_PEER_NAME, true);
            }*/
            if (websockets) {
                // In Rhea setting the username only causes it to choose SASL ANONYMOUS (and send the username as the 'trace'
                // information).
                // See comment https://github.com/amqp/rhea/blob/master/examples/sasl/simple_sasl_client.js
                senderClient.withAdditionalArgument(ClientArgument.USERNAME, "trace");
                receiverClient.withAdditionalArgument(ClientArgument.USERNAME, "trace");
                senderClient.withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET, true);
                receiverClient.withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET, true);
                senderClient.withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET_PROTOCOLS, "binary");
                receiverClient.withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET_PROTOCOLS, "binary");
            }
            clients.addAll(Arrays.asList(senderClient, receiverClient));
            List<Future<Boolean>> results = executor.invokeAll(List.of(senderClient::run, receiverClient::run));

            assertTrue(results.get(0).get(1, TimeUnit.MINUTES), "Sender failed, expected return code 0");
            assertTrue(results.get(1).get(1, TimeUnit.MINUTES), "Receiver failed, expected return code 0");
        } catch (Exception e) {
            cleanClients();
        }
    }

    public AmqpClient sendReceiveOutsideCluster(String host, int port, String address, boolean tls, boolean verifyHost, String caCert) throws Exception {
        ProtonClientOptions protonClientOptions = new ProtonClientOptions();
        if (tls) {
            protonClientOptions.setSsl(true);
            if (!verifyHost) {
                protonClientOptions.setHostnameVerificationAlgorithm("");
            }
            if (caCert != null) {
                protonClientOptions.setTrustOptions(new PemTrustOptions()
                        .addCertValue(Buffer.buffer(caCert)));
            }
        }
        AmqpClient client = ResourceManager.getInstance().getAmqpClientFactory().createClient(new AmqpConnectOptions()
                .setSaslMechanism("ANONYMOUS")
                .setQos(ProtonQoS.AT_LEAST_ONCE)
                .setEndpoint(new Endpoint(host, port))
                .setProtonClientOptions(protonClientOptions)
                .setTerminusFactory(TerminusFactory.queue()));

        assertEquals(1, client.sendMessages(address, Collections.singletonList("hello")).get(1, TimeUnit.MINUTES));
        return client;
    }

    public void cleanClients() throws InterruptedException {
        if (clients.size() > 0) {
            clients.clear();
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    public List<ExternalMessagingClient> getClients() {
        return clients;
    }
}
