/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingAddress;
import io.enmasse.api.model.MessagingAddressBuilder;
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingProject;
import io.enmasse.api.model.MessagingProjectBuilder;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.framework.LoggerUtils;
import io.enmasse.systemtest.messaginginfra.resources.MessagingEndpointResourceType;
import io.enmasse.systemtest.resolvers.JmsProviderParameterResolver;
import io.enmasse.systemtest.utils.JmsProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DefaultMessagingInfrastructure
@ExtendWith(JmsProviderParameterResolver.class)
public class MessagingAddressJMSTest extends TestBase {
    private static final Logger LOGGER = LoggerUtils.getLogger();

    private MessagingProject project;
    private MessagingEndpoint endpoint;

    @BeforeAll
    public void createProject(ExtensionContext extensionContext) {
        project = new MessagingProjectBuilder()
                .editOrNewMetadata()
                .withName("default")
                .withNamespace(environment.namespace())
                .endMetadata()
                .editOrNewSpec()
                .addToCapabilities("transactional")
                .endSpec()
                .build();
        endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withName("messaging")
                .withNamespace(environment.namespace())
                .endMetadata()
                .editOrNewSpec()
                .editOrNewNodePort()
                .endNodePort()
                .withHost(kubernetes.getHost())
                .addToProtocols("AMQP")
                .endSpec()
                .build();
        resourceManager.createResource(extensionContext, project, endpoint);
    }

    private Context createContext(JmsProvider jmsProvider, MessagingAddress address) throws NamingException {
        return jmsProvider.createContext(endpoint.getStatus().getHost(), MessagingEndpointResourceType.getPort("AMQP", endpoint), false, null, null, "jmsCliId", address);
    }

    @Test
    @DisplayName("testTransactionCommitRejectQueue")
    @Disabled("Transaction test needs looking into")
    void testTransactionCommitRejectQueue(JmsProvider jmsProvider, ExtensionContext extensionContext) throws Exception {
        MessagingAddress addressQueue = new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-queue-commit")
                .endMetadata()
                .withNewSpec()
                .editOrNewQueue()
                .endQueue()
                .withAddress("jmsQueueCommit")
                .endSpec()
                .build();
        resourceManager.createResource(extensionContext, addressQueue);

        Context context = createContext(jmsProvider, addressQueue);

        Connection connection = jmsProvider.createConnection(context);
        connection.start();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue testQueue = (Queue) jmsProvider.getDestination(addressQueue.getSpec().getAddress());

        MessageProducer sender = session.createProducer(testQueue);
        MessageConsumer receiver = session.createConsumer(testQueue);
        List<javax.jms.Message> recvd;

        int count = 50;

        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, count);

        //send messages and commit
        jmsProvider.sendMessages(sender, listMsgs);
        session.commit();
        LOGGER.info("messages sent commit");

        //receive commit messages
        recvd = jmsProvider.receiveMessages(receiver, count, 1000);
        for (javax.jms.Message message : recvd) {
            assertNotNull(message, "No message received");
        }
        session.commit();
        LOGGER.info("messages received commit");

        //send messages rollback
        jmsProvider.sendMessages(sender, listMsgs);
        session.rollback();
        LOGGER.info("messages sent rollback");
        Thread.sleep(10_000);

        //check if queue is empty
        javax.jms.Message received = receiver.receive(1000);
        assertNull(received, "Queue should be empty");
        LOGGER.info("queue is empty");

        //send messages
        jmsProvider.sendMessages(sender, listMsgs);
        session.commit();
        LOGGER.info("messages sent commit");

        //receive messages rollback
        recvd.clear();
        recvd = jmsProvider.receiveMessages(receiver, count, 1000);
        for (javax.jms.Message message : recvd) {
            assertNotNull(message, "No message received");
        }
        session.rollback();
        LOGGER.info("messages received rollback");

        //receive messages
        recvd.clear();
        recvd = jmsProvider.receiveMessages(receiver, count, 1000);
        for (javax.jms.Message message : recvd) {
            assertNotNull(message, "No message received");
        }
        session.commit();
        LOGGER.info("messages received commit");

