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

import io.vertx.proton.ProtonHelper;
import org.apache.qpid.proton.amqp.UnsignedByte;
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

    private static final String TOPICS_KEY = "topics";
    private static final String DESIRED_SETTLE_MODES_KEY = "desired-settle-modes";

    private final String clientId;
    private final Object messageId;
    private final List<AmqpTopicSubscription> topicSubscriptions;

    /**
     * Constructor
     *
     * @param clientId  client identifier
     * @param messageId message identifier
     * @param topicSubscriptions    list with topics and related quolity of service levels
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
    public static AmqpSubscribeMessage from(Message message) {

        if (!message.getSubject().equals(AMQP_SUBJECT)) {
            throw new IllegalArgumentException(String.format("AMQP message subject is no s%", AMQP_SUBJECT));
        }

        Section section = message.getBody();
        if ((section != null) && (section instanceof AmqpValue)) {

            Map<String, List<?>> map = (Map<String, List<?>>) ((AmqpValue) section).getValue();

            List<String> topics = (List<String>) map.get(TOPICS_KEY);
            List<List<UnsignedByte>> settleModes = (List<List<UnsignedByte>>) map.get(DESIRED_SETTLE_MODES_KEY);

            if (topics.size() != settleModes.size()) {
                throw new IllegalArgumentException("Topics and QoS lists differ in size");
            }

            // build the unique topic subscriptions list
            List<AmqpTopicSubscription> topicSubscriptions = new ArrayList<>();
            for (int i = 0; i < topics.size(); i++) {
                topicSubscriptions.add(new AmqpTopicSubscription(topics.get(i), AmqpQos.from(settleModes.get(0))));
            }

            return new AmqpSubscribeMessage(AmqpHelper.getClientId(message.getReplyTo()),
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

        message.setReplyTo(String.format(AmqpHelper.AMQP_CLIENT_ADDRESS_TEMPLATE, this.clientId));

        // extract two separate lists for topics and qos for encoding inside a Map into the raw AMQP message
        List<String> topics = new ArrayList<>();
        List<List<UnsignedByte>> qos = new ArrayList<>();

        this.topicSubscriptions.stream().forEach(amqpTopicSubscription -> {
            topics.add(amqpTopicSubscription.topic());
            qos.add(amqpTopicSubscription.qos().toList());
        });

        Map<String, List<?>> map = new HashMap<>();
        map.put(TOPICS_KEY, topics);
        map.put(DESIRED_SETTLE_MODES_KEY, qos);

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

}
