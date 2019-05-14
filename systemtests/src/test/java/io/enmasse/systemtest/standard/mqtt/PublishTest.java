/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard.mqtt;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.mqtt.MqttUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to publish messages via MQTT
 */
public class PublishTest extends TestBaseWithShared implements ITestBaseStandard {
    private static final String MYTOPIC = "mytopic";

    @Test
    void testPublishQoS0() throws Exception {
        List<MqttMessage> messages = Stream.generate(MqttMessage::new).limit(3).collect(Collectors.toList());
        messages.forEach(m -> {
            m.setPayload(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            m.setQos(0);
        });

        this.publish(messages, 0);
    }

    @Test
    void testPublishQoS1() throws Exception {
        List<MqttMessage> messages = Stream.generate(MqttMessage::new).limit(3).collect(Collectors.toList());
        messages.forEach(m -> {
            m.setPayload(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            m.setQos(1);
        });

        this.publish(messages, 1);
    }

    @Test
    @Disabled
    void testPublishQoS2() throws Exception {

        List<MqttMessage> messages = Stream.generate(MqttMessage::new).limit(3).collect(Collectors.toList());
        messages.forEach(m -> {
            m.setPayload(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            m.setQos(2);
        });

        this.publish(messages, 2);
    }

    @Test
    @Disabled("related issue: #1529")
    void testRetainedMessages() throws Exception {
        Address topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, "test-topic1"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic1")
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();
        setAddresses(topic);

        MqttMessage retainedMessage = new MqttMessage();
        retainedMessage.setQos(1);
        retainedMessage.setPayload("retained-message".getBytes(StandardCharsets.UTF_8));
        retainedMessage.setId(1);
        retainedMessage.setRetained(true);

        //send retained message to the topic
        IMqttClient publisher = mqttClientFactory.create();
        publisher.connect();
        publisher.publish(topic.getSpec().getAddress(), retainedMessage);
        publisher.disconnect();

        //each client which will subscribe to the topic should receive retained message!
        IMqttClient subscriber = mqttClientFactory.create();
        subscriber.connect();
        CompletableFuture<MqttMessage> messageFuture = new CompletableFuture<>();
        subscriber.subscribe(topic.getSpec().getAddress(), (topic1, message) -> messageFuture.complete(message));
        MqttMessage receivedMessage = messageFuture.get(1, TimeUnit.MINUTES);
        assertTrue(receivedMessage.isRetained(), "Retained message expected");

        subscriber.disconnect();
    }

    private void publish(List<MqttMessage> messages, int subscriberQos) throws Exception {

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, MYTOPIC))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress(MYTOPIC)
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();
        setAddresses(dest);

        IMqttClient client = mqttClientFactory.create();
        client.connect();

        List<CompletableFuture<MqttMessage>> receiveFutures = MqttUtils.subscribeAndReceiveMessages(client, dest.getSpec().getAddress(), messages.size(), subscriberQos);
        List<CompletableFuture<Void>> publishFutures = MqttUtils.publish(client, dest.getSpec().getAddress(), messages);

        int publishCount = MqttUtils.awaitAndReturnCode(publishFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages published",
                publishCount, is(messages.size()));

        int receivedCount = MqttUtils.awaitAndReturnCode(receiveFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages received",
                receivedCount, is(messages.size()));

        client.disconnect();
    }
}
