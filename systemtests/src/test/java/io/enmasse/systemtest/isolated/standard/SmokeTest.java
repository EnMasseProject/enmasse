/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.mqtt.MqttUtils;
import io.enmasse.systemtest.shared.standard.QueueTest;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.NON_PR;
import static io.enmasse.systemtest.TestTag.SMOKE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a simple smoketest of EnMasse. If this passes, the chances of something being
 * very wrong is minimized. The test should not take to long too execute
 */
@Tag(NON_PR)
@Tag(SMOKE)
@Tag(ACCEPTANCE)
class SmokeTest extends TestBase implements ITestIsolatedStandard {

    private Address queue;
    private Address topic;
    private Address mqttTopic;
    private Address anycast;
    private Address multicast;
    private AddressSpace addressSpace;
    private UserCredentials cred;

    @Test
    void smoketest() throws Exception {
        addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("smoke-space-standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED_WITH_MQTT)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "smoke-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("smoke_queue1")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();
        topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "smoketopic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("smoketopic")
                .withPlan(DestinationPlan.STANDARD_SMALL_TOPIC)
                .endSpec()
                .build();
        mqttTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "topicmqtt"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("topicmqtt")
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();
        anycast = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-anycast"))
                .endMetadata()
                .withNewSpec()
                .withType("anycast")
                .withAddress("test-anycast")
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endSpec()
                .build();
        multicast = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-multicast"))
                .endMetadata()
                .withNewSpec()
                .withType("multicast")
                .withAddress("test-multicast")
                .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);
        resourcesManager.setAddresses(queue, topic, mqttTopic, anycast, multicast);
        Thread.sleep(60_000);

        cred = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        testAnycast();
        testQueue();
        testMulticast();
        testTopic();
        testMqtt();
    }

    private void testQueue() throws Exception {
        AmqpClient client = getAmqpClientFactory().createQueueClient(addressSpace);
        client.getConnectOptions().setCredentials(cred);

        QueueTest.runQueueTest(client, queue);
    }

    private void testTopic() throws Exception {
        AmqpClient client = getAmqpClientFactory().createTopicClient(addressSpace);
        client.getConnectOptions().setCredentials(cred);
        List<String> msgs = TestUtils.generateMessages(500);

        List<Future<List<Message>>> recvResults = Arrays.asList(
                client.recvMessages(topic.getSpec().getAddress(), msgs.size()),
                client.recvMessages(topic.getSpec().getAddress(), msgs.size()),
                client.recvMessages(topic.getSpec().getAddress(), msgs.size()),
                client.recvMessages(topic.getSpec().getAddress(), msgs.size()),
                client.recvMessages(topic.getSpec().getAddress(), msgs.size()),
                client.recvMessages(topic.getSpec().getAddress(), msgs.size()));

        Thread.sleep(10_000);
        assertThat("Wrong count of messages sent",
                client.sendMessages(topic.getSpec().getAddress(), msgs).get(3, TimeUnit.MINUTES), is(msgs.size()));

        assertAll("Every subscriber should receive all messages",
                () -> assertThat("Wrong count of messages received: receiver0",
                        recvResults.get(0).get(3, TimeUnit.MINUTES).size(), is(msgs.size())),
                () -> assertThat("Wrong count of messages received: receiver1",
                        recvResults.get(1).get(3, TimeUnit.MINUTES).size(), is(msgs.size())),
                () -> assertThat("Wrong count of messages received: receiver2",
                        recvResults.get(2).get(3, TimeUnit.MINUTES).size(), is(msgs.size())),
                () -> assertThat("Wrong count of messages received: receiver3",
                        recvResults.get(3).get(3, TimeUnit.MINUTES).size(), is(msgs.size())),
                () -> assertThat("Wrong count of messages received: receiver4",
                        recvResults.get(4).get(3, TimeUnit.MINUTES).size(), is(msgs.size())),
                () -> assertThat("Wrong count of messages received: receiver5",
                        recvResults.get(5).get(3, TimeUnit.MINUTES).size(), is(msgs.size()))
        );
    }

    private void testMqtt() throws Exception {
        List<MqttMessage> messages = Stream.generate(MqttMessage::new).limit(3).collect(Collectors.toList());
        messages.forEach(m -> m.setPayload(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));

        resourcesManager.setMqttClientFactory(new MqttClientFactory(addressSpace, cred));
        IMqttClient client = getMqttClientFactory().create();
        client.connect();

        List<CompletableFuture<MqttMessage>> receiveFutures = MqttUtils.subscribeAndReceiveMessages(client, mqttTopic.getSpec().getAddress(), messages.size(), 1);
        List<CompletableFuture<Void>> publishFutures = MqttUtils.publish(client, mqttTopic.getSpec().getAddress(), messages);

        int numberSent = MqttUtils.awaitAndReturnCode(publishFutures, 1, TimeUnit.MINUTES);
        assertThat("Wrong count of messages sent", numberSent, is(messages.size()));

        int numberReceived = MqttUtils.awaitAndReturnCode(receiveFutures, 1, TimeUnit.MINUTES);
        assertThat("Wrong count of messages received", numberReceived, is(messages.size()));

        client.disconnect();
        client.close();
    }

    private void testAnycast() throws Exception {
        AmqpClient client = getAmqpClientFactory().createQueueClient(addressSpace);
        client.getConnectOptions().setCredentials(cred);

        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        Future<List<Message>> recvResult = client.recvMessages(anycast.getSpec().getAddress(), msgs.size());
        Future<Integer> sendResult = client.sendMessages(anycast.getSpec().getAddress(), msgs);

        assertThat("Wrong count of messages sent", sendResult.get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat("Wrong count of messages received", recvResult.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    private void testMulticast() throws Exception {
        AmqpClient client = getAmqpClientFactory().createBroadcastClient(addressSpace);
        client.getConnectOptions().setCredentials(cred);
        List<String> msgs = Collections.singletonList("foo");

        List<Future<List<Message>>> recvResults = Arrays.asList(
                client.recvMessages(multicast.getSpec().getAddress(), msgs.size()),
                client.recvMessages(multicast.getSpec().getAddress(), msgs.size()),
                client.recvMessages(multicast.getSpec().getAddress(), msgs.size()));
        Thread.sleep(10_000);
        assertThat("Wrong count of messages sent",
                client.sendMessages(multicast.getSpec().getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertAll("All receivers should receive all messages",
                () -> assertTrue(recvResults.get(0).get(30, TimeUnit.SECONDS).size() >= msgs.size(),
                        "Wrong count of messages received: receiver0"),
                () -> assertTrue(recvResults.get(1).get(30, TimeUnit.SECONDS).size() >= msgs.size(),
                        "Wrong count of messages received: receiver1"),
                () -> assertTrue(recvResults.get(2).get(30, TimeUnit.SECONDS).size() >= msgs.size(),
                        "Wrong count of messages received: receiver2")
        );
    }
}
