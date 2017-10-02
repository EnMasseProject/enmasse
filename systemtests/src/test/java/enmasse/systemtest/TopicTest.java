/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.systemtest;

import enmasse.systemtest.amqp.AmqpClient;
import enmasse.systemtest.amqp.TopicTerminusFactory;

import org.apache.qpid.proton.amqp.DescribedType;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.*;
import org.apache.qpid.proton.amqp.messaging.Properties;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TopicTest extends TestBase {

    @Test
    public void testMultipleSubscribers() throws Exception {
        Destination dest = Destination.topic("multiple-subtopic");
        setAddresses(dest);
        scale(dest, 1);
        Thread.sleep(60_000);
        AmqpClient client = amqpClientFactory.createTopicClient();
        List<String> msgs = TestUtils.generateMessages(1000);

        List<Future<List<Message>>> recvResults = Arrays.asList(
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()));

        Thread.sleep(60_000);

        assertThat(client.sendMessages(dest.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertThat(recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(2).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(3).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(4).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(5).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }


    public void testInmemoryTopics() throws Exception {
        Destination t1 = Destination.topic("inMemoryTopic", Optional.of("inmemory"));
        setAddresses(t1);

        AmqpClient topicClient = createTopicClient();
        runTopicTest(topicClient, t1, 2048);
    }

    public void testPersistedTopics() throws Exception {
        Destination t1 = Destination.topic("persistedTopic", Optional.of("persisted"));
        setAddresses(t1);

        AmqpClient topicClient = createTopicClient();
        runTopicTest(topicClient, t1, 2048);
    }

    public void runTopicTest(AmqpClient client, Destination dest, int msgCount) throws InterruptedException, IOException, TimeoutException, ExecutionException, IOException, TimeoutException, ExecutionException {
        List<String> msgs = TestUtils.generateMessages(msgCount);
        Future<List<Message>> recvMessages = client.recvMessages(dest.getAddress(), msgCount);

        assertThat(client.sendMessages(dest.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat(recvMessages.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    @Test
    public void testMessageSelectorsAppProperty() throws Exception {
        Destination selTopic = Destination.topic("selectorTopicAppProp");
        String linkName = "linkSelectorTopicAppProp";
        setAddresses(selTopic);

        Thread.sleep(30_000);

        AmqpClient topicClient = createTopicClient();

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

    public void assertAppProperty(AmqpClient client, String linkName, Map<String, Object> appProperties, String selector, Destination dest) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        Logging.log.info("Application property selector: " + selector);
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
        AmqpClient client2 = createTopicClient();
        Future<List<Message>> receivedWithoutSel = client2.recvMessages(dest.getAddress(), msgsCount - 1);
        Thread.sleep(10_000);

        Future<Integer> sent = client.sendMessages(dest.getAddress(), listOfMessages.toArray(new Message[listOfMessages.size()]));

        assertThat(sent.get(1, TimeUnit.MINUTES), is(msgsCount));
        assertThat(received.get(1, TimeUnit.MINUTES).size(), is(1));

        Map.Entry<String, Object> entry = appProperties.entrySet().iterator().next();
        assertThat(received.get(1, TimeUnit.MINUTES).get(0).getApplicationProperties().getValue().get(entry.getKey()), is(entry.getValue()));

        //receive rest of messages
        assertThat(receivedWithoutSel.get(1, TimeUnit.MINUTES).size(), is(msgsCount - 1));
    }

    @Test
    public void testMessageSelectorsProperty() throws Exception {
        Destination selTopic = Destination.topic("selectorTopicProp");
        String linkName = "linkSelectorTopicAppProp";
        setAddresses(selTopic);

        Thread.sleep(30_000);

        int msgsCount = 10;
        List<Message> listOfMessages = new ArrayList<>();
        for (int i = 0; i < msgsCount; i++) {
            Message msg = Message.Factory.create();
            msg.setAddress(selTopic.getAddress());
            msg.setBody(new AmqpValue(selTopic.getAddress()));
            msg.setSubject("subject");
            listOfMessages.add(msg);
        }

//        set appProperty for last message
        short correlationID = 1;
        if (msgsCount > 0) {
            Properties prop = new Properties();
            prop.setCorrelationId(correlationID);
            listOfMessages.get(msgsCount - 1).setCorrelationId(correlationID);
        }

        Source source = new Source();
        source.setAddress(selTopic.getAddress());
        source.setCapabilities(Symbol.getSymbol("topic"));
        Map<Symbol, DescribedType> map = new HashMap<>();
        map.put(Symbol.valueOf("jms-selector"), new AmqpJmsSelectorFilter("JMSCorrelationID IS NOT NULL"));
        source.setFilter(map);

        AmqpClient client = createTopicClient();
        Future<List<Message>> received = client.recvMessages(source, linkName, 1);

        Thread.sleep(10_000);

        Future<Integer> sent = client.sendMessages(selTopic.getAddress(), listOfMessages.toArray(new Message[listOfMessages.size()]));

        assertThat(sent.get(1, TimeUnit.MINUTES), is(msgsCount));

        assertThat(received.get(1, TimeUnit.MINUTES).size(), is(1));
        assertThat(received.get(1, TimeUnit.MINUTES).get(0).getCorrelationId(), is(correlationID));
    }

    public void testTopicWildcards() throws Exception {
        Destination t1 = Destination.topic("topic/No1");
        Destination t2 = Destination.topic("topic/No2");

        setAddresses(t1, t2);
        AmqpClient amqpClient = this.createTopicClient();

        List<String> msgs = Arrays.asList("foo", "bar", "baz", "qux");
        Thread.sleep(60_000);

        Future<List<Message>> recvResults = amqpClient.recvMessages("topic/#", msgs.size() * 2);

        List<Future<Integer>> sendResult = Arrays.asList(
                amqpClient.sendMessages(t1.getAddress(), msgs),
                amqpClient.sendMessages(t2.getAddress(), msgs));

        assertThat(sendResult.get(0).get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat(sendResult.get(1).get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat(recvResults.get(1, TimeUnit.MINUTES).size(), is(msgs.size() * 2));
    }

    public void testDurableLinkRoutedSubscription() throws Exception {
        Destination dest = Destination.topic("lrtopic");
        String linkName = "systest-durable";
        setAddresses(dest);
        scale(dest, 4);

        Thread.sleep(60_000);

        Source source = new TopicTerminusFactory().getSource("locate/" + dest.getAddress());
        source.setDurable(TerminusDurability.UNSETTLED_STATE);
//        source.setExpiryPolicy(TerminusExpiryPolicy.NEVER);
//        source.setCapabilities(Symbol.getSymbol("topic"));

        AmqpClient client = amqpClientFactory.createTopicClient();
        List<String> batch1 = Arrays.asList("one", "two", "three");

        Logging.log.info("Receiving first batch");
        Future<List<Message>> recvResults = client.recvMessages(source, linkName, batch1.size());

        // Wait for the redirect to kick in
        Thread.sleep(30_000);

        Logging.log.info("Sending first batch");
        assertThat(client.sendMessages(dest.getAddress(), batch1).get(1, TimeUnit.MINUTES), is(batch1.size()));
        assertThat(recvResults.get(1, TimeUnit.MINUTES), is(batch1));

        Logging.log.info("Sending second batch");
        List<String> batch2 = Arrays.asList("four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve");
        assertThat(client.sendMessages(dest.getAddress(), batch2).get(1, TimeUnit.MINUTES), is(batch2.size()));

        Logging.log.info("Done, waiting for 20 seconds");
        Thread.sleep(20_000);

        source.setAddress("locate/" + dest.getAddress());
        //at present may get one or more of the first three messages
        //redelivered due to DISPATCH-595, so use more lenient checks
        Logging.log.info("Receiving second batch again");
        recvResults = client.recvMessages(source, linkName, message -> {
            String body = (String) ((AmqpValue) message.getBody()).getValue();
            Logging.log.info("received " + body);
            return "twelve".equals(body);
        });
        assertTrue(recvResults.get(1, TimeUnit.MINUTES).containsAll(batch2));
    }

    @Test
    public void testDurableMessageRoutedSubscription() throws Exception {
        Destination dest = Destination.topic("mrtopic");
        String address = "myaddress";
        Logging.log.info("Deploying");
        setAddresses(dest);
        Logging.log.info("Scaling");
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

        Logging.log.info("Sending subscribe");
        subClient.sendMessages("$subctrl", sub).get(1, TimeUnit.MINUTES);

        Logging.log.info("Sending 12 messages");

        List<String> msgs = TestUtils.generateMessages(12);
        assertThat(topicClient.sendMessages(dest.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        Logging.log.info("Receiving 6 messages");
        Future<List<Message>> recvResult = queueClient.recvMessages(address, 6);
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(6));

        // Do scaledown and 'reconnect' receiver and verify that we got everything

        /*
        scale(dest, 3);
        Thread.sleep(5_000);
        scale(dest, 2);
        Thread.sleep(5_000);
        scale(dest, 1);

        Thread.sleep(30_000);
        */

        Logging.log.info("Receiving another 6 messages");
        recvResult = queueClient.recvMessages(address, 6);
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(6));

        Message unsub = Message.Factory.create();
        unsub.setAddress("$subctrl");
        unsub.setCorrelationId(address);
        sub.setBody(new AmqpValue(dest.getAddress()));
        unsub.setSubject("unsubscribe");
        Logging.log.info("Sending unsubscribe");
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
