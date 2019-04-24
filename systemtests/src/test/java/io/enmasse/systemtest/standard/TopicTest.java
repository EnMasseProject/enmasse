/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard;

import io.enmasse.address.model.Address;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.apache.qpid.proton.amqp.DescribedType;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.nonPR;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TopicTest extends TestBaseWithShared implements ITestBaseStandard {
    private static Logger log = CustomLogger.getLogger();

    private static void runTopicTest(AmqpClient client, Address dest)
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        runTopicTest(client, dest, 1024);
    }

    public static void runTopicTest(AmqpClient client, Address dest, int msgCount)
            throws InterruptedException, IOException, TimeoutException, ExecutionException {
        List<String> msgs = TestUtils.generateMessages(msgCount);
        Future<List<Message>> recvMessages = client.recvMessages(dest.getSpec().getAddress(), msgCount);

        assertThat("Wrong count of messages sent",
                client.sendMessages(dest.getSpec().getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat("Wrong count of messages received",
                recvMessages.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    @Test
    @Tag(nonPR)
    void testColocatedTopics() throws Exception {
        Address t1 = AddressUtils.createTopicAddressObject("col-topic1", DestinationPlan.STANDARD_SMALL_TOPIC);
        Address t2 = AddressUtils.createTopicAddressObject("col-topic2", DestinationPlan.STANDARD_SMALL_TOPIC);
        Address t3 = AddressUtils.createTopicAddressObject("col-topic3", DestinationPlan.STANDARD_SMALL_TOPIC);
        setAddresses(t1, t2, t3);

        AmqpClient client = amqpClientFactory.createTopicClient();
        runTopicTest(client, t1);
        runTopicTest(client, t2);
        runTopicTest(client, t3);
    }

    @Test
    void testShardedTopic() throws Exception {
        Address t1 = AddressUtils.createTopicAddressObject("shardedTopic1", DestinationPlan.STANDARD_LARGE_TOPIC);
        Address t2 = AddressUtils.createAddressObject("shardedTopic2", null, sharedAddressSpace.getMetadata().getName(), "sharded_addr_2", AddressType.TOPIC.toString(), DestinationPlan.STANDARD_LARGE_TOPIC);
        addressApiClient.createAddress(t2);

        appendAddresses(t1);
        waitForDestinationsReady(t2);

        AmqpClient topicClient = amqpClientFactory.createTopicClient();
        runTopicTest(topicClient, t1, 2048);
        runTopicTest(topicClient, t2, 2048);
    }

    @Test
    @Tag(nonPR)
    void testRestApi() throws Exception {
        Address t1 = AddressUtils.createTopicAddressObject("topicRest1", getDefaultPlan(AddressType.TOPIC));
        Address t2 = AddressUtils.createTopicAddressObject("topicRest2", getDefaultPlan(AddressType.TOPIC));

        runRestApiTest(sharedAddressSpace, t1, t2);
    }

    @Test
    @Tag(nonPR)
    void testMessageSelectorsAppProperty() throws Exception {
        Address selTopic = AddressUtils.createTopicAddressObject("selectorTopicAppProp", DestinationPlan.STANDARD_LARGE_TOPIC);
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

    private void assertAppProperty(AmqpClient client, String linkName, Map<String, Object> appProperties, String selector, Address dest) throws Exception {
        log.info("Application property selector: " + selector);
        int msgsCount = 10;
        List<Message> listOfMessages = new ArrayList<>();
        for (int i = 0; i < msgsCount; i++) {
            Message msg = Message.Factory.create();
            msg.setAddress(dest.getSpec().getAddress());
            msg.setBody(new AmqpValue(dest.getSpec().getAddress()));
            msg.setSubject("subject");
            listOfMessages.add(msg);
        }

        //set appProperty for last message
        if (listOfMessages.size() > 0) {
            listOfMessages.get(msgsCount - 1).setApplicationProperties(new ApplicationProperties(appProperties));
        }

        Source source = new Source();
        source.setAddress(dest.getSpec().getAddress());
        source.setCapabilities(Symbol.getSymbol("topic"));
        Map<Symbol, DescribedType> map = new HashMap<>();
        map.put(Symbol.valueOf("jms-selector"), new AmqpJmsSelectorFilter(selector));
        source.setFilter(map);

        Future<List<Message>> received = client.recvMessages(source, linkName, 1);
        AmqpClient client2 = amqpClientFactory.createTopicClient();
        Future<List<Message>> receivedWithoutSel = client2.recvMessages(dest.getSpec().getAddress(), msgsCount - 1);

        Future<Integer> sent = client.sendMessages(dest.getSpec().getAddress(), listOfMessages.toArray(new Message[0]));

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
    void testMessageSelectorsProperty() throws Exception {
        Address selTopic = AddressUtils.createTopicAddressObject("selectorTopicProp", DestinationPlan.STANDARD_LARGE_TOPIC);
        String linkName = "linkSelectorTopicProp";
        setAddresses(selTopic);

        int msgsCount = 10;
        List<Message> listOfMessages = new ArrayList<>();
        for (int i = 0; i < msgsCount; i++) {
            Message msg = Message.Factory.create();
            msg.setAddress(selTopic.getSpec().getAddress());
            msg.setBody(new AmqpValue(selTopic.getSpec().getAddress()));
            msg.setSubject("subject");
            listOfMessages.add(msg);
        }

        //set property for last message
        String groupID = "testGroupID";
        listOfMessages.get(msgsCount - 1).setGroupId(groupID);

        Source source = new Source();
        source.setAddress(selTopic.getSpec().getAddress());
        source.setCapabilities(Symbol.getSymbol("topic"));
        Map<Symbol, DescribedType> map = new HashMap<>();
        map.put(Symbol.valueOf("jms-selector"), new AmqpJmsSelectorFilter("JMSXGroupID IS NOT NULL"));
        source.setFilter(map);

        AmqpClient client = amqpClientFactory.createTopicClient();
        Future<List<Message>> received = client.recvMessages(source, linkName, 1);

        Future<Integer> sent = client.sendMessages(selTopic.getSpec().getAddress(), listOfMessages.toArray(new Message[0]));

        assertThat("Wrong count of messages sent", sent.get(1, TimeUnit.MINUTES), is(msgsCount));

        assertThat("Wrong count of messages received",
                received.get(1, TimeUnit.MINUTES).size(), is(1));
        assertThat("Message with wrong groupID received",
                received.get(1, TimeUnit.MINUTES).get(0).getGroupId(), is(groupID));
    }

    static List<String> extractBodyAsString(Future<List<Message>> msgs) throws Exception {
        return msgs.get(1, TimeUnit.MINUTES).stream().map(m -> (String) ((AmqpValue) m.getBody()).getValue()).collect(Collectors.toList());
    }

    @Test
    void testDurableSubscriptionOnPooledTopic() throws Exception {
        Address topic = AddressUtils.createTopicAddressObject("mytopic", DestinationPlan.STANDARD_SMALL_TOPIC);
        Address subscription = AddressUtils.createSubscriptionAddressObject("mysub", "mytopic", DestinationPlan.STANDARD_SMALL_SUBSCRIPTION);
        setAddresses(topic, subscription);

        AmqpClient client = amqpClientFactory.createTopicClient();
        List<String> batch1 = Arrays.asList("one", "two", "three");

        log.info("Receiving first batch");
        Future<List<Message>> recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription), batch1.size());

        log.info("Sending first batch");
        assertThat("Wrong count of messages sent: batch1",
                client.sendMessages(topic.getSpec().getAddress(), batch1).get(1, TimeUnit.MINUTES), is(batch1.size()));
        assertThat("Wrong messages received: batch1", extractBodyAsString(recvResults), is(batch1));

        log.info("Sending second batch");
        List<String> batch2 = Arrays.asList("four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve");
        assertThat("Wrong messages sent: batch2",
                client.sendMessages(topic.getSpec().getAddress(), batch2).get(1, TimeUnit.MINUTES), is(batch2.size()));

        log.info("Receiving second batch");
        recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription), batch2.size());
        assertThat("Wrong messages received: batch2", extractBodyAsString(recvResults), is(batch2));
    }

    @Test
    void testDurableSubscriptionOnShardedTopic() throws Exception {
        Address topic = AddressUtils.createTopicAddressObject("mytopic", DestinationPlan.STANDARD_LARGE_TOPIC);
        Address subscription1 = AddressUtils.createSubscriptionAddressObject("mysub", "mytopic", DestinationPlan.STANDARD_SMALL_SUBSCRIPTION);
        Address subscription2 = AddressUtils.createSubscriptionAddressObject("anothersub", "mytopic", DestinationPlan.STANDARD_SMALL_SUBSCRIPTION);
        setAddresses(topic, subscription1, subscription2);

        AmqpClient client = amqpClientFactory.createTopicClient();
        List<String> batch1 = Arrays.asList("one", "two", "three");

        log.info("Receiving first batch");
        Future<List<Message>> recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription1), batch1.size());

        log.info("Sending first batch");
        assertThat("Wrong count of messages sent: batch1",
                client.sendMessages(topic.getSpec().getAddress(), batch1).get(1, TimeUnit.MINUTES), is(batch1.size()));
        assertThat("Wrong messages received: batch1", extractBodyAsString(recvResults), is(batch1));

        log.info("Sending second batch");
        List<String> batch2 = Arrays.asList("four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve");
        assertThat("Wrong count of messages sent: batch2",
                client.sendMessages(topic.getSpec().getAddress(), batch2).get(1, TimeUnit.MINUTES), is(batch2.size()));

        log.info("Receiving second batch");
        recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription1), batch2.size());
        assertThat("Wrong messages received: batch2", extractBodyAsString(recvResults), is(batch2));

        log.info("Receiving messages from second subscription");
        List<String> allmessages = new ArrayList<>(batch1);
        allmessages.addAll(batch2);
        AmqpClient client2 = amqpClientFactory.createTopicClient();
        recvResults = client2.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription2), allmessages.size());
        assertThat("Wrong messages received for second subscription", extractBodyAsString(recvResults), is(allmessages));
    }

    @Test
    void testDurableSubscriptionOnShardedTopic2() throws Exception {
        Address topic = AddressUtils.createTopicAddressObject("mytopic", DestinationPlan.STANDARD_LARGE_TOPIC);
        Address subscription1 = AddressUtils.createSubscriptionAddressObject("mysub", "mytopic", DestinationPlan.STANDARD_SMALL_SUBSCRIPTION);
        setAddresses(topic, subscription1);

        AmqpClient client = amqpClientFactory.createTopicClient();
        List<String> batch1 = Arrays.asList("one", "two", "three");

        log.info("Sending first batch");
        assertThat("Wrong count of messages sent: batch1",
                client.sendMessages(topic.getSpec().getAddress(), batch1).get(1, TimeUnit.MINUTES), is(batch1.size()));

        log.info("Receiving first batch");
        Future<List<Message>> recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription1), batch1.size());
        assertThat("Wrong messages received: batch1", extractBodyAsString(recvResults), is(batch1));

        log.info("Creating second subscription");
        Address subscription2 = AddressUtils.createSubscriptionAddressObject("anothersub", "mytopic", DestinationPlan.STANDARD_SMALL_SUBSCRIPTION);
        appendAddresses(subscription2);

        log.info("Sending second batch");
        List<String> batch2 = Arrays.asList("four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve");

        assertThat("Wrong count of messages sent: batch2",
                client.sendMessages(topic.getSpec().getAddress(), batch2).get(1, TimeUnit.MINUTES), is(batch2.size()));
        log.info("Receiving second batch");
        recvResults = client.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription1), batch2.size());
        assertThat("Wrong messages received: batch2", extractBodyAsString(recvResults), is(batch2));

        log.info("Receiving messages from second subscription");
        AmqpClient client2 = amqpClientFactory.createTopicClient();
        recvResults = client2.recvMessages(AddressUtils.getQualifiedSubscriptionAddress(subscription2), batch2.size());
        assertThat("Wrong messages received for second subscription", extractBodyAsString(recvResults), is(batch2));
    }

    @Test
    void testTopicWildcardsSharded() throws Exception {
        doTopicWildcardTest(DestinationPlan.STANDARD_LARGE_TOPIC);
    }

    @Test
    void testTopicWildcardsPooled() throws Exception {
        doTopicWildcardTest(DestinationPlan.STANDARD_SMALL_TOPIC);
    }

    private void doTopicWildcardTest(String plan) throws Exception {
        Address t0 = AddressUtils.createTopicAddressObject("topic", plan);
        setAddresses(t0);

        AmqpClient amqpClient = amqpClientFactory.createTopicClient();

        List<String> msgs = Arrays.asList("foo", "bar", "baz", "qux");

        Future<List<Message>> recvResults = amqpClient.recvMessages("topic/#", msgs.size() * 3);

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

    class AmqpJmsSelectorFilter implements DescribedType {

        private final String selector;

        AmqpJmsSelectorFilter(String selector) {
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
