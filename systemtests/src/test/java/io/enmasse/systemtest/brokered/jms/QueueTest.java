package io.enmasse.systemtest.brokered.jms;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.Destination;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class QueueTest extends JMSTestBase {

    private Hashtable<Object, Object> env;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private Context context;
    private String queue = "jmsQueue";
    private Destination addressQueue;

    private String jmsUsername = "test";
    private String jmsPassword = "test";
    private String jmsClientID = "testClient";

    @Before
    public void setUp() throws Exception {
        addressQueue = Destination.queue(queue);
        setAddresses(defaultAddressSpace, addressQueue);

        env = setUpEnv("amqps://" + getRouteEndpoint(defaultAddressSpace).toString(), jmsUsername, jmsPassword, jmsClientID,
                new HashMap<String, String>() {{
                    put("queue." + queue, queue);
                }});
        context = new InitialContext(env);
        connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionFactory");
        connection = connectionFactory.createConnection();
        connection.start();
    }

    @After
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
    public void transactionCommitRejectTest() throws Exception {
        Logging.log.info("testTransactionCommitReject");
        session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue testQueue = (Queue) context.lookup(queue);

        MessageProducer sender = session.createProducer(testQueue);
        MessageConsumer receiver = session.createConsumer(testQueue);
        List<Message> recvd;

        int count = 50;

        List<Message> listMsgs = generateMessages(session, count);

        //send messages and commit
        sendMessages(sender, listMsgs);
        session.commit();
        Logging.log.info("messages sent commit");

        //receive commit messages
        recvd = receiveMessages(receiver, count, 1000);
        for (Message message : recvd){
            assertNotNull(message);
        }
        session.commit();
        Logging.log.info("messages received commit");

        //send messages rollback
        sendMessages(sender, listMsgs);
        session.rollback();
        Logging.log.info("messages sent rollback");

        //check if queue is empty
        Message received = receiver.receive(1000);
        assertNull(received);
        Logging.log.info("queue is empty");

        //send messages
        sendMessages(sender, listMsgs);
        session.commit();
        Logging.log.info("messages sent commit");

        //receive messages rollback
        recvd.clear();
        recvd = receiveMessages(receiver, count, 1000);
        for (Message message : recvd){
            assertNotNull(message);
        }
        session.rollback();
        Logging.log.info("messages received rollback");

        //receive messages
        recvd.clear();
        recvd = receiveMessages(receiver, count, 1000);
        for (Message message : recvd){
            assertNotNull(message);
        }
        session.commit();
        Logging.log.info("messages received commit");

        sender.close();
        receiver.close();
    }
}
