/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.messages;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.proton.ProtonHelper;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an AMQP_SUBSCRIBE message
 */
public class AmqpSubscribeMessage {

    public static final String AMQP_SUBJECT = "subscribe";

    private final String clientId;
    private final Object messageId;
    private final List<AmqpTopicSubscription> topicSubscriptions;

    /**
     * Constructor
     *
     * @param clientId  client identifier
     * @param messageId message identifier
     * @param topicSubscriptions    list with topics and related quality of service levels
     */
    public AmqpSubscribeMessage(String clientId, Object messageId, List<AmqpTopicSubscription> topicSubscriptions) {

        this.clientId = clientId;
        this.messageId = messageId;
        this.topicSubscriptions = topicSubscriptions;
    }

    /**
     * Return an AMQP_SUBSCRIBE message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_SUBSCRIBE message
     */
    @SuppressWarnings("unchecked")
    public static AmqpSubscribeMessage from(Message message) {

        if (!message.getSubject().equals(AMQP_SUBJECT)) {
            throw new IllegalArgumentException(String.format("AMQP message subject is no s%", AMQP_SUBJECT));
        }

        Section section = message.getBody();
        if ((section != null) && (section instanceof AmqpValue)) {

            Map<String, String> map = (Map<String, String>) ((AmqpValue) section).getValue();

            // build the unique topic subscriptions list
            List<AmqpTopicSubscription> topicSubscriptions = new ArrayList<>();
            for (Map.Entry<String, String> entry: map.entrySet()) {
                topicSubscriptions.add(new AmqpTopicSubscription(entry.getKey(), MqttQoS.valueOf(Integer.valueOf(entry.getValue()))));
            }

            return new AmqpSubscribeMessage(AmqpHelper.getClientIdFromPublishAddress((String) message.getCorrelationId()),
                    message.getMessageId(),
                    topicSubscriptions);

        } else {
            throw new IllegalArgumentException("AMQP message wrong body type");
        }
    }

    /**
     * Return a raw AMQP message
     *
     * @return
     */
    public Message toAmqp() {

        Message message = ProtonHelper.message();

        message.setSubject(AMQP_SUBJECT);

        message.setMessageId(this.messageId);

        message.setCorrelationId(String.format(AmqpHelper.AMQP_CLIENT_PUBLISH_ADDRESS_TEMPLATE, this.clientId));

        // map with topic -> qos (in String format)
        Map<String, String> map = new HashMap<>();

        this.topicSubscriptions.stream().forEach(amqpTopicSubscription -> {

            map.put(amqpTopicSubscription.topic(), String.valueOf(amqpTopicSubscription.qos().value()));
        });

        message.setBody(new AmqpValue(map));

        return message;
    }

    /**
     * Client identifier
     * @return
     */
    public String clientId() {
        return this.clientId;
    }

    /**
     * Message identifier
     * @return
     */
    public Object messageId() {
        return messageId;
    }

    /**
     * List with topics and related quolity of service levels
     * @return
     */
    public List<AmqpTopicSubscription> topicSubscriptions() {
        return this.topicSubscriptions;
    }

    @Override
    public String toString() {

        return "AmqpSubscribeMessage{" +
                "clientId=" + this.clientId +
                ", messageId=" + this.messageId +
                ", topicSubscriptions=" + this.topicSubscriptions +
                "}";
    }
}
