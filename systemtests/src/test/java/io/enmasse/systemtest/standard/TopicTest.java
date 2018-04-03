/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.TestUtils;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.TopicTerminusFactory;
import io.enmasse.systemtest.bases.StandardTestBase;
import org.apache.qpid.proton.amqp.DescribedType;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TopicTest extends StandardTestBase {
    private static Logger log = CustomLogger.getLogger();

    public static void runTopicTest(AmqpClient client, Destination dest)
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        runTopicTest(client, dest, 1024);
    }

    public static void runTopicTest(AmqpClient client, Destination dest, int msgCount)
            throws InterruptedException, IOException, TimeoutException, ExecutionException {
        List<String> msgs = TestUtils.generateMessages(msgCount);
        Future<List<Message>> recvMessages = client.recvMessages(dest.getAddress(), msgCount);

        assertThat("Wrong count of messages sent",
                client.sendMessages(dest.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat("Wrong count of messages received",
                recvMessages.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    @Test
    public void testColocatedTopics() throws Exception {
        Destination t1 = Destination.topic("col-topic1", "pooled-topic");
        Destination t2 = Destination.topic("col-topic2", "pooled-topic");
        Destination t3 = Destination.topic("col-topic3", "pooled-topic");
        setAddresses(t1, t2, t3);

        AmqpClient client = amqpClientFactory.createTopicClient();
        runTopicTest(client, t1);
        runTopicTest(client, t2);
        runTopicTest(client, t3);
    }

    @Test
    public void testShardedTopic() throws Exception {
        Destination t1 = Destination.topic("shardedTopic", "sharded-topic");
        setAddresses(t1);

        AmqpClient topicClient = amqpClientFactory.createTopicClient();
        runTopicTest(topicClient, t1, 2048);
    }

    @Test
    public void testRestApi() throws Exception {
        Destination t1 = Destination.topic("topicRest1", getDefaultPlan(AddressType.TOPIC));
        Destination t2 = Destination.topic("topicRest2", getDefaultPlan(AddressType.TOPIC));

        runRestApiTest(t1, t2);
    }

    @Test
    public void testMessageSelectorsAppProperty() throws Exception {
        Destination selTopic = Destination.topic("selectorTopicAppProp", "sharded-topic");
        String linkName = "linkSelectorTopicAppProp";
        setAddresses(selTopic);

        AmqpClient topicClient = amqpClientFactory.createTopicClient();

        Map<String, Object> appProperties = new HashMap<>();
        appProperties.put("appPar1", 1);
        assertAppProperty(topicClient, linkName, appProperties, "appPar1 = 1", selTopic);

        appProperties.clear();
        appProperties.put("appPar2", 10);
        assertAppProperty(topicClient, linkName, appProperties, "appPar2 > 9", selTopic);

        appProperties.clear();
        appProperties.put("appPar3", 10);
        assertAppProperty(topicClient, linkName, appProperties, "appPar3 < 11", selTopic);

        appProperties.clear();
        appProperties.put("appPar4", 10);
        assertAppProperty(topicClient, linkName, appProperties, "appPar4 * 2 > 10", selTopic);

        appProperties.clear();
        appProperties.put("year", 2000);
        assertAppProperty(topicClient, linkName, appProperties, "(year > 1000) AND (year < 3000)", selTopic);

        appProperties.clear();
        appProperties.put("year2", 2000);
        assertAppProperty(topicClient, linkName, appProperties, "year2 BETWEEN 1999 AND 2018", selTopic);

        appProperties.clear();
        appProperties.put("appPar5", "1");
        assertAppProperty(topicClient, linkName, appProperties, "appPar5 = '1'", selTopic);

        appProperties.clear();
        appProperties.put("appPar6", true);
        assertAppProperty(topicClient, linkName, appProperties, "appPar6 = TRUE", selTopic);

        appProperties.clear();
        appProperties.put("appPar7", "SOMETHING");
        assertAppProperty(topicClient, linkName, appProperties, "appPar7 IS NOT NULL", selTopic);

        appProperties.clear();
        appProperties.put("appPar8", "SOMETHING");
        assertAppProperty(topicClient, linkName, appProperties, "appPar8 LIKE '%THING' ", selTopic);

        appProperties.clear();
        appProperties.put("appPar9", "bar");
        assertAppProperty(topicClient, linkName, appProperties, "appPar9 IN ('foo', 'bar', 'baz')", selTopic);

    }

    public void assertAppProperty(AmqpClient client, String linkName, Map<String, Object> appProperties, String selector, Destination dest) throws Exception {
        log.info("Application property selector: " + selector);
        int msgsCount = 10;
        List<Message> listOfMessages = new ArrayList<>();
        for (int i = 0; i < msgsCount; i++) {
            Message msg = Message.Factory.create();
            msg.setAddress(dest.getAddress());
            msg.setBody(new AmqpValue(dest.getAddress()));
            msg.setSubject("subject");
            listOfMessages.add(msg);
        }

        //set appProperty for last message
        if (listOfMessages.size() > 0) {
            listOfMessages.get(msgsCount - 1).setApplicationProperties(new ApplicationProperties(appProperties));
        }

        Source source = new Source();
        source.setAddress(dest.getAddress());
        source.setCapabilities(Symbol.getSymbol("topic"));
        Map<Symbol, DescribedType> map = new HashMap<>();
        map.put(Symbol.valueOf("jms-selector"), new AmqpJmsSelectorFilter(selector));
        source.setFilter(map);

        Future<List<Message>> received = client.recvMessages(source, linkName, 1);
        AmqpClient client2 = amqpClientFactory.createTopicClient();
        Future<List<Message>> receivedWithoutSel = client2.recvMessages(dest.getAddress(), msgsCount - 1);

        Future<Integer> sent = client.sendMessages(dest.getAddress(), listOfMessages.toArray(new Message[listOfMessages.size()]));

        assertThat("Wrong count of messages sent",
                sent.get(1, TimeUnit.MINUTES), is(msgsCount));
        assertThat("Wrong count of messages received",
                received.get(1, TimeUnit.MINUTES).size(), is(1));

        Map.Entry<String, Object> entry = appProperties.entrySet().iterator().next();
        assertThat("Wrong application property",
                received.get(1, TimeUnit.MINUTES)
                        .get(0)
                        .getApplicationProperties()
                        .getValue()
                        .get(entry.getKey()),
                is(entry.getValue()));

        //receive rest of messages
        assertThat("Wrong count of messages received",
                receivedWithoutSel.get(1, TimeUnit.MINUTES).size(), is(msgsCount - 1));
    }

    @Test
    public void testMessageSelectorsProperty() throws Exception {
        Destination selTopic = Destination.topic("selectorTopicProp", "sharded-topic");
        String linkName = "linkSelectorTopicProp";
        setAddresses(selTopic);

        int msgsCount = 10;
        List<Message> listOfMessages = new ArrayList<>();
        for (int i = 0; i < msgsCount; i++) {
            Message msg = Message.Factory.create();
            msg.setAddress(selTopic.getAddress());
            msg.setBody(new AmqpValue(selTopic.getAddress()));
            msg.setSubject("subject");
            listOfMessages.add(msg);
        }

        //set property for last message
        String groupID = "testGroupID";
        if (msgsCount > 0) {
            listOfMessages.get(msgsCount - 1).setGroupId(groupID);
        }

        Source source = new Source();
        source.setAddress(selTopic.getAddress());
        source.setCapabilities(Symbol.getSymbol("topic"));
        Map<Symbol, DescribedType> map = new HashMap<>();
        map.put(Symbol.valueOf("jms-selector"), new AmqpJmsSelectorFilter("JMSXGroupID IS NOT NULL"));
        source.setFilter(map);

        AmqpClient client = amqpClientFactory.createTopicClient();
        Future<List<Message>> received = client.recvMessages(source, linkName, 1);

        Future<Integer> sent = client.sendMessages(selTopic.getAddress(), listOfMessages.toArray(new Message[listOfMessages.size()]));

        assertThat("Wrong count of messages sent", sent.get(1, TimeUnit.MINUTES), is(msgsCount));

        assertThat("Wrong count of messages received",
                received.get(1, TimeUnit.MINUTES).size(), is(1));
        assertThat("Message with wrong groupID received",
                received.get(1, TimeUnit.MINUTES).get(0).getGroupId(), is(groupID));
    }

    public void testTopicWildcards() throws Exception {
        Destination t1 = Destination.topic("topic/No1", "sharded-topic");
        Destination t2 = Destination.topic("topic/No2", "sharded-topic");

        setAddresses(t1, t2);
        AmqpClient amqpClient = amqpClientFactory.createTopicClient();

        List<String> msgs = Arrays.asList("foo", "bar", "baz", "qux");
        Thread.sleep(60_000);

        Future<List<Message>> recvResults = amqpClient.recvMessages("topic/#", msgs.size() * 2);

        List<Future<Integer>> sendResult = Arrays.asList(
                amqpClient.sendMessages(t1.getAddress(), msgs),
                amqpClient.sendMessages(t2.getAddress(), msgs));

        assertThat("Wrong count of messages sent: sender0",
                sendResult.get(0).get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat("Wrong count of messages sent: sender1",
                sendResult.get(1).get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat("Wrong count of messages received",
                recvResults.get(1, TimeUnit.MINUTES).size(), is(msgs.size() * 2));
    }

    public void testDurableLinkRoutedSubscription() throws Exception {
        Destination dest = Destination.topic("lrtopic", "sharded-topic");
        String linkName = "systest-durable";
        setAddresses(dest);
        scale(dest, 4);

        Thread.sleep(60_000);

        Source source = new TopicTerminusFactory().getSource("locate/" + dest.getAddress());
        source.setDurable(TerminusDurability.UNSETTLED_STATE);

        AmqpClient client = amqpClientFactory.createTopicClient();
        List<String> batch1 = Arrays.asList("one", "two", "three");

        log.info("Receiving first batch");
        Future<List<Message>> recvResults = client.recvMessages(source, linkName, batch1.size());

        // Wait for the redirect to kick in
        Thread.sleep(30_000);

        log.info("Sending first batch");
        assertThat("Wrong count of messages sent: batch1",
                client.sendMessages(dest.getAddress(), batch1).get(1, TimeUnit.MINUTES), is(batch1.size()));
        assertThat("Wrong count of messages received: batch1",
                recvResults.get(1, TimeUnit.MINUTES), is(batch1));

        log.info("Sending second batch");
        List<String> batch2 = Arrays.asList("four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve");
        assertThat("Wrong count of messages sent: batch2",
                client.sendMessages(dest.getAddress(), batch2).get(1, TimeUnit.MINUTES), is(batch2.size()));

        log.info("Done, waiting for 20 seconds");
        Thread.sleep(20_000);

        source.setAddress("locate/" + dest.getAddress());
        //at present may get one or more of the first three messages
        //redelivered due to DISPATCH-595, so use more lenient checks
        log.info("Receiving second batch again");
        recvResults = client.recvMessages(source, linkName, message -> {
            String body = (String) ((AmqpValue) message.getBody()).getValue();
            log.info("received " + body);
            return "twelve".equals(body);
        });
        assertTrue(recvResults.get(1, TimeUnit.MINUTES).containsAll(batch2),
                "Wrong count of messages received: batch2");
    }

    public void testDurableMessageRoutedSubscription() throws Exception {
        Destination dest = Destination.topic("mrtopic", "sharded-topic");
        String address = "myaddress";
        log.info("Deploying");
        setAddresses(dest);
        log.info("Scaling");
        scale(dest, 1);

        Thread.sleep(120_000);

        AmqpClient subClient = amqpClientFactory.createQueueClient();
        AmqpClient queueClient = amqpClientFactory.createQueueClient();
        AmqpClient topicClient = amqpClientFactory.createTopicClient();

        Message sub = Message.Factory.create();
        sub.setAddress("$subctrl");
        sub.setCorrelationId(address);
        sub.setSubject("subscribe");
        sub.setBody(new AmqpValue(dest.getAddress()));

        log.info("Sending subscribe");
        subClient.sendMessages("$subctrl", sub).get(1, TimeUnit.MINUTES);

        log.info("Sending 12 messages");

        List<String> msgs = TestUtils.generateMessages(12);
        assertThat("Wrong count of messages sent",
                topicClient.sendMessages(dest.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        log.info("Receiving 6 messages");
        Future<List<Message>> recvResult = queueClient.recvMessages(address, 6);
        assertThat("Wrong count of messages received",
                recvResult.get(1, TimeUnit.MINUTES).size(), is(6));

        // Do scaledown and 'reconnect' receiver and verify that we got everything

        /*
        scale(dest, 3);
        Thread.sleep(5_000);
        scale(dest, 2);
        Thread.sleep(5_000);
        scale(dest, 1);

        Thread.sleep(30_000);
        */

        log.info("Receiving another 6 messages");
        recvResult = queueClient.recvMessages(address, 6);
        assertThat("Wrong count of messages received",
                recvResult.get(1, TimeUnit.MINUTES).size(), is(6));

        Message unsub = Message.Factory.create();
        unsub.setAddress("$subctrl");
        unsub.setCorrelationId(address);
        sub.setBody(new AmqpValue(dest.getAddress()));
        unsub.setSubject("unsubscribe");
        log.info("Sending unsubscribe");
        subClient.sendMessages("$subctrl", unsub).get(1, TimeUnit.MINUTES);
    }

    public class AmqpJmsSelectorFilter implements DescribedType {

        private final String selector;

        public AmqpJmsSelectorFilter(String selector) {
            this.selector = selector;
        }

        @Override
        public Object getDescriptor() {
            return Symbol.valueOf("apache.org:selector-filter:string");
        }

        @Override
        public Object getDescribed() {
            return this.selector;
        }

        @Override
        public String toString() {
            return "AmqpJmsSelectorType{" + selector + "}";
        }
    }
}
