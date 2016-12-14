/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
