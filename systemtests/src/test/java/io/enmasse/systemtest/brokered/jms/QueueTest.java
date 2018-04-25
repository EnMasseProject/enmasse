/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.jms;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.JmsProvider;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.resolvers.JmsProviderParameterResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


@ExtendWith(JmsProviderParameterResolver.class)
public class QueueTest extends TestBaseWithShared implements ITestBaseBrokered {
    private static Logger log = CustomLogger.getLogger();

    private Hashtable<Object, Object> env;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private Context context;
    private String queue = "jmsQueue";
    private Destination addressQueue;

    @BeforeEach
    public void setUp(JmsProvider jmsProvider) throws Exception {
        addressQueue = Destination.queue(queue, getDefaultPlan(AddressType.QUEUE));
        setAddresses(addressQueue);

        env = jmsProvider.setUpEnv("amqps://" + getMessagingRoute(sharedAddressSpace).toString(),
                defaultCredentials.getUsername(), defaultCredentials.getPassword(), "testJmsCliId",
                new HashMap<String, String>() {{
                    put("queue." + queue, queue);
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


    @Test
    public void transactionCommitRejectTest(JmsProvider jmsProvider) throws Exception {
        log.info("testTransactionCommitReject");
        session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue testQueue = (Queue) context.lookup(queue);

        MessageProducer sender = session.createProducer(testQueue);
        MessageConsumer receiver = session.createConsumer(testQueue);
        List<Message> recvd;

        int count = 50;

        List<Message> listMsgs = jmsProvider.generateMessages(session, count);

        //send messages and commit
        jmsProvider.sendMessages(sender, listMsgs);
        session.commit();
        log.info("messages sent commit");

        //receive commit messages
        recvd = jmsProvider.receiveMessages(receiver, count, 1000);
        for (Message message : recvd) {
            assertNotNull(message, "No message received");
        }
        session.commit();
        log.info("messages received commit");

        //send messages rollback
        jmsProvider.sendMessages(sender, listMsgs);
        session.rollback();
        log.info("messages sent rollback");

        //check if queue is empty
        Message received = receiver.receive(1000);
        assertNull(received, "Queue should be empty");
        log.info("queue is empty");

        //send messages
        jmsProvider.sendMessages(sender, listMsgs);
        session.commit();
        log.info("messages sent commit");

        //receive messages rollback
        recvd.clear();
        recvd = jmsProvider.receiveMessages(receiver, count, 1000);
        for (Message message : recvd) {
            assertNotNull(message, "No message received");
        }
        session.rollback();
        log.info("messages received rollback");

        //receive messages
        recvd.clear();
        recvd = jmsProvider.receiveMessages(receiver, count, 1000);
        for (Message message : recvd) {
            assertNotNull(message, "No message received");
        }
        session.commit();
        log.info("messages received commit");

        sender.close();
        receiver.close();
    }

    @Test
    public void testLoadMessages(JmsProvider jmsProvider) throws Exception {
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue testQueue = (Queue) context.lookup(queue);

        MessageProducer sender = session.createProducer(testQueue);
        MessageConsumer receiver = session.createConsumer(testQueue);
        List<Message> recvd;
        List<Message> recvd2;

        int count = 60_895;
        int batch = new java.util.Random().nextInt(count) + 1;

        List<Message> listMsgs = jmsProvider.generateMessages(session, count);

        jmsProvider.sendMessages(sender, listMsgs, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
        log.info("{} messages sent", count);

        recvd = jmsProvider.receiveMessages(receiver, batch, 1000);
        assertThat("Wrong count fo received messages", recvd.size(), is(batch));
        log.info("{} messages received", batch);

        recvd2 = jmsProvider.receiveMessages(receiver, count - batch, 1000);
        assertThat("Wrong count fo received messages", recvd2.size(), is(count - batch));
        log.info("{} messages received", count - batch);

        jmsProvider.sendMessages(sender, listMsgs, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
        log.info("{} messages sent", count);

        recvd = jmsProvider.receiveMessages(receiver, count, 1000);
        assertThat("Wrong count fo received messages", recvd.size(), is(count));
        log.info("{} messages received", count);

        sender.close();
        receiver.close();
    }
}
