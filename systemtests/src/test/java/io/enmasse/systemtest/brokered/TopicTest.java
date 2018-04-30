/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.resolvers.JmsProviderParameterResolver;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import javax.jms.*;
import javax.naming.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(JmsProviderParameterResolver.class)
class TopicTest extends TestBaseWithShared implements ITestBaseBrokered {
    private static Logger log = CustomLogger.getLogger();
    private Connection connection;

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    @Test
    @Disabled("disable due to authorization exception with create queue on topic address with wildcards")
    void testTopicPubSubWildcards() throws Exception {

        int msgCount = 1000;
        int topicCount = 10;
        int senderCount = 10;
        int recvCount = topicCount / 2;

        List<Destination> topicList = new ArrayList<>();

        //create queues
        for (int i = 0; i < recvCount; i++) {
            topicList.add(Destination.topic(String.format("test-topic-pubsub%d.%d", i, i + 1), getDefaultPlan(AddressType.TOPIC)));
            topicList.add(Destination.topic(String.format("test-topic-pubsub%d.%d", i, i + 2), getDefaultPlan(AddressType.TOPIC)));
        }
        setAddresses(topicList.toArray(new Destination[0]));

        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        AmqpClient client = amqpClientFactory.createTopicClient(sharedAddressSpace);
        client.getConnectOptions().setCredentials(defaultCredentials);

        //attach subscribers
        List<Future<List<Message>>> recvResults = new ArrayList<>();
        for (int i = 0; i < recvCount; i++) {
            recvResults.add(client.recvMessages(String.format("test-topic-pubsub%d.*", i), msgCount * 2));
        }

        //attach producers
        for (int i = 0; i < senderCount; i++) {
            assertThat("Wrong count of messages sent: sender" + i,
                    client.sendMessages(topicList.get(i).getAddress(), msgBatch,
                            1, TimeUnit.MINUTES).get(1, TimeUnit.MINUTES), is(msgBatch.size()));
        }

        //check received messages
        for (int i = 0; i < recvCount; i++) {
            assertThat("Wrong count of messages received: receiver" + i,
                    recvResults.get(i).get().size(), is(msgCount * 2));
        }

        client.close();
    }

    @Test
    void testRestApi() throws Exception {
        Destination t1 = Destination.topic("topic1", getDefaultPlan(AddressType.TOPIC));
        Destination t2 = Destination.topic("topic2", getDefaultPlan(AddressType.TOPIC));

        runRestApiTest(sharedAddressSpace, t1, t2);
    }

    @Test
    void testMessageSubscription(JmsProvider jmsProvider) throws Exception {
        Destination addressTopic = Destination.topic("jmsTopic", getDefaultPlan(AddressType.TOPIC));
        setAddresses(addressTopic);

        connection = jmsProvider.createConnection(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials,
                "jmsCliId", addressTopic);
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getAddress());

        MessageConsumer subscriber1 = session.createConsumer(testTopic);
        MessageProducer messageProducer = session.createProducer(testTopic);

