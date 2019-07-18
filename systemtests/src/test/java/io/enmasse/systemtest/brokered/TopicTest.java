/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.JmsProvider;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.resolvers.JmsProviderParameterResolver;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.enmasse.systemtest.TestTag.nonPR;
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
    void testTopicPubSubWildcards() throws Exception {
        doTopicWildcardTest(DestinationPlan.BROKERED_TOPIC);
    }

    private void doTopicWildcardTest(String plan) throws Exception {
        Address t0 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("topic")
                .withPlan(plan)
                .endSpec()
                .build();
        setAddresses(t0);

        AmqpClient amqpClient = amqpClientFactory.createTopicClient();

        List<String> msgs = Arrays.asList("foo", "bar", "baz", "qux");

        Future<List<org.apache.qpid.proton.message.Message>> recvResults = amqpClient.recvMessages("topic/#", msgs.size() * 3);

        amqpClient.sendMessages(t0.getSpec().getAddress() + "/foo", msgs);
        amqpClient.sendMessages(t0.getSpec().getAddress() + "/bar", msgs);
        amqpClient.sendMessages(t0.getSpec().getAddress() + "/baz/foobar", msgs);

        assertThat("Wrong count of messages received",
                recvResults.get(1, TimeUnit.MINUTES).size(), is(msgs.size() * 3));

        recvResults = amqpClient.recvMessages("topic/world/+", msgs.size() * 2);

        amqpClient.sendMessages(t0.getSpec().getAddress() + "/world/africa", msgs);
        amqpClient.sendMessages(t0.getSpec().getAddress() + "/world/europe", msgs);
        amqpClient.sendMessages(t0.getSpec().getAddress() + "/world/asia/maldives", msgs);

        assertThat("Wrong count of messages received",
                recvResults.get(1, TimeUnit.MINUTES).size(), is(msgs.size() * 2));
    }

    @Test
    @Tag(nonPR)
    @Disabled("issue #2852")
    void testRestApi() throws Exception {
        Address t1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "topic1"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("topic1")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();
        Address t2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "topic2"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("topic2")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();

        runRestApiTest(sharedAddressSpace, t1, t2);
    }

    @Test
    void testMessageSubscription(JmsProvider jmsProvider) throws Exception {
        Address addressTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "jms-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("jmsTopic")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();
        setAddresses(addressTopic);

        connection = jmsProvider.createConnection(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials,
                "jmsCliId", addressTopic);
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getSpec().getAddress());

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
        Address addressTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "jms-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("jmsTopic")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();
        setAddresses(addressTopic);

        connection = jmsProvider.createConnection(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials,
                "jmsCliId", addressTopic);
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getSpec().getAddress());

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
        Address addressTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "jms-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("jmsTopic")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();
        setAddresses(addressTopic);

        connection = jmsProvider.createConnection(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials,
                "jmsCliId", addressTopic);
        connection.start();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getSpec().getAddress());

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
        Address addressTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "jms-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("jmsTopic")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();
        setAddresses(addressTopic);

        Context context1 = jmsProvider.createContextForShared(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials, addressTopic);
        Connection connection1 = jmsProvider.createConnection(context1);
        Context context2 = jmsProvider.createContextForShared(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials, addressTopic);
        Connection connection2 = jmsProvider.createConnection(context2);
        connection1.start();
        connection2.start();

        Session session = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session2 = connection2.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getSpec().getAddress());

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
        Address addressTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "jms-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("jmsTopic")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();
        setAddresses(addressTopic);

        Context context1 = jmsProvider.createContextForShared(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials, addressTopic);
        Connection connection1 = jmsProvider.createConnection(context1);
        Context context2 = jmsProvider.createContextForShared(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials, addressTopic);
        Connection connection2 = jmsProvider.createConnection(context2);
        connection1.start();
        connection2.start();

        Session session = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session2 = connection2.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getSpec().getAddress());
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
