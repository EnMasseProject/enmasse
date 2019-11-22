/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.clients;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointStatus;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.ReceiverStatus;
import io.enmasse.systemtest.amqp.UnauthorizedAccessException;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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

    public static void preparePolicyClients(AbstractClient sender, AbstractClient receiver, Address dest, AddressSpace addressSpace) {
        ClientArgumentMap arguments = new ClientArgumentMap();
        UserCredentials credentials = new UserCredentials("test", "test");

        arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        arguments.put(ClientArgument.BROKER, getInnerMessagingRoute(addressSpace).toString());
        arguments.put(ClientArgument.ADDRESS, dest.getSpec().getAddress());
        arguments.put(ClientArgument.COUNT, "5");
        arguments.put(ClientArgument.MSG_CONTENT, "msg no. %d");
        arguments.put(ClientArgument.TIMEOUT, "30");
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.LOG_MESSAGES, "json");

        sender.setArguments(arguments);
        arguments.remove(ClientArgument.MSG_CONTENT);
        receiver.setArguments(arguments);
    }

    private static Endpoint getInnerMessagingRoute(AddressSpace addressSpace) {
/*        return new Endpoint(String.format("%s-%s.%s.svc.cluster.local",
                "messaging", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace),
                Environment.getInstance().namespace()), 5671);*/

            for (EndpointStatus endpointStatus : addressSpace.getStatus().getEndpointStatuses()) {
                if (endpointStatus.getName().equals("messaging")) {
                    return new Endpoint(endpointStatus.getServiceHost(), 5671);
                }
            }
            return null;
    }
}
