/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import io.enmasse.address.model.Address;
import io.enmasse.api.model.MessagingAddress;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class JmsProvider {
    private static Logger log = CustomLogger.getLogger();
    private Context context;
    private Connection connection;

    public Context getContext() {
        return context;
    }

    public Connection getConnection() {
        return connection;
    }

    public javax.jms.Destination getDestination(String address) throws NamingException {
        return (javax.jms.Destination) context.lookup(address);
    }

    private String getAddress(MessagingAddress address) {
        if (address.getSpec().getAddress() != null) {
            return address.getSpec().getAddress();
        }
        return address.getMetadata().getName();
    }

    private Map<String, String> createAddressMap(MessagingAddress destination) {
        String identification;
        if (destination.getSpec().getQueue() != null) {
            identification = "queue.";
        } else {
            identification = "topic.";
        }

        return new HashMap<>() {{
            put(identification + getAddress(destination), getAddress(destination));
        }};
    }

    private HashMap<String, String> createAddressMap(Address destination) {
        String identification;
        if (destination.getSpec().getType().equals(AddressType.QUEUE.toString())) {
            identification = "queue.";
        } else {
            identification = "topic.";
        }

        return new HashMap<String, String>() {{
            put(identification + destination.getSpec().getAddress(), destination.getSpec().getAddress());
        }};
    }

    public Context createContext(String host, int port, boolean tls, String username, String password, String clientID, MessagingAddress address) throws NamingException {
        Hashtable env = setUpEnv(host, port, tls, username, password, clientID, createAddressMap(address));
        context = new InitialContext(env);
        return context;
    }

    public Context createContext(String route, UserCredentials credentials, String cliID, Address address) throws Exception {
        Hashtable env = setUpEnv("amqps://" + route, 0, true, credentials.getUsername(), credentials.getPassword(), cliID,
                createAddressMap(address));
        context = new InitialContext(env);
        return context;
    }

    public Context createContextForShared(String route, UserCredentials credentials, Address address) throws Exception {
        Hashtable env = setUpEnv("amqps://" + route, credentials.getUsername(), credentials.getPassword(),
                createAddressMap(address));
        return new InitialContext(env);
    }

    public Connection createConnection(String route, UserCredentials credentials, String cliID, Address address) throws Exception {
        context = createContext(route, credentials, cliID, address);
        ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionFactory");
        connection = connectionFactory.createConnection();
        return connection;
    }

    public Connection createConnection(Context context) throws Exception {
        this.context = context;
        ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionFactory");
        connection = connectionFactory.createConnection();
        return connection;
    }


    public Hashtable<Object, Object> setUpEnv(String url, String username, String password, Map<String, String> prop) {
        return setUpEnv(url, 0, true, username, password, "", prop);
    }

    public Hashtable<Object, Object> setUpEnv(String host, int port, boolean tls, String username, String password, String clientID, Map<String, String> prop) {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");

        String scheme = "amqp://";
        List<String> params = new ArrayList<>();
        if (tls) {
            scheme = "amqps://";
            params.add("?transport.trustAll=true");
            params.add("?transport.verifyHost=false");
        }

        if (username != null && password != null) {
            params.add("amqp.saslMechanisms=PLAIN");
            params.add(String.format("jms.username=%s", username));
            params.add(String.format("jms.password=%s", password));
        } else {
            params.add("amqp.saslMechanisms=ANONYMOUS");
        }

        if (clientID != null && !clientID.isBlank()) {
            params.add(String.format("jms.clientID=%s", clientID));
        }

        StringBuilder url = new StringBuilder(String.format("%s%s", scheme, host));
        if (port != 0) {
            url.append(":").append(port);
        }

        char sep = '?';
        for (String param : params) {
            url.append(sep).append(param);
            sep = '&';
        }

        env.put("connectionfactory.qpidConnectionFactory", url.toString());
        for (Map.Entry<String, String> entry : prop.entrySet()) {
            env.put(entry.getKey(), entry.getValue());
        }
        return env;
    }

    public List<Message> generateMessages(Session session, int count) {
        return generateMessages(session, "", count);
    }

    public List<Message> generateMessages(Session session, String prefix, int count) {
        List<Message> messages = new LinkedList<>();
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, count).forEach(i -> {
            try {
                messages.add(session.createTextMessage(sb.append(prefix).append("testMessage").append(i).toString()));
                sb.setLength(0);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
        return messages;
    }

    public List<Message> generateMessages(Session session, int count, int sizeInBytes) throws Exception {
        List<Message> messages = new LinkedList<>();
        IntStream.range(0, count).forEach(i -> {
            try {
                messages.add(session.createTextMessage(String.join("", Collections.nCopies(sizeInBytes, "F"))));
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
        return messages;
    }

    public void sendMessages(MessageProducer producer, List<Message> messages) {
        sendMessages(producer, messages, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
    }


    public void sendMessages(MessageProducer producer, List<Message> messages, int mode, int priority, long ttl) {
        messages.forEach(m -> {
            try {
                producer.send(m, mode, priority, ttl);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    public List<CompletableFuture<List<Message>>> receiveMessagesAsync(int count, MessageConsumer... consumer) throws JMSException {
        AtomicInteger totalCount = new AtomicInteger(count);
        List<CompletableFuture<List<Message>>> resultsList = new ArrayList<>();
        List<List<Message>> receivedResList = new LinkedList<>();

        for (int i = 0; i < consumer.length; i++) {
            final int index = i;
            resultsList.add(new CompletableFuture<>());
            receivedResList.add(new ArrayList<>());
            MessageListener myListener = message -> {
                log.info("Mesages received" + message + " count: " + totalCount.get());
                receivedResList.get(index).add(message);
                if (totalCount.decrementAndGet() == 0) {
                    for (int j = 0; j < consumer.length; j++) {
                        resultsList.get(j).complete(receivedResList.get(j));
                    }
                }
            };
            consumer[i].setMessageListener(myListener);
        }
        return resultsList;
    }

    public CompletableFuture<List<Message>> receiveMessagesAsync(MessageConsumer consumer, AtomicInteger totalCount) throws JMSException {
        CompletableFuture<List<Message>> received = new CompletableFuture<>();
        List<Message> recvd = new LinkedList<>();
        MessageListener myListener = message -> {
            log.info("Mesages received" + message + " count: " + totalCount.get());
            recvd.add(message);
            if (totalCount.decrementAndGet() == 0) {
                received.complete(recvd);
            }
        };
        consumer.setMessageListener(myListener);
        return received;
    }

    public List<Message> receiveMessages(MessageConsumer consumer, int count) {
        return receiveMessages(consumer, count, 0);
    }

    public List<Message> receiveMessages(MessageConsumer consumer, int count, long timeout) {
        List<Message> recvd = new LinkedList<>();
        IntStream.range(0, count).forEach(i -> {
            try {
                recvd.add(timeout > 0 ? consumer.receive(timeout) : consumer.receive());
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
        return recvd;
    }

    public void assertMessageContent(List<Message> msgs, String content) {
        msgs.forEach(m -> {
            try {
                Assertions.assertTrue(((TextMessage) m).getText().contains(content),
                        "Message compare failed, message doesn't contain content.");
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }
}
