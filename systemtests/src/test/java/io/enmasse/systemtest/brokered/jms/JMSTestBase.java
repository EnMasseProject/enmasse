package io.enmasse.systemtest.brokered.jms;

import io.enmasse.systemtest.Logging;
import io.enmasse.systemtest.BrokeredTestBase;

import javax.jms.*;
import javax.naming.Context;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;

public class JMSTestBase extends BrokeredTestBase {

    protected Hashtable<Object, Object> setUpEnv(String url, String username, String password, Map<String, String> prop) {
        return setUpEnv(url, username, password, "", prop);
    }

    protected Hashtable<Object, Object> setUpEnv(String url, String username, String password, String clientID, Map<String, String> prop) {
        Hashtable env = new Hashtable<Object, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        StringBuilder urlParam = new StringBuilder();
        urlParam.append("?transport.trustAll=true")
                .append("&jms.password=").append(username)
                .append("&jms.username=").append(password)
                .append("&transport.verifyHost=false");
        urlParam.append(clientID.isEmpty() ? clientID : "&jms.clientID=" + clientID);

        env.put("connectionfactory.qpidConnectionFactory", url + urlParam);
        for (Map.Entry<String, String> entry : prop.entrySet()) {
            env.put(entry.getKey(), entry.getValue());
        }
        return env;
    }

    protected List<Message> generateMessages(Session session, int count) {
        return generateMessages(session, "", count);
    }

    protected List<Message> generateMessages(Session session, String prefix, int count) {
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


    protected void sendMessages(MessageProducer producer, List<Message> messages) {
        messages.forEach(m -> {
            try {
                producer.send(m);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    protected List<CompletableFuture<List<Message>>> receiveMessagesAsync(int count, MessageConsumer... consumer) throws JMSException {
        AtomicInteger totalCount = new AtomicInteger(count);
        List<CompletableFuture<List<Message>>> resultsList = new ArrayList<>();
        List<List<Message>> receivedResList = new ArrayList<>();

        for (int i = 0; i < consumer.length; i++) {
            final int index = i;
            resultsList.add(new CompletableFuture<>());
            receivedResList.add(new ArrayList<>());
            MessageListener myListener = message -> {
                Logging.log.info("Mesages received" + message + " count: " + totalCount.get());
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

    protected CompletableFuture<List<Message>> receiveMessagesAsync(MessageConsumer consumer, AtomicInteger totalCount) throws JMSException {
        CompletableFuture<List<Message>> received = new CompletableFuture<>();
        List<Message> recvd = new ArrayList<>();
        MessageListener myListener = message -> {
            Logging.log.info("Mesages received" + message + " count: " + totalCount.get());
            recvd.add(message);
            if (totalCount.decrementAndGet() == 0) {
                received.complete(recvd);
            }
        };
        consumer.setMessageListener(myListener);
        return received;
    }

    protected List<Message> receiveMessages(MessageConsumer consumer, int count) {
        return receiveMessages(consumer, count, 0);
    }

    protected List<Message> receiveMessages(MessageConsumer consumer, int count, long timeout) {
        List<Message> recvd = new ArrayList<>();
        IntStream.range(0, count).forEach(i -> {
            try {
                recvd.add(timeout > 0 ? consumer.receive(timeout) : consumer.receive());
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
        return recvd;
    }

    protected void assertMessageContent(List<Message> msgs, String content) {
        msgs.forEach(m -> {
            try {
                assertTrue(((TextMessage) m).getText().contains(content));
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }
}
