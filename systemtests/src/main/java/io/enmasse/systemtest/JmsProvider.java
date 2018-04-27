/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.*;
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

    private HashMap<String, String> createAddressMap(Destination destination) {
        String identification;
        if (destination.getType().equals(AddressType.QUEUE.toString())) {
            identification = "queue.";
        } else {
            identification = "topic.";
        }

        return new HashMap<String, String>() {{
            put(identification + destination.getAddress(), destination.getAddress());
        }};
    }

    public Context createContext(String route, KeycloakCredentials credentials, String cliID, Destination address) throws Exception {
        Hashtable env = setUpEnv("amqps://" + route, credentials.getUsername(), credentials.getPassword(), cliID,
                createAddressMap(address));
        context = new InitialContext(env);
        return context;
    }

    public Context createContextForShared(String route, KeycloakCredentials credentials, Destination address) throws Exception {
        Hashtable env = setUpEnv("amqps://" + route, credentials.getUsername(), credentials.getPassword(),
                createAddressMap(address));
        return new InitialContext(env);
    }

    public Connection createConnection(String route, KeycloakCredentials credentials, String cliID, Destination address) throws Exception {
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
        return setUpEnv(url, username, password, "", prop);
    }

    public Hashtable<Object, Object> setUpEnv(String url, String username, String password, String clientID, Map<String, String> prop) {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        StringBuilder urlParam = new StringBuilder();
        urlParam.append("?transport.trustAll=true")
                .append("&jms.password=").append(username)
                .append("&jms.username=").append(password)
                .append("&transport.verifyHost=false")
                .append("&amqp.saslMechanisms=PLAIN");
        urlParam.append(clientID.isEmpty() ? clientID : "&jms.clientID=" + clientID);

        env.put("connectionfactory.qpidConnectionFactory", url + urlParam);
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