        int count = 1000;
        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, count);

        CompletableFuture<List<javax.jms.Message>> received = new CompletableFuture<>();

        List<javax.jms.Message> recvd = new ArrayList<>();
        AtomicInteger i = new AtomicInteger(0);
        MessageListener myListener = message -> {
            recvd.add(message);
            if (i.incrementAndGet() == count) {
                received.complete(recvd);
            }
        };
        subscriber1.setMessageListener(myListener);

        jmsProvider.sendMessages(messageProducer, listMsgs);
        log.info("messages sent");

        assertThat("Wrong count of messages received", received.get(30, TimeUnit.SECONDS).size(), is(count));
        log.info("messages received");

        subscriber1.close();
        messageProducer.close();
    }

    @Test
    void testMessageDurableSubscription(JmsProvider jmsProvider) throws Exception {
        Destination addressTopic = Destination.topic("jmsTopic", getDefaultPlan(AddressType.TOPIC));
        setAddresses(addressTopic);

        connection = jmsProvider.createConnection(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials,
                "jmsCliId", addressTopic);
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getAddress());

        String sub1ID = "sub1DurSub";
        String sub2ID = "sub2DurSub";
        MessageConsumer subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        MessageConsumer subscriber2 = session.createDurableSubscriber(testTopic, sub2ID);
        MessageProducer messageProducer = session.createProducer(testTopic);

        int count = 100;
        String batchPrefix = "First";
        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, batchPrefix, count);
        jmsProvider.sendMessages(messageProducer, listMsgs);
        log.info("First batch messages sent");

        List<javax.jms.Message> recvd1 = jmsProvider.receiveMessages(subscriber1, count);
        List<javax.jms.Message> recvd2 = jmsProvider.receiveMessages(subscriber2, count);

        assertThat("Wrong count of messages received: by " + sub1ID, recvd1.size(), is(count));
        jmsProvider.assertMessageContent(recvd1, batchPrefix);
        log.info(sub1ID + " :First batch messages received");

        assertThat("Wrong count of messages received: by " + sub2ID, recvd2.size(), is(count));
        jmsProvider.assertMessageContent(recvd2, batchPrefix);
        log.info(sub2ID + " :First batch messages received");

        subscriber1.close();
        log.info(sub1ID + " : closed");

        batchPrefix = "Second";
        listMsgs = jmsProvider.generateMessages(session, batchPrefix, count);
        jmsProvider.sendMessages(messageProducer, listMsgs);
        log.info("Second batch messages sent");

        recvd2 = jmsProvider.receiveMessages(subscriber2, count);
        assertThat("Wrong count of messages received: by " + sub2ID, recvd2.size(), is(count));
        jmsProvider.assertMessageContent(recvd2, batchPrefix);
        log.info(sub2ID + " :Second batch messages received");

        subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        log.info(sub1ID + " :connected");

        recvd1 = jmsProvider.receiveMessages(subscriber1, count);
        assertThat("Wrong count of messages received: by " + sub1ID, recvd1.size(), is(count));
        jmsProvider.assertMessageContent(recvd1, batchPrefix);
        log.info(sub1ID + " :Second batch messages received");

        subscriber1.close();
        subscriber2.close();

        session.unsubscribe(sub1ID);
        session.unsubscribe(sub2ID);
    }

    @Test
    void testMessageDurableSubscriptionTransacted(JmsProvider jmsProvider) throws Exception {
        Destination addressTopic = Destination.topic("jmsTopic", getDefaultPlan(AddressType.TOPIC));
        setAddresses(addressTopic);

        connection = jmsProvider.createConnection(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials,
                "jmsCliId", addressTopic);
        connection.start();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getAddress());

        String sub1ID = "sub1DurSubTrans";
        String sub2ID = "sub2DurSubTrans";

        MessageConsumer subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        MessageConsumer subscriber2 = session.createDurableSubscriber(testTopic, sub2ID);
        MessageProducer messageProducer = session.createProducer(testTopic);

        int count = 100;
        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, count);
        jmsProvider.sendMessages(messageProducer, listMsgs);
        session.commit();
        log.info("messages sent");

        List<javax.jms.Message> recvd1 = jmsProvider.receiveMessages(subscriber1, count);
        session.commit();
        List<javax.jms.Message> recvd2 = jmsProvider.receiveMessages(subscriber2, count);
        session.commit();

        log.info(sub1ID + " :messages received");
        log.info(sub2ID + " :messages received");

        assertAll(
                () -> assertThat("Wrong count of messages received: by " + sub1ID, recvd1.size(), is(count)),
                () -> assertThat("Wrong count of messages received: by " + sub2ID, recvd2.size(), is(count)));

        subscriber1.close();
        subscriber2.close();

        session.unsubscribe(sub1ID);
        session.unsubscribe(sub2ID);
    }

    @Test
    void testSharedDurableSubscription(JmsProvider jmsProvider) throws Exception {
        Destination addressTopic = Destination.topic("jmsTopic", getDefaultPlan(AddressType.TOPIC));
        setAddresses(addressTopic);

        Context context1 = jmsProvider.createContextForShared(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials, addressTopic);
        Connection connection1 = jmsProvider.createConnection(context1);
        Context context2 = jmsProvider.createContextForShared(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials, addressTopic);
        Connection connection2 = jmsProvider.createConnection(context2);
        connection1.start();
        connection2.start();

        Session session = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session2 = connection2.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getAddress());

        String subID = "sharedConsumerDurable123";

        MessageConsumer subscriber1 = session.createSharedDurableConsumer(testTopic, subID);
        MessageConsumer subscriber2 = session2.createSharedDurableConsumer(testTopic, subID);
        MessageProducer messageProducer = session.createProducer(testTopic);
        messageProducer.setDeliveryMode(DeliveryMode.PERSISTENT);

        int count = 10;
        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, count);
        jmsProvider.sendMessages(messageProducer, listMsgs);
        log.info("messages sent");

        List<javax.jms.Message> recvd1 = jmsProvider.receiveMessages(subscriber1, count, 1);
        List<javax.jms.Message> recvd2 = jmsProvider.receiveMessages(subscriber2, count, 1);

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
    void testSharedNonDurableSubscription(JmsProvider jmsProvider) throws Exception {
        Destination addressTopic = Destination.topic("jmsTopic", getDefaultPlan(AddressType.TOPIC));
        setAddresses(addressTopic);

        Context context1 = jmsProvider.createContextForShared(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials, addressTopic);
        Connection connection1 = jmsProvider.createConnection(context1);
        Context context2 = jmsProvider.createContextForShared(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials, addressTopic);
        Connection connection2 = jmsProvider.createConnection(context2);
        connection1.start();
        connection2.start();

        Session session = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session2 = connection2.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getAddress());
        String subID = "sharedConsumerNonDurable123";
        MessageConsumer subscriber1 = session.createSharedConsumer(testTopic, subID);
        MessageConsumer subscriber2 = session2.createSharedConsumer(testTopic, subID);
        MessageConsumer subscriber3 = session2.createSharedConsumer(testTopic, subID);
        MessageProducer messageProducer = session.createProducer(testTopic);
        messageProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        int count = 10;
        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, count);
        List<CompletableFuture<List<javax.jms.Message>>> results = jmsProvider.receiveMessagesAsync(count, subscriber1, subscriber2, subscriber3);
        jmsProvider.sendMessages(messageProducer, listMsgs);
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
