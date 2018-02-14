/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.messages;

import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * Represent a subscription to a topic in the AMQP way
 */
public class AmqpTopicSubscription {

    private final String topic;
    private final MqttQoS qos;

    /**
     * Constructor
     *
     * @param topic topic name for the subscription
     * @param qos   quality of service level
     */
    public AmqpTopicSubscription(String topic, MqttQoS qos) {
        this.topic = topic;
        this.qos = qos;
    }

    /**
     * Subscription topic name
     *
     * @return
     */
    public String topic() {
        return topic;
    }

    /**
     * Quality of Service level for the subscription
     *
     * @return
     */
    public MqttQoS qos() {
        return qos;
    }

    @Override
    public String toString() {

        return "AmqpTopicSubscription{" +
                "topic=" + this.topic +
                ", qos=" + this.qos +
                "}";
    }
}
