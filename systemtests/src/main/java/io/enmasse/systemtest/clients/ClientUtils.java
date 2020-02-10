/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.clients;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointStatus;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.ReceiverStatus;
import io.enmasse.systemtest.amqp.UnauthorizedAccessException;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientConnector;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.sasl.MechanismMismatchException;
import io.vertx.proton.sasl.SaslSystemException;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;

import javax.security.sasl.AuthenticationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClientUtils {
    private static Logger LOGGER = CustomLogger.getLogger();

    /**
     * Estimation of max milliseconds it could take, in worst case, to send or receive one message
     */
    public static final long ESTIMATE_MAX_MS_PER_MESSAGE = 200;

    public void sendDurableMessages(ResourceManager resourceManager, AddressSpace addressSpace, Address destination,
                                    UserCredentials credentials, int count) throws Exception {
        AmqpClient client = resourceManager.getAmqpClientFactory().createQueueClient(addressSpace);
        LOGGER.debug("Address fucking space!: " + addressSpace.toString());
        client.getConnectOptions().setCredentials(credentials);
        List<Message> listOfMessages = new ArrayList<>();
        IntStream.range(0, count).forEach(num -> {
            Message msg = Message.Factory.create();
            msg.setAddress(destination.getSpec().getAddress());
            msg.setDurable(true);
            listOfMessages.add(msg);
        });
        Future<Integer> sent = client.sendMessages(destination.getSpec().getAddress(), listOfMessages.toArray(new Message[0]));
        assertThat("Cannot send durable messages to " + destination, sent.get(1, TimeUnit.MINUTES), is(count));
        client.close();
    }

    public void connectAddressSpace(ResourceManager resourceManager, AddressSpace addressSpace, UserCredentials credentials) throws Exception {
        try (AmqpClient client = resourceManager.getAmqpClientFactory().createQueueClient(addressSpace)) {
            client.getConnectOptions().setCredentials(credentials);
            CompletableFuture<Void> connect = client.connect();
            try {
                connect.get(5, TimeUnit.MINUTES);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof Exception) {
                    throw ((Exception) e.getCause());
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
    }

    public void receiveDurableMessages(ResourceManager resourceManager, AddressSpace addressSpace, Address dest,
                                       UserCredentials credentials, int count) throws Exception {
        AmqpClient client = resourceManager.getAmqpClientFactory().createQueueClient(addressSpace);
        client.getConnectOptions().setCredentials(credentials);
        ReceiverStatus receiverStatus = client.recvMessagesWithStatus(dest.getSpec().getAddress(), count);
        assertThat("Cannot receive durable messages from " + dest + ". Got " + receiverStatus.getNumReceived(), receiverStatus.getResult().get(1, TimeUnit.MINUTES).size(), is(count));
        client.close();
    }

    private boolean canConnectWithAmqpAddress(ResourceManager resourceManager, AddressSpace addressSpace, UserCredentials credentials, AddressType addressType, String address, boolean defaultValue) throws Exception {
        Set<AddressType> brokeredAddressTypes = new HashSet<>(Arrays.asList(AddressType.QUEUE, AddressType.TOPIC));
        if (AddressSpaceUtils.isBrokered(addressSpace) && !brokeredAddressTypes.contains(addressType)) {
            return defaultValue;
        }
        try (AmqpClient client = resourceManager.getAmqpClientFactory().createAddressClient(addressSpace, addressType)) {
            client.getConnectOptions().setCredentials(credentials);
            ProtonClientOptions protonClientOptions = client.getConnectOptions().getProtonClientOptions();
            protonClientOptions.setLogActivity(true);
            client.getConnectOptions().setProtonClientOptions(protonClientOptions);

            try {
                Future<List<Message>> received = client.recvMessages(address, 1);
                Future<Integer> sent = client.sendMessages(address, Collections.singletonList("msg1"));

                int numReceived = received.get(1, TimeUnit.MINUTES).size();
                int numSent = sent.get(1, TimeUnit.MINUTES);
                return (numSent == numReceived);
            } catch (ExecutionException | SecurityException | UnauthorizedAccessException ex) {
                Throwable cause = ex;
                if (ex instanceof ExecutionException) {
                    cause = ex.getCause();
                }

                if (cause instanceof AuthenticationException || cause instanceof SaslSystemException || cause instanceof SecurityException || cause instanceof UnauthorizedAccessException || cause instanceof MechanismMismatchException) {
                    LOGGER.info("canConnectWithAmqpAddress {} ({}): {}", address, addressType, ex.getMessage());
                    return false;
                } else {
                    LOGGER.warn("canConnectWithAmqpAddress {} ({}) exception", address, addressType, ex);
                    throw ex;
                }
            }
        }
    }

    public void assertCanConnect(AddressSpace addressSpace, UserCredentials credentials, List<Address> destinations, ResourceManager resourceManager) throws Exception {
        for (Address destination : destinations) {
            String message = String.format("Client failed, cannot connect to %s under user %s", destination.getSpec().getAddress(), credentials);
            AddressType addressType = AddressType.getEnum(destination.getSpec().getType());
            Assertions.assertTrue(canConnectWithAmqpAddress(resourceManager, addressSpace, credentials, addressType, destination.getSpec().getAddress(), true), message);
        }
    }

    public void assertCannotConnect(AddressSpace addressSpace, UserCredentials credentials, List<Address> destinations, ResourceManager resourceManager) throws Exception {
        for (Address destination : destinations) {
            String message = String.format("Client failed, can connect to %s under user %s", destination.getSpec().getAddress(), credentials);
            AddressType addressType = AddressType.getEnum(destination.getSpec().getType());
            Assertions.assertFalse(canConnectWithAmqpAddress(resourceManager, addressSpace, credentials, addressType, destination.getSpec().getAddress(), false), message);
        }
    }

    public void receiveMessages(AmqpClient amqpClient, String address, int count) throws Exception {
        long timeoutMs = count * ESTIMATE_MAX_MS_PER_MESSAGE;
        LOGGER.info("Start receiving with " + timeoutMs + " ms timeout");
        ReceiverStatus receiverStatus = amqpClient.recvMessagesWithStatus(address, count);
        assertThat("Incorrect count of messages received from " + address + ". Got " + receiverStatus.getNumReceived(),
                receiverStatus.getResult().get(timeoutMs, TimeUnit.MILLISECONDS).size(), is(count));
    }

    public static ExternalMessagingClient getPolicyClient(AbstractClient client, Address dest, AddressSpace addressSpace) {
        return new ExternalMessagingClient()
                .withClientEngine(client)
                .withCredentials("test", "test")
                .withMessagingRoute(Objects.requireNonNull(getInnerMessagingRoute(addressSpace)))
                .withAddress(dest)
                .withCount(5)
                .withMessageBody("msg no. %d")
                .withTimeout(6);
    }

    private static Endpoint getInnerMessagingRoute(AddressSpace addressSpace) {
        for (EndpointStatus endpointStatus : addressSpace.getStatus().getEndpointStatuses()) {
            if (endpointStatus.getName().equals("messaging")) {
                return new Endpoint(endpointStatus.getServiceHost(), 5671);
            }
        }
        return null;
    }

    /**
     * attach N receivers into one address with own username/password
     */
    public List<ExternalMessagingClient> attachReceivers(AddressSpace addressSpace, Address destination,
                                                         int receiverCount, int timeout, UserCredentials userCredentials) throws Exception {

        List<ExternalMessagingClient> receivers = new ArrayList<>();
        for (int i = 0; i < receiverCount; i++) {
            ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                    .withClientEngine(new RheaClientReceiver())
                    .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(addressSpace))
                    .withAddress(destination)
                    .withCredentials(userCredentials)
                    .withTimeout(timeout);
            receiverClient.runAsync(false);
            receivers.add(receiverClient);
        }

        Thread.sleep(15000); //wait for attached
        return receivers;
    }

    public List<ExternalMessagingClient> attachSenders(AddressSpace addressSpace, List<Address> destinations, UserCredentials userCredentials) throws Exception {
        return attachSenders(addressSpace, destinations, 360, userCredentials);
    }

    /**
     * attach senders to destinations (for N-th destination is attached N+1 senders)
     */
    public List<ExternalMessagingClient> attachSenders(AddressSpace addressSpace, List<Address> destinations, int timeout, UserCredentials userCredentials) throws Exception {
        List<ExternalMessagingClient> senders = new ArrayList<>();

        for (int i = 0; i < destinations.size(); i++) {
            for (int j = 0; j < i + 1; j++) {
                ExternalMessagingClient senderClient = new ExternalMessagingClient()
                        .withClientEngine(new RheaClientSender())
                        .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(addressSpace))
                        .withAddress(destinations.get(i))
                        .withCredentials(userCredentials)
                        .withMessageBody("msg no.%d")
                        .withTimeout(timeout)
                        .withCount(timeout)
                        .withAdditionalArgument(ClientArgument.DURATION, timeout * 1000);
                senderClient.runAsync(false);
                senders.add(senderClient);
            }
        }

        return senders;
    }

    public ExternalMessagingClient attachReceiver(AddressSpace addressSpace, Address destination, UserCredentials userCredentials, int count) throws Exception {
        ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                .withClientEngine(new RheaClientReceiver())
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(addressSpace))
                .withAddress(destination)
                .withCredentials(userCredentials)
                .withTimeout(500)
                .withCount(count);
        receiverClient.runAsync(false);
        return receiverClient;
    }

    public ExternalMessagingClient attachSender(AddressSpace addressSpace, Address destination, UserCredentials userCredentials, int count, int durationMillis) throws Exception {
        ExternalMessagingClient senderClient = new ExternalMessagingClient()
                .withClientEngine(new RheaClientSender())
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(addressSpace))
                .withAddress(destination)
                .withCredentials(userCredentials)
                .withMessageBody("msg no.%d")
                .withTimeout(500)
                .withCount(count)
                .withAdditionalArgument(ClientArgument.DURATION, durationMillis);
        senderClient.runAsync(false);
        return senderClient;
    }

    public List<ExternalMessagingClient> attachReceivers(AddressSpace addressSpace, List<Address> destinations, UserCredentials userCredentials) throws Exception {
        return attachReceivers(addressSpace, destinations, 360, userCredentials);
    }

    /**
     * attach receivers to destinations (for N-th destination is attached N+1 senders)
     */
    public List<ExternalMessagingClient> attachReceivers(AddressSpace addressSpace, List<Address> destinations, int timeout, UserCredentials userCredentials) throws Exception {
        List<ExternalMessagingClient> receivers = new ArrayList<>();

        for (int i = 0; i < destinations.size(); i++) {
            for (int j = 0; j < i + 1; j++) {
                ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                        .withClientEngine(new RheaClientReceiver())
                        .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(addressSpace))
                        .withAddress(destinations.get(i))
                        .withCredentials(userCredentials)
                        .withTimeout(timeout);
                receiverClient.runAsync(false);
                receivers.add(receiverClient);
            }
        }

        return receivers;
    }

    /**
     * create M connections with N receivers and K senders
     */
    public ExternalMessagingClient attachConnector(AddressSpace addressSpace, Address destination,
                                                      int connectionCount,
                                                      int senderCount, int receiverCount, UserCredentials credentials, int timeout) throws Exception {

        ExternalMessagingClient connectorClient = new ExternalMessagingClient()
                .withClientEngine(new RheaClientConnector())
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(addressSpace))
                .withAddress(destination)
                .withCredentials(credentials)
                .withCount(connectionCount)
                .withTimeout(timeout)
                .withAdditionalArgument(ClientArgument.SENDER_COUNT, Integer.toString(senderCount))
                .withAdditionalArgument(ClientArgument.RECEIVER_COUNT, Integer.toString(receiverCount));
        connectorClient.runAsync(false);

        return connectorClient;
    }

    /**
     * stop all clients from list of Abstract clients
     */
    public void stopClients(List<ExternalMessagingClient> clients, boolean testFailed) {
        if (clients != null) {
            LOGGER.info("Stopping clients...");
            clients.forEach(c -> {
                c.stop();
                if (testFailed) {
                    LOGGER.info("=======================================");
                    LOGGER.info("stderr {}", c.getStdError());
                    LOGGER.info("stdout {}", c.getStdOutput());
                }
            });
        }
    }

    @FunctionalInterface
    public static interface ClientAttacher {

        List<ExternalMessagingClient> attach(AddressSpace addressSpace, List<Address> destinations, UserCredentials userCredentials) throws Exception;

    }
}
