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
 * Represents an AMQP_SUBSCRIPTIONS message
 */
public class AmqpSubscriptionsMessage {

    public static final String AMQP_SUBJECT = "subscriptions";

    private final List<AmqpTopicSubscription> topicSubscriptions;

    /**
     * Constructor
     *
     * @param topicSubscriptions    list with topics and related quality of service levels
     */
    public AmqpSubscriptionsMessage(List<AmqpTopicSubscription> topicSubscriptions) {

        this.topicSubscriptions = topicSubscriptions;
    }

    /**
     * Return an AMQP_SUBSCRIPTIONS message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_SUBSCRIPTIONS message
     */
    @SuppressWarnings("unchecked")
    public static AmqpSubscriptionsMessage from(Message message) {

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

            return new AmqpSubscriptionsMessage(topicSubscriptions);

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

        // map with topic -> qos (in String format)
        Map<String, String> map = new HashMap<>();

        this.topicSubscriptions.stream().forEach(amqpTopicSubscription -> {

            map.put(amqpTopicSubscription.topic(), String.valueOf(amqpTopicSubscription.qos().value()));
        });

        message.setBody(new AmqpValue(map));

        return message;
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

        return "AmqpSubscriptionsMessage{" +
                "topicSubscriptions=" + this.topicSubscriptions +
                "}";
    }
}
