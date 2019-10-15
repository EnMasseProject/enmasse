/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.amqp.ReceiverStatus;
import io.enmasse.systemtest.amqp.UnauthorizedAccessException;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientConnector;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.mqtt.MqttUtils;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.sasl.MechanismMismatchException;
import io.vertx.proton.sasl.SaslSystemException;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;

import javax.jms.DeliveryMode;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.security.sasl.AuthenticationException;
import java.nio.charset.StandardCharsets;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessagingUtils {
    private static Logger LOGGER = CustomLogger.getLogger();
    private AddressSpaceUtils addressSpaceUtils = new AddressSpaceUtils();

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
        if (addressSpaceUtils.isBrokered(addressSpace) && !brokeredAddressTypes.contains(addressType)) {
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

    public static void doMessaging(ResourceManager manager, List<Address> dest, List<UserCredentials> users, String destNamePrefix, int customerIndex, int messageCount) throws Exception {
        ArrayList<AmqpClient> clients = new ArrayList<>(users.size());
        String sufix = new AddressSpaceUtils().isBrokered(manager.getSharedAddressSpace()) ? "#" : "*";
        users.forEach((user) -> {
            try {
                manager.createOrUpdateUser(manager.getSharedAddressSpace(),
                        UserUtils.createUserResource(user)
                                .editSpec()
                                .withAuthorization(Collections.singletonList(
                                        new UserAuthorizationBuilder()
                                                .withAddresses(String.format("%s.%s.%s", destNamePrefix, customerIndex, sufix))
                                                .withOperations(Operation.send, Operation.recv).build()))
                                .endSpec()
                                .done());
                AmqpClient queueClient = manager.getAmqpClientFactory().createQueueClient();
                queueClient.getConnectOptions().setCredentials(user);
                clients.add(queueClient);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        AddressUtils.waitForDestinationsReady(dest.toArray(new Address[0]));
        //start sending messages
        int everyN = 3;
        for (AmqpClient client : clients) {
            for (int i = 0; i < dest.size(); i++) {
                if (i % everyN == 0) {
                    Future<Integer> sent = client.sendMessages(dest.get(i).getSpec().getAddress(), TestUtils.generateMessages(messageCount));
                    //wait for messages sent
                    assertEquals(messageCount, sent.get(1, TimeUnit.MINUTES).intValue(),
                            "Incorrect count of messages send");
                }
            }
        }

        //receive messages
        for (AmqpClient client : clients) {
            for (int i = 0; i < dest.size(); i++) {
                if (i % everyN == 0) {
                    Future<List<Message>> received = client.recvMessages(dest.get(i).getSpec().getAddress(), messageCount);
                    //wait for messages received
                    assertEquals(messageCount, received.get(1, TimeUnit.MINUTES).size(),
                            "Incorrect count of messages received");
                }
            }
            client.close();
        }
    }

    /**
     * attach N receivers into one address with own username/password
     */
    public static List<AbstractClient> attachReceivers(AddressSpace addressSpace, Address destination,
                                                int receiverCount, int timeout, UserCredentials credentials) throws Exception {
        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, Kubernetes.getInstance().getMessagingRoute(addressSpace).toString());
        if (timeout > 0) {
            arguments.put(ClientArgument.TIMEOUT, Integer.toString(timeout));
        }
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        arguments.put(ClientArgument.ADDRESS, destination.getSpec().getAddress());
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property2~testValue");

        List<AbstractClient> receivers = new ArrayList<>();
        for (int i = 0; i < receiverCount; i++) {
            RheaClientReceiver rec = new RheaClientReceiver();
            rec.setArguments(arguments);
            rec.runAsync(false);
            receivers.add(rec);
        }

        Thread.sleep(15000); //wait for attached
        return receivers;
    }

    /**
     * attach senders to destinations (for N-th destination is attached N+1 senders)
     */
    public static List<AbstractClient> attachSenders(AddressSpace addressSpace, List<Address> destinations, int timeout, UserCredentials defaultCredentials) throws Exception {
        List<AbstractClient> senders = new ArrayList<>();

        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, Kubernetes.getInstance().getMessagingRoute(addressSpace).toString());
        if (timeout > 0) {
            arguments.put(ClientArgument.TIMEOUT, Integer.toString(timeout));
        }
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.USERNAME, defaultCredentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, defaultCredentials.getPassword());
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        arguments.put(ClientArgument.MSG_CONTENT, "msg no.%d");
        arguments.put(ClientArgument.COUNT, "30");
        arguments.put(ClientArgument.DURATION, "30");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property2~testValue");

        for (int i = 0; i < destinations.size(); i++) {
            arguments.put(ClientArgument.ADDRESS, destinations.get(i).getSpec().getAddress());
            for (int j = 0; j < i + 1; j++) {
                AbstractClient send = new RheaClientSender();
                send.setArguments(arguments);
                send.runAsync(false);
                senders.add(send);
            }
        }

        return senders;
    }

    /**
     * attach receivers to destinations (for N-th destination is attached N+1 senders)
     */
    public static List<AbstractClient> attachReceivers(AddressSpace addressSpace, List<Address> destinations, int timeout, UserCredentials userCredentials) throws Exception {
        List<AbstractClient> receivers = new ArrayList<>();

        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, Kubernetes.getInstance().getMessagingRoute(addressSpace).toString());
        if (timeout > 0) {
            arguments.put(ClientArgument.TIMEOUT, Integer.toString(timeout));
        }
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.USERNAME, userCredentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, userCredentials.getPassword());
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property2~testValue");

        for (int i = 0; i < destinations.size(); i++) {
            arguments.put(ClientArgument.ADDRESS, destinations.get(i).getSpec().getAddress());
            for (int j = 0; j < i + 1; j++) {
                AbstractClient rec = new RheaClientReceiver();
                rec.setArguments(arguments);
                rec.runAsync(false);
                receivers.add(rec);
            }
        }

        return receivers;
    }

    /**
     * create M connections with N receivers and K senders
     */
    public static AbstractClient attachConnector(AddressSpace addressSpace, Address destination,
                                             int connectionCount,
                                             int senderCount, int receiverCount, UserCredentials credentials, int timeout) throws Exception {
        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, Kubernetes.getInstance().getMessagingRoute(addressSpace).toString());
        if (timeout > 0) {
            arguments.put(ClientArgument.TIMEOUT, Integer.toString(timeout));
        }
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        arguments.put(ClientArgument.OBJECT_CONTROL, "CESR");
        arguments.put(ClientArgument.ADDRESS, destination.getSpec().getAddress());
        arguments.put(ClientArgument.COUNT, Integer.toString(connectionCount));
        arguments.put(ClientArgument.SENDER_COUNT, Integer.toString(senderCount));
        arguments.put(ClientArgument.RECEIVER_COUNT, Integer.toString(receiverCount));
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property2~testValue");

        AbstractClient cli = new RheaClientConnector();
        cli.setArguments(arguments);
        cli.runAsync(false);

        return cli;
    }

    /**
     * stop all clients from list of Abstract clients
     */
    public static void stopClients(List<AbstractClient> clients) {
        if (clients != null) {
            LOGGER.info("Stopping clients...");
            clients.forEach(AbstractClient::stop);
        }
    }

    public static void sendReceiveLargeMessage(JmsProvider jmsProvider, int sizeInMB, Address dest, int count) throws Exception {
        sendReceiveLargeMessage(jmsProvider, sizeInMB, dest, count, DeliveryMode.NON_PERSISTENT);
    }

    public static void sendReceiveLargeMessage(JmsProvider jmsProvider, int sizeInMB, Address dest, int count, int mode) throws Exception {
        int size = sizeInMB * 1024 * 1024;

        Session session = jmsProvider.getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
        javax.jms.Queue testQueue = (javax.jms.Queue) jmsProvider.getDestination(dest.getSpec().getAddress());
        List<javax.jms.Message> messages = jmsProvider.generateMessages(session, count, size);

        MessageProducer sender = session.createProducer(testQueue);
        MessageConsumer receiver = session.createConsumer(testQueue);
        List<javax.jms.Message> recvd;

        jmsProvider.sendMessages(sender, messages, mode, javax.jms.Message.DEFAULT_PRIORITY, javax.jms.Message.DEFAULT_TIME_TO_LIVE);
        LOGGER.info("{}MB {} message sent", sizeInMB, mode == DeliveryMode.PERSISTENT ? "durable" : "non-durable");

        recvd = jmsProvider.receiveMessages(receiver, count, 2000);
        assertThat("Wrong count of received messages", recvd.size(), Matchers.is(count));
        LOGGER.info("{}MB {} message received", sizeInMB, mode == DeliveryMode.PERSISTENT ? "durable" : "non-durable");
    }

    public static List<String> extractBodyAsString(Future<List<Message>> msgs) throws Exception {
        return msgs.get(1, TimeUnit.MINUTES).stream().map(m -> (String) ((AmqpValue) m.getBody()).getValue()).collect(Collectors.toList());
    }

    public static void simpleMQTTSendReceive(Address dest, IMqttClient client, int msgCount) throws Exception {
        List<MqttMessage> messages = IntStream.range(0, msgCount).boxed().map(i -> {
            MqttMessage m = new MqttMessage();
            m.setPayload(String.format("mqtt-simple-send-receive-%s", i).getBytes(StandardCharsets.UTF_8));
            m.setQos(1);
            return m;
        }).collect(Collectors.toList());

        List<CompletableFuture<MqttMessage>> receiveFutures = MqttUtils.subscribeAndReceiveMessages(client, dest.getSpec().getAddress(), messages.size(), 1);
        List<CompletableFuture<Void>> publishFutures = MqttUtils.publish(client, dest.getSpec().getAddress(), messages);

        int publishCount = MqttUtils.awaitAndReturnCode(publishFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages published",
                publishCount, is(messages.size()));

        int receivedCount = MqttUtils.awaitAndReturnCode(receiveFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages received",
                receivedCount, is(messages.size()));
    }
}
