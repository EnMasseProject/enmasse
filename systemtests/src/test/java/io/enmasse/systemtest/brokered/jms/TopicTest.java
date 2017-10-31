package io.enmasse.systemtest.brokered.jms;


import io.enmasse.systemtest.*;
import io.enmasse.systemtest.Destination;
import org.junit.*;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TopicTest extends MultiTenantTestBase {

    private Hashtable<Object, Object> env;
    private Connection connection;
    private Session session;
    private Context context;
    private String topic = "jmsTopic";
    private Destination addressTopic;

    private String jmsUsername = "test";
    private String jmsPassword = "test";
    private String jmsClientID = "testClient";

    @Before
    public void setUp() throws Exception {
        AddressSpace addressSpace = new AddressSpace(
                "brokered-space-jms-topics",
                "brokered-space-jms-topics",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");

        addressTopic = Destination.topic(topic);
        setAddresses(addressSpace, addressTopic);

        env = new Hashtable<Object, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        env.put("connectionfactory.qpidConnectionFactory", "amqps://" + getRouteEndpoint(addressSpace).toString() +
                "?jms.clientID=" + jmsClientID +
                "&transport.trustAll=true" +
                "&jms.password=" + jmsUsername +
                "&jms.username=" + jmsPassword +
                "&transport.verifyHost=false");
        env.put("topic." + topic, topic);

        context = new InitialContext(env);
        ConnectionFactory connectionFactory
                = (ConnectionFactory) context.lookup("qpidConnectionFactory");
        connection = connectionFactory.createConnection();
        connection.start();
    }

    @After
    public void tearDown() throws Exception {
        deleteAddresses(addressTopic);
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
    public void testMessageSubscription() throws Exception {
        Logging.log.info("testMessageSubscription");
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
        Logging.log.info("messages sent");

        assertThat(received.get(30, TimeUnit.SECONDS).size(), is(count));
        Logging.log.info("messages received");

        subscriber1.close();
        messageProducer.close();
    }

    @Test
    public void testMessageDurableSubscription() throws Exception {
        Logging.log.info("testMessageDurableSubscription");
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic testTopic = (Topic) context.lookup(topic);

        String sub1ID = "sub1";
        String sub2ID = "sub2";
        MessageConsumer subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        MessageConsumer subscriber2 = session.createDurableSubscriber(testTopic, sub2ID);
        MessageProducer messageProducer = session.createProducer(testTopic);

        int count = 100;
        String batchPrefix = "First";
        List<Message> listMsgs = generateMessages(session, batchPrefix, count);
        sendMessages(messageProducer, listMsgs);
        Logging.log.info("First batch messages sent");

        List<Message> recvd1 = receiveMessages(subscriber1, count);
        List<Message> recvd2 = receiveMessages(subscriber2, count);

        assertThat(recvd1.size(), is(count));
        assertMessageContent(recvd1, batchPrefix);
        Logging.log.info(sub1ID + " :First batch messages received");

        assertThat(recvd2.size(), is(count));
        assertMessageContent(recvd2, batchPrefix);
        Logging.log.info(sub2ID + " :First batch messages received");

        subscriber1.close();
        Logging.log.info(sub1ID + " : closed");

        batchPrefix = "Second";
        listMsgs = generateMessages(session, batchPrefix, count);
        sendMessages(messageProducer, listMsgs);
        Logging.log.info("Second batch messages sent");

        recvd2 = receiveMessages(subscriber2, count);
        assertThat(recvd2.size(), is(count));
        assertMessageContent(recvd2, batchPrefix);
        Logging.log.info(sub2ID + " :Second batch messages received");

        subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        Logging.log.info(sub1ID + " :connected");

        recvd1 = receiveMessages(subscriber1, count);
        assertThat(recvd1.size(), is(count));
        assertMessageContent(recvd1, batchPrefix);
        Logging.log.info(sub1ID + " :Second batch messages received");

        subscriber1.close();
        subscriber2.close();

        session.unsubscribe(sub1ID);
        session.unsubscribe(sub2ID);
    }

    @Test
    public void testMessageDurableSubscriptionTransacted() throws Exception {
        Logging.log.info("testMessageDurableSubscriptionTransacted");
        session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Topic testTopic = (Topic) context.lookup(topic);

        String sub1ID = "sub1";
        String sub2ID = "sub2";
        MessageConsumer subscriber1 = session.createDurableSubscriber(testTopic, sub1ID);
        MessageConsumer subscriber2 = session.createDurableSubscriber(testTopic, sub2ID);
        MessageProducer messageProducer = session.createProducer(testTopic);

        int count = 100;
        List<Message> listMsgs = generateMessages(session, count);
        sendMessages(messageProducer, listMsgs);
        session.commit();
        Logging.log.info("messages sent");

        List<Message> recvd1 = receiveMessages(subscriber1, count);
        session.commit();
        List<Message> recvd2 = receiveMessages(subscriber2, count);
        session.commit();

        Logging.log.info(sub1ID + " :messages received");
        Logging.log.info(sub2ID + " :messages received");

        assertThat(recvd1.size(), is(count));
        assertThat(recvd2.size(), is(count));

        subscriber1.close();
        subscriber2.close();

        session.unsubscribe(sub1ID);
        session.unsubscribe(sub2ID);
    }

    private List<Message> generateMessages(Session session, String prefix, int count) {
        List<Message> messages = new ArrayList<>();
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

    private List<Message> generateMessages(Session session, int count) {
        return generateMessages(session, "", count);
    }

    private void sendMessages(MessageProducer producer, List<Message> messages) {
        messages.forEach(m -> {
            try {
                producer.send(m);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    private List<Message> receiveMessages(MessageConsumer consumer, int count) {
        List<Message> recvd = new ArrayList<>();
        IntStream.range(0, count).forEach(i -> {
            try {
                recvd.add(consumer.receive());
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
        return recvd;
    }

    private void assertMessageContent(List<Message> msgs, String content) {
        msgs.forEach(m -> {
            try {
                assertTrue(((TextMessage) m).getText().contains(content));
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

}
