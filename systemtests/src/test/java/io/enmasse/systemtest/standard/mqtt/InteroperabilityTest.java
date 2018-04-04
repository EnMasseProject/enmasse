/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard.mqtt;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.StandardTestBase;
import io.enmasse.systemtest.mqtt.MqttClient;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests related to interoperability mqtt with amqp
 */
public class InteroperabilityTest extends StandardTestBase {
    private static Logger log = CustomLogger.getLogger();

    @Override
    protected boolean skipDummyAddress() {
        return true;
    }

    @Test
    public void testSendMqttReceiveAmqp() throws Exception {
        Destination mqttTopic = Destination.topic("mqtt-amqp-topic", "sharded-topic");
        setAddresses(mqttTopic);

        String payloadPrefix = "send mqtt, receive amqp";
        MqttMessage[] messages = mqttMessageGenerator(20, 0, payloadPrefix);

        MqttClient mqttClient = mqttClientFactory.createClient();
        AmqpClient amqpClient = amqpClientFactory.createTopicClient();
        Future<List<Message>> recvResultAmqp = amqpClient.recvMessages(mqttTopic.getAddress(), messages.length);
        Future<Integer> sendResultMqtt = mqttClient.sendMessages(mqttTopic.getAddress(), messages);

        assertThat("Incorrect count of messages sent",
                sendResultMqtt.get(1, TimeUnit.MINUTES), is(messages.length));
        assertThat("Incorrect count of messages received",
                recvResultAmqp.get(1, TimeUnit.MINUTES).size(), is(messages.length));

        for (Message m : recvResultAmqp.get()) {
            assertThat("Incorrect message body received!", m.getBody().toString(), containsString(payloadPrefix));
        }
    }

    //@Test
    public void testSendAmqpReceiveMqtt() throws Exception {
        Destination mqttTopic = Destination.topic("amqp-mqtt-topic", "sharded-topic");
        setAddresses(mqttTopic);

        String payloadPrefix = "send amqp, receive mqtt :)";
        Message[] messages = amqpMessageGenerator(mqttTopic.getAddress(), 20, payloadPrefix);

        MqttClient mqttClient = mqttClientFactory.createClient();
        AmqpClient amqpClient = amqpClientFactory.createTopicClient();

        Future<List<MqttMessage>> recvResultMqtt = mqttClient.recvMessages(mqttTopic.getAddress(), messages.length, 0);
        Future<Integer> sendResultAmqp = amqpClient.sendMessages(mqttTopic.getAddress(), messages);

        assertThat("Incorrect count of messages sent",
                sendResultAmqp.get(1, TimeUnit.MINUTES), is(messages.length));
        assertThat("Incorrect count of messages received",
                recvResultMqtt.get(1, TimeUnit.MINUTES).size(), is(messages.length));

        for (MqttMessage m : recvResultMqtt.get()) {
            assertThat("Incorrect message body received!", new String(m.getPayload(), "UTF-8"),
                    containsString(payloadPrefix));
        }
    }

    private MqttMessage[] mqttMessageGenerator(int count, int qos, String payloadPrefix) {
        MqttMessage[] mqttMessages = new MqttMessage[count];
        for (int i = 0; i < count; i++) {
            MqttMessage message = new MqttMessage();
            message.setId(i);
            message.setPayload((String.format("%s-%s", payloadPrefix, i).getBytes()));
            message.setQos(qos);
            mqttMessages[i] = message;
        }
        return mqttMessages;
    }

    private Message[] amqpMessageGenerator(String address, int count, String payloadPrefix) {
        Message[] mqttMessages = new Message[count];
        for (int i = 0; i < count; i++) {
            Message message = Message.Factory.create();
            message.setMessageId(new AmqpValue(i));
            message.setAddress(address);
            message.setSubject("mysubject");
            message.setBody(new AmqpValue(String.format("%s-%d", payloadPrefix, i)));
            mqttMessages[i] = message;
        }
        return mqttMessages;
    }
}
