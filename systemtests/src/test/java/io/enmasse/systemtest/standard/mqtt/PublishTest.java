/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard.mqtt;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.ExposeType;
import io.enmasse.address.model.TlsTermination;
import io.enmasse.systemtest.AddressSpacePlans;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.ability.ITestBaseWithMqtt;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.mqtt.MqttUtils;
import io.enmasse.systemtest.standard.AnycastTest;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

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
public class PublishTest extends TestBaseWithShared implements ITestBaseWithMqtt {
    private static final String MYTOPIC = "mytopic";
    private static final Logger log = CustomLogger.getLogger();

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

    @Test
    void testCustomMessagingRoutes() throws Exception {
        String endpointPrefix = "test-endpoint-";

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED_WITH_MQTT)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()

                .addNewEndpoint()
                .withName(endpointPrefix + "messaging")
                .withService("messaging")
                .editOrNewExpose()
                .withType(ExposeType.route)
                .withRouteTlsTermination(TlsTermination.passthrough)
                .withRouteServicePort("amqps")
                .endExpose()
                .endEndpoint()

                .addNewEndpoint()
                .withName(endpointPrefix + "mqtt")
                .withService("mqtt")
                .editOrNewExpose()
                .withType(ExposeType.route)
                .withRouteTlsTermination(TlsTermination.passthrough)
                .withRouteServicePort("secure-mqtt")
                .endExpose()
                .endEndpoint()
                .endSpec()
                .build();
        createAddressSpace(addressSpace);

        UserCredentials luckyUser = new UserCredentials("lucky", "luckyPswd");
        createOrUpdateUser(addressSpace, luckyUser);

        //try to get all external endpoints
        kubernetes.getExternalEndpoint(endpointPrefix + "messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        kubernetes.getExternalEndpoint(endpointPrefix + "mqtt-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));

        //messsaging
        Address anycast = new AddressBuilder()
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
        setAddresses(anycast);
        AmqpClient client1 = amqpClientFactory.createQueueClient(addressSpace);
        client1.getConnectOptions().setCredentials(luckyUser);
        AmqpClient client2 = amqpClientFactory.createQueueClient(addressSpace);
        client2.getConnectOptions().setCredentials(luckyUser);
        AnycastTest.runAnycastTest(anycast, client1, client2);

        //mqtt
        Address topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic")
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();
        appendAddresses(topic);
        MqttClientFactory mqttFactory = new MqttClientFactory(addressSpace, luckyUser);
        IMqttClient mqttClient = mqttFactory.create();
        try {
            mqttClient.connect();
            simpleMQTTSendReceive(topic, mqttClient, 3);
            mqttClient.disconnect();
        } finally {
            mqttFactory.close();
        }
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

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(options.getConnectionTimeout() * 2);
        options.setAutomaticReconnect(true);
        IMqttClient client = mqttClientFactory.build().mqttConnectionOptions(options).create();
        try {
            log.info("Connecting");
            client.connect();


            List<CompletableFuture<MqttMessage>> receiveFutures = MqttUtils.subscribeAndReceiveMessages(client, dest.getSpec().getAddress(), messages.size(), subscriberQos);
            List<CompletableFuture<Void>> publishFutures = MqttUtils.publish(client, dest.getSpec().getAddress(), messages);

            int publishCount = MqttUtils.awaitAndReturnCode(publishFutures, 1, TimeUnit.MINUTES);
            assertThat("Incorrect count of messages published",
                    publishCount, is(messages.size()));

            int receivedCount = MqttUtils.awaitAndReturnCode(receiveFutures, 1, TimeUnit.MINUTES);
            assertThat("Incorrect count of messages received",
                    receivedCount, is(messages.size()));
        } finally {
            log.info("Disconnecting");
            if (client != null) {
                client.disconnect();
            }
        }

    }
}
