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
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents an AMQP_SUBSCRIBE message
 */
public class AmqpSubscribeMessage {

    public static final String AMQP_SUBJECT = "subscribe";

    private static final String TOPICS_KEY = "topics";
    private static final String DESIRED_SETTLE_MODES_KEY = "desired-settle-modes";

    private final String clientId;
    private final Object messageId;
    private final List<String> topics;
    private final List<AmqpQos> qos;

    /**
     * Constructor
     *
     * @param clientId  client identifier
     * @param messageId message identifier
     * @param topics    topics to subscribe
     * @param qos   qos levels for topics to subscribe
     */
    public AmqpSubscribeMessage(String clientId, Object messageId, List<String> topics, List<AmqpQos> qos) {

        this.clientId = clientId;
        this.messageId = messageId;
        this.topics = topics;
        this.qos = qos;
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

            AmqpSubscribeMessage amqpSubscribeMessage =
                    new AmqpSubscribeMessage(AmqpHelper.getClientId(message.getReplyTo()),
                            message.getMessageId(),
                            topics,
                            settleModes.stream().map(settleMode -> { return AmqpQos.toAmqpQos(settleMode); }).collect(Collectors.toList()));

            return amqpSubscribeMessage;

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

        Map<String, List<?>> map = new HashMap<>();
        map.put(TOPICS_KEY, this.topics);
        map.put(DESIRED_SETTLE_MODES_KEY, this.qos.stream().map(amqpQos -> amqpQos.toList()).collect(Collectors.toList()));

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
     * Topics to subscribe
     * @return
     */
    public List<String> topics() {
        return this.topics;
    }

    /**
     * QoS levels for topics to subscribe
     * @return
     */
    public List<AmqpQos> qos() {
        return this.qos;
    }

}
