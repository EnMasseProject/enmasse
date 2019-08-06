/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.shared.standard.mqtt;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedWithMqtt;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.mqtt.MqttUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests related to interoperability mqtt with amqp
 */
class InteroperabilityTest extends TestBase implements ITestSharedWithMqtt {
    private static final String MQTT_AMQP_TOPIC = "mqtt-amqp-topic";
    private static final String AMQP_MQTT_TOPIC = "amqp-mqtt-topic";

    @Test
    void testSendMqttReceiveAmqp() throws Exception {
        Address mqttTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), MQTT_AMQP_TOPIC))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress(MQTT_AMQP_TOPIC)
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();
        resourcesManager.setAddresses(mqttTopic);

        String payloadPrefix = "send mqtt, receive amqp";
        List<MqttMessage> messages = mqttMessageGenerator(20, 0, payloadPrefix);

        IMqttClient mqttClient = getMqttClientFactory().create();
        mqttClient.connect();

        AmqpClient amqpClient = getAmqpClientFactory().createTopicClient();
        Future<List<Message>> recvResultAmqp = amqpClient.recvMessages(mqttTopic.getSpec().getAddress(), messages.size());

        List<CompletableFuture<Void>> publishFutures = MqttUtils.publish(mqttClient, mqttTopic.getSpec().getAddress(), messages);

        int sentCount = MqttUtils.awaitAndReturnCode(publishFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages sent",
                sentCount, is(messages.size()));
        assertThat("Incorrect count of messages received",
                recvResultAmqp.get(1, TimeUnit.MINUTES).size(), is(messages.size()));

        for (Message m : recvResultAmqp.get()) {
            assertThat("Incorrect message body received!", m.getBody().toString(), containsString(payloadPrefix));
        }

        mqttClient.disconnect();
        mqttClient.close();
    }

    @Test
    void testSendAmqpReceiveMqtt() throws Exception {
        Address mqttTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), AMQP_MQTT_TOPIC))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress(AMQP_MQTT_TOPIC)
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();
        resourcesManager.setAddresses(mqttTopic);

        String payloadPrefix = "send amqp, receive mqtt :)";
        List<Message> messages = amqpMessageGenerator(mqttTopic.getSpec().getAddress(), 20, payloadPrefix);

        IMqttClient mqttClient = getMqttClientFactory().create();
        mqttClient.connect();

        List<CompletableFuture<MqttMessage>> receivedFutures = MqttUtils.subscribeAndReceiveMessages(mqttClient, mqttTopic.getSpec().getAddress(), messages.size(), 1);

        AmqpClient amqpClient = getAmqpClientFactory().createTopicClient();

        Future<Integer> sendResultAmqp = amqpClient.sendMessages(mqttTopic.getSpec().getAddress(), messages.toArray(new Message[messages.size()]));

        assertThat("Incorrect count of messages sent",
                sendResultAmqp.get(1, TimeUnit.MINUTES), is(messages.size()));

        int receivedCount = MqttUtils.awaitAndReturnCode(receivedFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages received",
                receivedCount, is(messages.size()));

        for (CompletableFuture<MqttMessage> future : receivedFutures) {
            MqttMessage message = future.get();
            assertThat("Incorrect message body received!", new String(message.getPayload(), StandardCharsets.UTF_8),
                    containsString(payloadPrefix));
        }

        mqttClient.disconnect();
    }

    private List<MqttMessage> mqttMessageGenerator(int count, int qos, String payloadPrefix) {
        List<MqttMessage> mqttMessages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MqttMessage message = new MqttMessage();
            message.setId(i);
            message.setPayload((String.format("%s-%s", payloadPrefix, i).getBytes()));
            message.setQos(qos);
            mqttMessages.add(message);
        }
        return mqttMessages;
    }

    private List<Message> amqpMessageGenerator(String address, int count, String payloadPrefix) {
        List<Message> mqttMessages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Message message = Message.Factory.create();
            message.setMessageId(new AmqpValue(i));
            message.setAddress(address);
            //message.setSubject("mysubject");  // subject mishandled - see issue #1528
            String body = String.format("%s-%d", payloadPrefix, i);
            // Body currently must be a binary - see issue #64
            message.setBody(new Data(new Binary(body.getBytes(StandardCharsets.UTF_8))));
            mqttMessages.add(message);
        }
        return mqttMessages;
    }
}
