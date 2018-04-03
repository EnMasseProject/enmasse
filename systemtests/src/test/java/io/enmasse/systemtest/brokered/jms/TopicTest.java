/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.jms;


import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Destination;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TopicTest extends JMSTestBase {
    private static Logger log = CustomLogger.getLogger();

    private Hashtable<Object, Object> env;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private Context context;
    private String topic = "jmsTopic";
    private Destination addressTopic;

    @BeforeEach
    public void setUp() throws Exception {
        addressTopic = Destination.topic(topic, getDefaultPlan(AddressType.TOPIC));
        setAddresses(addressTopic);

        env = setUpEnv("amqps://" + getMessagingRoute(sharedAddressSpace).toString(), jmsUsername, jmsPassword, jmsClientID,
                new HashMap<String, String>() {{
                    put("topic." + topic, topic);
                }});
        context = new InitialContext(env);
        connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionFactory");
        connection = connectionFactory.createConnection();
        connection.start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.stop();
        }
        if (session != null) {
            session.close();
        }
        if (connection != null) {
            connection.close();
        }

    }

    private Context createContextForShared() throws JMSException, NamingException {
        Hashtable env2 = setUpEnv("amqps://" + getMessagingRoute(sharedAddressSpace).toString(), jmsUsername, jmsPassword,
                new HashMap<String, String>() {{
                    put("topic." + topic, topic);
                }});
        return new InitialContext(env2);
    }

    @Test
    public void testMessageSubscription() throws Exception {
        log.info("testMessageSubscription");
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic testTopic = (Topic) context.lookup(topic);
        MessageConsumer subscriber1 = session.createConsumer(testTopic);
        MessageProducer messageProducer = session.createProducer(testTopic);

        int count = 1000;
        List<Message> listMsgs = generateMessages(session, count);

        CompletableFuture<List<Message>> received = new CompletableFuture<>();

        List<Message> recvd = new ArrayList<>();
        AtomicInteger i = new AtomicInteger(0);
        MessageListener myListener = message -> {
            recvd.add(message);
            if (i.incrementAndGet() == count) {
                received.complete(recvd);
            }
        };
        subscriber1.setMessageListener(myListener);

        sendMessages(messageProducer, listMsgs);
        log.info("messages sent");

        assertThat("Wrong count of messages received", received.get(30, TimeUnit.SECONDS).size(), is(count));
        log.info("messages received");

        subscriber1.close();
        messageProducer.close();
    }

    //TODO: this test can be enabled when ENTMQBR-910 will be fixed
    public void testMessageDurableSubscription() throws Exception {
        log.info("testMessageDurableSubscription");
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic testTopic = (Topic) context.lookup(topic);

        String sub1ID = "sub1DurSub";
        String sub2ID = "sub2DurSub";
        MessageConsumer subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        MessageConsumer subscriber2 = session.createDurableSubscriber(testTopic, sub2ID);
        MessageProducer messageProducer = session.createProducer(testTopic);

        int count = 100;
        String batchPrefix = "First";
        List<Message> listMsgs = generateMessages(session, batchPrefix, count);
        sendMessages(messageProducer, listMsgs);
        log.info("First batch messages sent");

        List<Message> recvd1 = receiveMessages(subscriber1, count);
        List<Message> recvd2 = receiveMessages(subscriber2, count);

        assertThat("Wrong count of messages received: by " + sub1ID, recvd1.size(), is(count));
        assertMessageContent(recvd1, batchPrefix);
        log.info(sub1ID + " :First batch messages received");

        assertThat("Wrong count of messages received: by " + sub2ID, recvd2.size(), is(count));
        assertMessageContent(recvd2, batchPrefix);
        log.info(sub2ID + " :First batch messages received");

        subscriber1.close();
        Thread.sleep(30000); //!TODO: this row can be removed when ENTMQBR-910 will be fixed
        log.info(sub1ID + " : closed");

        batchPrefix = "Second";
        listMsgs = generateMessages(session, batchPrefix, count);
        sendMessages(messageProducer, listMsgs);
        log.info("Second batch messages sent");

        recvd2 = receiveMessages(subscriber2, count);
        assertThat("Wrong count of messages received: by " + sub2ID, recvd2.size(), is(count));
        assertMessageContent(recvd2, batchPrefix);
        log.info(sub2ID + " :Second batch messages received");

        subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        log.info(sub1ID + " :connected");

        recvd1 = receiveMessages(subscriber1, count);
        assertThat("Wrong count of messages received: by " + sub1ID, recvd1.size(), is(count));
        assertMessageContent(recvd1, batchPrefix);
        log.info(sub1ID + " :Second batch messages received");

        subscriber1.close();
        subscriber2.close();

        session.unsubscribe(sub1ID);
        session.unsubscribe(sub2ID);
    }

    @Test
    public void testMessageDurableSubscriptionTransacted() throws Exception {
        log.info("testMessageDurableSubscriptionTransacted");
        session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Topic testTopic = (Topic) context.lookup(topic);

        String sub1ID = "sub1DurSubTrans";
        String sub2ID = "sub2DurSubTrans";

        MessageConsumer subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        MessageConsumer subscriber2 = session.createDurableSubscriber(testTopic, sub2ID);
        MessageProducer messageProducer = session.createProducer(testTopic);

        int count = 100;
        List<Message> listMsgs = generateMessages(session, count);
        sendMessages(messageProducer, listMsgs);
        session.commit();
        log.info("messages sent");

        List<Message> recvd1 = receiveMessages(subscriber1, count);
        session.commit();
        List<Message> recvd2 = receiveMessages(subscriber2, count);
        session.commit();

        log.info(sub1ID + " :messages received");
        log.info(sub2ID + " :messages received");

        assertThat("Wrong count of messages received: by " + sub1ID, recvd1.size(), is(count));
        assertThat("Wrong count of messages received: by " + sub2ID, recvd2.size(), is(count));

        subscriber1.close();
        subscriber2.close();

        session.unsubscribe(sub1ID);
        session.unsubscribe(sub2ID);
    }

    @Test
    public void testSharedDurableSubscription() throws Exception {
        log.info("testSharedDurableSubscription");

        Context context1 = createContextForShared();
        ConnectionFactory connectionFactory1 = (ConnectionFactory) context1.lookup("qpidConnectionFactory");
        Connection connection1 = connectionFactory1.createConnection();
        Context context2 = createContextForShared();
        ConnectionFactory connectionFactory2 = (ConnectionFactory) context2.lookup("qpidConnectionFactory");
        Connection connection2 = connectionFactory2.createConnection();
        connection1.start();
        connection2.start();

        Session session = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session2 = connection2.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic testTopic = (Topic) context1.lookup(topic);

        String subID = "sharedConsumerDurable123";

        MessageConsumer subscriber1 = session.createSharedDurableConsumer(testTopic, subID);
        MessageConsumer subscriber2 = session2.createSharedDurableConsumer(testTopic, subID);
        MessageProducer messageProducer = session.createProducer(testTopic);
        messageProducer.setDeliveryMode(DeliveryMode.PERSISTENT);

        int count = 10;
        List<Message> listMsgs = generateMessages(session, count);
        sendMessages(messageProducer, listMsgs);
        log.info("messages sent");

        List<Message> recvd1 = receiveMessages(subscriber1, count, 1);
        List<Message> recvd2 = receiveMessages(subscriber2, count, 1);

        log.info(subID + " :messages received");

        assertThat("Wrong count of messages received: by both receivers",
                recvd1.size() + recvd2.size(), is(2 * count));

        subscriber1.close();
        subscriber2.close();
        session.unsubscribe(subID);
        session2.unsubscribe(subID);
        connection1.stop();
        connection2.stop();
        session.close();
        session2.close();
        connection1.close();
        connection2.close();
    }

    @Test
    public void testSharedNonDurableSubscription() throws JMSException, NamingException, InterruptedException, ExecutionException, TimeoutException {
        log.info("testSharedNonDurableSubscription");

        Context context1 = createContextForShared();
        ConnectionFactory connectionFactory1 = (ConnectionFactory) context1.lookup("qpidConnectionFactory");
        Connection connection1 = connectionFactory1.createConnection();
        Context context2 = createContextForShared();
        ConnectionFactory connectionFactory2 = (ConnectionFactory) context2.lookup("qpidConnectionFactory");
        Connection connection2 = connectionFactory2.createConnection();
        connection1.start();
        connection2.start();

        Session session = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session2 = connection2.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic testTopic = (Topic) context1.lookup(topic);
        String subID = "sharedConsumerNonDurable123";
        MessageConsumer subscriber1 = session.createSharedConsumer(testTopic, subID);
        MessageConsumer subscriber2 = session2.createSharedConsumer(testTopic, subID);
        MessageConsumer subscriber3 = session2.createSharedConsumer(testTopic, subID);
        MessageProducer messageProducer = session.createProducer(testTopic);
        messageProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        int count = 10;
        List<Message> listMsgs = generateMessages(session, count);
        List<CompletableFuture<List<Message>>> results = receiveMessagesAsync(count, subscriber1, subscriber2, subscriber3);
        sendMessages(messageProducer, listMsgs);
        log.info("messages sent");

        assertThat("Each message should be received only by one consumer",
                results.get(0).get(20, TimeUnit.SECONDS).size() +
                        results.get(1).get(20, TimeUnit.SECONDS).size() +
                        results.get(2).get(20, TimeUnit.SECONDS).size(),
                is(count));
        log.info("messages received");

        connection1.stop();
        connection2.stop();
        subscriber1.close();
        subscriber2.close();
        subscriber3.close();
        session.close();
        session2.close();
        connection1.close();
        connection2.close();
    }
}