        sender.close();
        receiver.close();
    }

    @Test
    @DisplayName("testLoadMessagesQueue")
    void testLoadMessagesQueue(JmsProvider jmsProvider, ExtensionContext extensionContext) throws Exception {
        MessagingAddress addressQueue = new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-queue-load")
                .endMetadata()
                .withNewSpec()
                .editOrNewQueue()
                .endQueue()
                .withAddress("jmsQueueLoad")
                .endSpec()
                .build();
        resourceManager.createResource(extensionContext, addressQueue);

        Context context = createContext(jmsProvider, addressQueue);
        Connection connection = jmsProvider.createConnection(context);
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue testQueue = (Queue) jmsProvider.getDestination(addressQueue.getSpec().getAddress());

        MessageProducer sender = session.createProducer(testQueue);
        MessageConsumer receiver = session.createConsumer(testQueue);
        List<javax.jms.Message> recvd;
        List<javax.jms.Message> recvd2;

        int count = 60_895;
        int batch = new java.util.Random().nextInt(count) + 1;

        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, count);

        jmsProvider.sendMessages(sender, listMsgs);
        LOGGER.info("{} messages sent", count);

        recvd = jmsProvider.receiveMessages(receiver, batch, 1000);
        assertThat("Wrong count of received messages", recvd.size(), Matchers.is(batch));
        LOGGER.info("{} messages received", batch);

        recvd2 = jmsProvider.receiveMessages(receiver, count - batch, 1000);
        assertThat("Wrong count of received messages", recvd2.size(), Matchers.is(count - batch));
        LOGGER.info("{} messages received", count - batch);

        jmsProvider.sendMessages(sender, listMsgs);
        LOGGER.info("{} messages sent", count);

        recvd = jmsProvider.receiveMessages(receiver, count, 1000);
        assertThat("Wrong count of received messages", recvd.size(), Matchers.is(count));
        LOGGER.info("{} messages received", count);
        sender.close();
        receiver.close();
    }

    @Test
    @DisplayName("testLargeMessagesQueue")
    void testLargeMessagesQueue(JmsProvider jmsProvider, ExtensionContext extensionContext) throws Exception {
        MessagingAddress addressQueue = new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-queue-large")
                .endMetadata()
                .withNewSpec()
                .editOrNewQueue()
                .endQueue()
                .withAddress("jmsQueueLarge")
                .endSpec()
                .build();
        resourceManager.createResource(extensionContext, addressQueue);

        Context context = createContext(jmsProvider, addressQueue);
        Connection connection = jmsProvider.createConnection(context);
        connection.start();

        assertSendReceiveLargeMessageQueue(jmsProvider, 1, addressQueue, 1);
        assertSendReceiveLargeMessageQueue(jmsProvider, 0.5, addressQueue, 1);
        assertSendReceiveLargeMessageQueue(jmsProvider, 0.25, addressQueue, 1);
        assertSendReceiveLargeMessageQueue(jmsProvider, 1, addressQueue, 1, DeliveryMode.PERSISTENT);
        assertSendReceiveLargeMessageQueue(jmsProvider, 0.5, addressQueue, 1, DeliveryMode.PERSISTENT);
        assertSendReceiveLargeMessageQueue(jmsProvider, 0.25, addressQueue, 1, DeliveryMode.PERSISTENT);
        connection.stop();
        connection.close();
    }

    @Test
    @DisplayName("testMessageNonDurableSubscription")
    void testMessageNonDurableSubscription(JmsProvider jmsProvider, ExtensionContext extensionContext) throws Exception {
        MessagingAddress addressTopic = new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-topic-mess")
                .endMetadata()
                .withNewSpec()
                .editOrNewTopic()
                .endTopic()
                .withAddress("jmsTopicMess")
                .endSpec()
                .build();
        resourceManager.createResource(extensionContext, addressTopic);

        Context context = createContext(jmsProvider, addressTopic);
        Connection connection = jmsProvider.createConnection(context);
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getSpec().getAddress());

        MessageConsumer subscriber1 = session.createConsumer(testTopic);
        MessageProducer messageProducer = session.createProducer(testTopic);

        int count = 1000;
        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, count);

        CompletableFuture<List<Message>> received = new CompletableFuture<>();

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
        LOGGER.info("messages sent");

        assertThat("Wrong count of messages received", received.get(30, TimeUnit.SECONDS).size(), is(count));
        LOGGER.info("messages received");

        subscriber1.close();
        messageProducer.close();
    }

    @Test
    @Disabled("Not yet supported")
    @DisplayName("testMessageDurableSubscription")
    void testMessageDurableSubscription(JmsProvider jmsProvider, ExtensionContext extensionContext) throws Exception {
        MessagingAddress addressTopic = new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-topic-dur-subs")
                .endMetadata()
                .withNewSpec()
                .editOrNewTopic()
                .endTopic()
                .withAddress("jmsTopicDurSubs")
                .endSpec()
                .build();
        MessagingAddress addressSub1= new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-topic-dur-sub-1")
                .endMetadata()
                .withNewSpec()
                .editOrNewSubscription()
                .withTopic("jmsTopicDurSubs")
                .endSubscription()
                .withAddress("sub1DurSub")
                .endSpec()
                .build();
        MessagingAddress addressSub2= new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-topic-dur-sub-2")
                .endMetadata()
                .withNewSpec()
                .editOrNewSubscription()
                .withTopic("jmsTopicDurSubs")
                .endSubscription()
                .withAddress("sub2DurSub")
                .endSpec()
                .build();
        resourceManager.createResource(extensionContext, addressTopic, addressSub1, addressSub2);

        Context context = createContext(jmsProvider, addressTopic);
        Connection connection = jmsProvider.createConnection(context);
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getSpec().getAddress());

        String sub1ID = addressSub1.getSpec().getAddress();
        String sub2ID = addressSub2.getSpec().getAddress();
        MessageConsumer subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        MessageConsumer subscriber2 = session.createDurableSubscriber(testTopic, sub2ID);
        MessageProducer messageProducer = session.createProducer(testTopic);

        int count = 100;
        String batchPrefix = "First";
        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, batchPrefix, count);
        jmsProvider.sendMessages(messageProducer, listMsgs);
        LOGGER.info("First batch messages sent");

        List<javax.jms.Message> recvd1 = jmsProvider.receiveMessages(subscriber1, count);
        List<javax.jms.Message> recvd2 = jmsProvider.receiveMessages(subscriber2, count);

        assertThat("Wrong count of messages received: by " + sub1ID, recvd1.size(), is(count));
        jmsProvider.assertMessageContent(recvd1, batchPrefix);
        LOGGER.info(sub1ID + " :First batch messages received");

        assertThat("Wrong count of messages received: by " + sub2ID, recvd2.size(), is(count));
        jmsProvider.assertMessageContent(recvd2, batchPrefix);
        LOGGER.info(sub2ID + " :First batch messages received");

        subscriber1.close();
        LOGGER.info(sub1ID + " : closed");

        batchPrefix = "Second";
        listMsgs = jmsProvider.generateMessages(session, batchPrefix, count);
        jmsProvider.sendMessages(messageProducer, listMsgs);
        LOGGER.info("Second batch messages sent");

        recvd2 = jmsProvider.receiveMessages(subscriber2, count);
        assertThat("Wrong count of messages received: by " + sub2ID, recvd2.size(), is(count));
        jmsProvider.assertMessageContent(recvd2, batchPrefix);
        LOGGER.info(sub2ID + " :Second batch messages received");

        subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        LOGGER.info(sub1ID + " :connected");

        recvd1 = jmsProvider.receiveMessages(subscriber1, count);
        assertThat("Wrong count of messages received: by " + sub1ID, recvd1.size(), is(count));
        jmsProvider.assertMessageContent(recvd1, batchPrefix);
        LOGGER.info(sub1ID + " :Second batch messages received");

        subscriber1.close();
        subscriber2.close();

        session.unsubscribe(sub1ID);
        session.unsubscribe(sub2ID);
    }

    @Test
    @Disabled("Not yet supported")
    @DisplayName("testMessageDurableSubscriptionTransacted")
    void testMessageDurableSubscriptionTransacted(JmsProvider jmsProvider, ExtensionContext extensionContext) throws Exception {
        String topicAddress = "jmsTopicTrans";
        String sub1ID = "sub1DurSubTrans";
        String sub2ID = "sub2DurSubTrans";
        MessagingAddress addressTopic = new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-topic-trans")
                .endMetadata()
                .withNewSpec()
                .editOrNewTopic()
                .endTopic()
                .withAddress(topicAddress)
                .endSpec()
                .build();
        MessagingAddress addressSub1= new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-topic-trans-sub1")
                .endMetadata()
                .withNewSpec()
                .editOrNewSubscription()
                .withTopic(topicAddress)
                .endSubscription()
                .withAddress(sub1ID)
                .endSpec()
                .build();
        MessagingAddress addressSub2= new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-topic-trans-sub2")
                .endMetadata()
                .withNewSpec()
                .editOrNewSubscription()
                .withTopic(topicAddress)
                .endSubscription()
                .withAddress(sub2ID)
                .endSpec()
                .build();
        resourceManager.createResource(extensionContext, addressTopic, addressSub1, addressSub2);

        Context context = createContext(jmsProvider, addressTopic);
        Connection connection = jmsProvider.createConnection(context);
        connection.start();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getSpec().getAddress());


        MessageConsumer subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        MessageConsumer subscriber2 = session.createDurableSubscriber(testTopic, sub2ID);
        MessageProducer messageProducer = session.createProducer(testTopic);

        int count = 100;
        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, count);
        jmsProvider.sendMessages(messageProducer, listMsgs);
        session.commit();
        LOGGER.info("messages sent");

        List<javax.jms.Message> recvd1 = jmsProvider.receiveMessages(subscriber1, count);
        session.commit();
        List<javax.jms.Message> recvd2 = jmsProvider.receiveMessages(subscriber2, count);
        session.commit();

        LOGGER.info(sub1ID + " :messages received");
        LOGGER.info(sub2ID + " :messages received");

        assertAll(
                () -> assertThat("Wrong count of messages received: by " + sub1ID, recvd1.size(), is(count)),
                () -> assertThat("Wrong count of messages received: by " + sub2ID, recvd2.size(), is(count)));

        subscriber1.close();
        subscriber2.close();

        session.unsubscribe(sub1ID);
        session.unsubscribe(sub2ID);
    }

    @Test
    @Disabled("Not yet supported")
    @DisplayName("testSharedDurableSubscription")
    void testSharedDurableSubscription(JmsProvider jmsProvider, ExtensionContext extensionContext) throws Exception {
        String topicAddress = "jmsTopicDurable";
        String subID = "sharedConsumerDurable123";
        MessagingAddress addressTopic = new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-topic-durable")
                .endMetadata()
                .withNewSpec()
                .editOrNewTopic()
                .endTopic()
                .withAddress(topicAddress)
                .endSpec()
                .build();
        MessagingAddress addressSub1= new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-topic-durable-sub")
                .endMetadata()
                .withNewSpec()
                .editOrNewSubscription()
                .withTopic(topicAddress)
                .endSubscription()
                .withAddress(subID)
                .endSpec()
                .build();
        resourceManager.createResource(extensionContext, addressTopic, addressSub1);

        Context context1 = createContext(jmsProvider, addressTopic);
        Connection connection1 = jmsProvider.createConnection(context1);
        Context context2 = createContext(jmsProvider, addressTopic);
        Connection connection2 = jmsProvider.createConnection(context2);
        connection1.start();
        connection2.start();

        Session session = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session2 = connection2.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic testTopic = (Topic) jmsProvider.getDestination(topicAddress);

        LOGGER.info("Creating subscriber 1");
        MessageConsumer subscriber1 = session.createSharedDurableConsumer(testTopic, subID);
        LOGGER.info("Creating subscriber 2");
        MessageConsumer subscriber2 = session2.createSharedDurableConsumer(testTopic, subID);
        LOGGER.info("Creating producer");
        MessageProducer messageProducer = session.createProducer(testTopic);
        messageProducer.setDeliveryMode(DeliveryMode.PERSISTENT);

        int count = 10;
        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, count);
        jmsProvider.sendMessages(messageProducer, listMsgs);
        LOGGER.info("messages sent");

        List<javax.jms.Message> recvd1 = jmsProvider.receiveMessages(subscriber1, count, 1);
        List<javax.jms.Message> recvd2 = jmsProvider.receiveMessages(subscriber2, count, 1);

        LOGGER.info(subID + " :messages received");

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
    @Disabled("javax.jms.JMSException: Remote peer does not support shared subscriptions")
    @DisplayName("testSharedNonDurableSubscription")
    void testSharedNonDurableSubscription(JmsProvider jmsProvider, ExtensionContext extensionContext) throws Exception {
        String topicAddress = "jmsTopicNonDurable";
        String subID = "sharedConsumerDurable123";
        MessagingAddress addressTopic = new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-topic-nondurable")
                .endMetadata()
                .withNewSpec()
                .editOrNewTopic()
                .endTopic()
                .withAddress(topicAddress)
                .endSpec()
                .build();
        resourceManager.createResource(extensionContext, addressTopic);

        Context context1 = createContext(jmsProvider, addressTopic);
        Connection connection1 = jmsProvider.createConnection(context1);
        Context context2 = createContext(jmsProvider, addressTopic);
        Connection connection2 = jmsProvider.createConnection(context2);
        connection1.start();
        connection2.start();

        Session session = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session2 = connection2.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic testTopic = (Topic) jmsProvider.getDestination(addressTopic.getSpec().getAddress());
        MessageConsumer subscriber1 = session.createSharedConsumer(testTopic, subID);
        MessageConsumer subscriber2 = session2.createSharedConsumer(testTopic, subID);
        MessageConsumer subscriber3 = session2.createSharedConsumer(testTopic, subID);
        MessageProducer messageProducer = session.createProducer(testTopic);
        messageProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        int count = 10;
        List<javax.jms.Message> listMsgs = jmsProvider.generateMessages(session, count);
        List<CompletableFuture<List<javax.jms.Message>>> results = jmsProvider.receiveMessagesAsync(count, subscriber1, subscriber2, subscriber3);
        jmsProvider.sendMessages(messageProducer, listMsgs);
        LOGGER.info("messages sent");

        assertThat("Each message should be received only by one consumer",
                results.get(0).get(20, TimeUnit.SECONDS).size() +
                        results.get(1).get(20, TimeUnit.SECONDS).size() +
                        results.get(2).get(20, TimeUnit.SECONDS).size(),
                is(count));
        LOGGER.info("messages received");

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

    @Test
    @DisplayName("testLargeMessages")
    void testLargeMessages(JmsProvider jmsProvider, ExtensionContext extensionContext) throws Exception {
        MessagingAddress addressTopic = new MessagingAddressBuilder()
                .withNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("jms-topic-large")
                .endMetadata()
                .withNewSpec()
                .editOrNewTopic()
                .endTopic()
                .withAddress("jmsTopicLarge")
                .endSpec()
                .build();
        resourceManager.createResource(extensionContext, addressTopic);

        Context context = createContext(jmsProvider, addressTopic);
        Connection connection = jmsProvider.createConnection(context);
        connection.start();

        assertSendReceiveLargeMessageTopic(jmsProvider, 1, addressTopic, 1);
        assertSendReceiveLargeMessageTopic(jmsProvider, 0.5, addressTopic, 1);
        assertSendReceiveLargeMessageTopic(jmsProvider, 0.25, addressTopic, 1);
        connection.stop();
        connection.close();
    }

    private void assertSendReceiveLargeMessageQueue(JmsProvider jmsProvider, double sizeInMB, MessagingAddress dest, int count) throws Exception {
        assertSendReceiveLargeMessageQueue(jmsProvider, sizeInMB, dest, count, DeliveryMode.NON_PERSISTENT);
    }

    private void assertSendReceiveLargeMessageQueue(JmsProvider jmsProvider, double sizeInMB, MessagingAddress dest, int count, int mode) throws Exception {
        int size = (int) (sizeInMB * 1024 * 1024);

        Session session = jmsProvider.getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
        javax.jms.Queue testQueue = (javax.jms.Queue) jmsProvider.getDestination(dest.getSpec().getAddress());
        List<javax.jms.Message> messages = jmsProvider.generateMessages(session, count, size);

        MessageProducer sender = session.createProducer(testQueue);
        MessageConsumer receiver = session.createConsumer(testQueue);

        assertSendReceiveLargeMessage(jmsProvider, sender, receiver, sizeInMB, mode, count, messages);

    }

    private void assertSendReceiveLargeMessageTopic(JmsProvider jmsProvider, double sizeInMB, MessagingAddress dest, int count) throws Exception {
        assertSendReceiveLargeMessageTopic(jmsProvider, sizeInMB, dest, count, DeliveryMode.NON_PERSISTENT);
    }

    private void assertSendReceiveLargeMessageTopic(JmsProvider jmsProvider, double sizeInMB, MessagingAddress dest, int count, int mode) throws Exception {
        int size = (int) (sizeInMB * 1024 * 1024);

        Session session = jmsProvider.getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
        javax.jms.Topic testTopic = (javax.jms.Topic) jmsProvider.getDestination(dest.getSpec().getAddress());
        List<javax.jms.Message> messages = jmsProvider.generateMessages(session, count, size);

        MessageProducer sender = session.createProducer(testTopic);
        MessageConsumer receiver = session.createConsumer(testTopic);

        assertSendReceiveLargeMessage(jmsProvider, sender, receiver, sizeInMB, mode, count, messages);
        session.close();
        sender.close();
        receiver.close();
    }

    private void assertSendReceiveLargeMessage(JmsProvider jmsProvider, MessageProducer sender, MessageConsumer receiver, double sizeInMB, int mode, int count, List<javax.jms.Message> messages) {
        List<javax.jms.Message> recvd;

        jmsProvider.sendMessages(sender, messages, mode, javax.jms.Message.DEFAULT_PRIORITY, javax.jms.Message.DEFAULT_TIME_TO_LIVE);
        LOGGER.info("{}MB {} message sent", sizeInMB, mode == DeliveryMode.PERSISTENT ? "durable" : "non-durable");

        recvd = jmsProvider.receiveMessages(receiver, count, 2000);
        assertThat("Wrong count of received messages", recvd.size(), Matchers.is(count));
        LOGGER.info("{}MB {} message received", sizeInMB, mode == DeliveryMode.PERSISTENT ? "durable" : "non-durable");
    }
}
