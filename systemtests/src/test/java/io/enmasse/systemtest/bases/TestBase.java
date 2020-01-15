/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.bases;

import com.google.common.collect.Ordering;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.IndicativeSentences;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.broker.BrokerManagement;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.listener.JunitCallbackListener;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.mqtt.MqttUtils;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.selenium.SeleniumManagement;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.JmsProvider;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import javax.jms.DeliveryMode;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base class for all tests
 */
@ExtendWith(JunitCallbackListener.class)
@DisplayNameGeneration(IndicativeSentences.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TestBase implements ITestBase, ITestSeparator {
    protected static final UserCredentials clusterUser = new UserCredentials(KubeCMDClient.getOCUser());
    protected static final Environment environment = Environment.getInstance();
    protected static final GlobalLogCollector logCollector = new GlobalLogCollector(kubernetes, environment.testLogDir());
    protected ResourceManager resourcesManager;
    protected UserCredentials defaultCredentials = null;
    protected UserCredentials managementCredentials = null;

    @BeforeEach
    public void initTest() throws Exception {
        LOGGER.info("Test init");
        resourcesManager = getResourceManager();
        if (TestInfo.getInstance().isTestShared()) {
            defaultCredentials = environment.getSharedDefaultCredentials();
            managementCredentials = environment.getSharedManagementCredentials();
            resourcesManager.setAddressSpacePlan(getDefaultAddressSpacePlan());
            resourcesManager.setAddressSpaceType(getAddressSpaceType().toString());
            resourcesManager.setDefaultAddSpaceIdentifier(getDefaultAddrSpaceIdentifier());
            if (resourcesManager.getSharedAddressSpace() == null) {
                resourcesManager.setup();
            }
        } else {
            defaultCredentials = environment.getDefaultCredentials();
            managementCredentials = environment.getManagementCredentials();
            resourcesManager.setup();
        }
    }

    //================================================================================================
    //======================================= Help methods ===========================================
    //================================================================================================

    protected void waitForBrokerReplicas(AddressSpace addressSpace, Address destination, int expectedReplicas) throws
            Exception {
        TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
        TestUtils.waitForNBrokerReplicas(addressSpace, expectedReplicas, true, destination, budget, 5000);
    }

    protected void waitForRouterReplicas(AddressSpace addressSpace, int expectedReplicas) throws
            Exception {
        TimeoutBudget budget = new TimeoutBudget(3, TimeUnit.MINUTES);
        Map<String, String> labels = new HashMap<>();
        labels.put("name", "qdrouterd");
        labels.put("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        TestUtils.waitForNReplicas(expectedReplicas, labels, budget);
    }

    protected void waitForPodsToTerminate(List<String> uids) throws Exception {
        LOGGER.info("Waiting for following pods to be deleted {}", uids);
        assertWaitForValue(true, () -> (kubernetes.listPods(kubernetes.getInfraNamespace()).stream()
                .noneMatch(pod -> uids.contains(pod.getMetadata().getUid()))), new TimeoutBudget(2, TimeUnit.MINUTES));
    }

    protected void logWithSeparator(Logger logger, String... messages) {
        logger.info("--------------------------------------------------------------------------------");
        for (String message : messages) {
            logger.info(message);
        }
    }

    protected List<String> extractBodyAsString(Future<List<Message>> msgs) throws Exception {
        return msgs.get(1, TimeUnit.MINUTES).stream().map(m -> (String) ((AmqpValue) m.getBody()).getValue()).collect(Collectors.toList());
    }

    //================================================================================================
    //==================================== Asserts methods ===========================================
    //================================================================================================

    protected static void assertSimpleMQTTSendReceive(Address dest, IMqttClient client, int msgCount) throws Exception {
        List<MqttMessage> messages = IntStream.range(0, msgCount).boxed().map(i -> {
            MqttMessage m = new MqttMessage();
            m.setPayload(String.format("mqtt-simple-send-receive-%s", i).getBytes(StandardCharsets.UTF_8));
            m.setQos(1);
            return m;
        }).collect(Collectors.toList());

        List<CompletableFuture<MqttMessage>> receiveFutures = MqttUtils.subscribeAndReceiveMessages(client, dest.getSpec().getAddress(), messages.size(), 1);
        List<CompletableFuture<Void>> publishFutures = MqttUtils.publish(client, dest.getSpec().getAddress(), messages);

        int publishCount = MqttUtils.awaitAndReturnCode(publishFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages published",
                publishCount, is(messages.size()));

        int receivedCount = MqttUtils.awaitAndReturnCode(receiveFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages received",
                receivedCount, is(messages.size()));
    }

    protected <T extends Comparable<T>> void assertSorted(String message, Iterable<T> list) throws Exception {
        assertSorted(message, list, false);
    }

    protected <T> void assertSorted(String message, Iterable<T> list, Comparator<T> comparator) throws Exception {
        assertSorted(message, list, false, comparator);
    }

    protected <T extends Comparable<T>> void assertSorted(String message, Iterable<T> list, boolean reverse) {
        LOGGER.info("Assert sort reverse: " + reverse);
        if (!reverse) {
            assertTrue(Ordering.natural().isOrdered(list), message);
        } else {
            assertTrue(Ordering.natural().reverse().isOrdered(list), message);
        }
    }

    protected <T> void assertSorted(String message, Iterable<T> list, boolean reverse, Comparator<T> comparator) {
        LOGGER.info("Assert sort reverse: " + reverse);
        if (!reverse) {
            assertTrue(Ordering.from(comparator).isOrdered(list), message);
        } else {
            assertTrue(Ordering.from(comparator).reverse().isOrdered(list), message);
        }
    }

    protected <T> void assertWaitForValue(T expected, Callable<T> fn, TimeoutBudget budget) throws Exception {
        T got = null;
        LOGGER.info("waiting for expected value '{}' ...", expected);
        while (budget.timeLeft() >= 0) {
            got = fn.call();
            if (Objects.equals(expected, got)) {
                return;
            }
            Thread.sleep(100);
        }
        fail(String.format("Incorrect result value! expected: '%s', got: '%s'", expected, Objects.requireNonNull(got)));
    }

    protected static void assertDefaultEnabled(final Boolean enabled) {
        if (enabled != null && !Boolean.TRUE.equals(enabled)) {
            fail("Default value must be 'null' or 'true'");
        }
    }

    protected void assertConcurentMessaging(List<Address> dest, List<UserCredentials> users, String destNamePrefix, int customerIndex, int messageCount) throws Exception {
        ArrayList<AmqpClient> clients = new ArrayList<>(users.size());
        String sufix = AddressSpaceUtils.isBrokered(resourcesManager.getSharedAddressSpace()) ? "#" : "*";
        users.forEach((user) -> {
            try {
                resourcesManager.createOrUpdateUser(resourcesManager.getSharedAddressSpace(),
                        UserUtils.createUserResource(user)
                                .editSpec()
                                .withAuthorization(Collections.singletonList(
                                        new UserAuthorizationBuilder()
                                                .withAddresses(String.format("%s.%s.%s", destNamePrefix, customerIndex, sufix))
                                                .withOperations(Operation.send, Operation.recv).build()))
                                .endSpec()
                                .done());
                AmqpClient queueClient = resourcesManager.getAmqpClientFactory().createQueueClient();
                queueClient.getConnectOptions().setCredentials(user);
                clients.add(queueClient);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        AddressUtils.waitForDestinationsReady(dest.toArray(new Address[0]));
        //start sending messages
        int everyN = 3;
        for (AmqpClient client : clients) {
            for (int i = 0; i < dest.size(); i++) {
                if (i % everyN == 0) {
                    Future<Integer> sent = client.sendMessages(dest.get(i).getSpec().getAddress(), TestUtils.generateMessages(messageCount));
                    //wait for messages sent
                    assertEquals(messageCount, sent.get(1, TimeUnit.MINUTES).intValue(),
                            "Incorrect count of messages send");
                }
            }
        }

        //receive messages
        for (AmqpClient client : clients) {
            for (int i = 0; i < dest.size(); i++) {
                if (i % everyN == 0) {
                    Future<List<Message>> received = client.recvMessages(dest.get(i).getSpec().getAddress(), messageCount);
                    //wait for messages received
                    assertEquals(messageCount, received.get(1, TimeUnit.MINUTES).size(),
                            "Incorrect count of messages received");
                }
            }
            client.close();
        }
    }

    protected void assertSendReceiveLargeMessageQueue(JmsProvider jmsProvider, double sizeInMB, Address dest, int count) throws Exception {
        assertSendReceiveLargeMessageQueue(jmsProvider, sizeInMB, dest, count, DeliveryMode.NON_PERSISTENT);
    }

    protected void assertSendReceiveLargeMessageQueue(JmsProvider jmsProvider, double sizeInMB, Address dest, int count, int mode) throws Exception {
        int size = (int) (sizeInMB * 1024 * 1024);

        Session session = jmsProvider.getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
        javax.jms.Queue testQueue = (javax.jms.Queue) jmsProvider.getDestination(dest.getSpec().getAddress());
        List<javax.jms.Message> messages = jmsProvider.generateMessages(session, count, size);

        MessageProducer sender = session.createProducer(testQueue);
        MessageConsumer receiver = session.createConsumer(testQueue);

        assertSendReceiveLargeMessage(jmsProvider, sender, receiver, sizeInMB, mode, count, messages);

    }

    protected void assertSendReceiveLargeMessageTopic(JmsProvider jmsProvider, double sizeInMB, Address dest, int count) throws Exception {
        assertSendReceiveLargeMessageTopic(jmsProvider, sizeInMB, dest, count, DeliveryMode.NON_PERSISTENT);
    }

    protected void assertSendReceiveLargeMessageTopic(JmsProvider jmsProvider, double sizeInMB, Address dest, int count, int mode) throws Exception {
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

    protected void assertAddressApi(AddressSpace addressSpace, Address d1, Address d2) throws Exception {
        List<String> destinationsNames = Arrays.asList(d1.getSpec().getAddress(), d2.getSpec().getAddress());
        resourcesManager.setAddresses(d1);
        resourcesManager.appendAddresses(d2);

        //d1, d2
        List<String> response = AddressUtils.getAddresses(addressSpace).stream().map(address -> address.getSpec().getAddress()).collect(Collectors.toList());
        assertThat("Rest api does not return all addresses", response, is(destinationsNames));
        LOGGER.info("addresses {} successfully created", Arrays.toString(destinationsNames.toArray()));

        //get specific address d2
        Address res = kubernetes.getAddressClient(addressSpace.getMetadata().getNamespace()).withName(d2.getMetadata().getName()).get();
        assertThat("Rest api does not return specific address", res.getSpec().getAddress(), is(d2.getSpec().getAddress()));

        resourcesManager.deleteAddresses(d1);

        //d2
        response = AddressUtils.getAddresses(addressSpace).stream().map(address -> address.getSpec().getAddress()).collect(Collectors.toList());
        assertThat("Rest api does not return right addresses", response, is(destinationsNames.subList(1, 2)));
        LOGGER.info("address {} successfully deleted", d1.getSpec().getAddress());

        resourcesManager.deleteAddresses(d2);

        //empty
        List<Address> listRes = AddressUtils.getAddresses(addressSpace);
        assertThat("Rest api returns addresses", listRes, is(Collections.emptyList()));
        LOGGER.info("addresses {} successfully deleted", d2.getSpec().getAddress());

        resourcesManager.setAddresses(d1, d2);
        resourcesManager.deleteAddresses(d1, d2);

        listRes = AddressUtils.getAddresses(addressSpace);
        assertThat("Rest api returns addresses", listRes, is(Collections.emptyList()));
        LOGGER.info("addresses {} successfully deleted", Arrays.toString(destinationsNames.toArray()));
    }
}
