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
import io.vertx.core.buffer.Buffer;
import io.vertx.proton.ProtonHelper;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Header;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an AMQP_PUBLISH message
 */
public class AmqpPublishMessage {

    public static final String AMQP_SUBJECT = "publish";

    private static final String AMQP_RETAIN_ANNOTATION = "x-retain";
    private static final String AMQP_QOS_ANNOTATION = "x-qos";

    private final Object messageId;
    private final MqttQoS qos;
    private final boolean isDup;
    private final boolean isRetain;
    private final String topic;
    private final Buffer payload;

    /**
     * Constructor
     *
     * @param messageId message identifier
     * @param qos MQTT QoS level
     * @param isDup if the message is a duplicate
     * @param isRetain  if the message needs to be retained
     * @param topic topic on which the message is published
     * @param payload   message payload
     */
    public AmqpPublishMessage(Object messageId, MqttQoS qos, boolean isDup, boolean isRetain, String topic, Buffer payload) {

        this.messageId = messageId;
        this.qos = qos;
        this.isDup = isDup;
        this.isRetain = isRetain;
        this.topic = topic;
        this.payload = payload;
    }

    /**
     * Return an AMQP_PUBLISH message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_PUBLISH message
     */
    public static AmqpPublishMessage from(Message message) {

        if (!message.getSubject().equals(AMQP_SUBJECT)) {
            throw new IllegalArgumentException(String.format("AMQP message subject is no s%", AMQP_SUBJECT));
        }

        MessageAnnotations messageAnnotations = message.getMessageAnnotations();
        if (messageAnnotations == null) {
            throw new IllegalArgumentException("AMQP message has no annotations");
        } else {

            boolean isRetain = false;
            if (messageAnnotations.getValue().containsKey(Symbol.valueOf(AMQP_RETAIN_ANNOTATION))) {
                isRetain = (boolean) messageAnnotations.getValue().get(Symbol.valueOf(AMQP_RETAIN_ANNOTATION));
            }

            MqttQoS qos;
            if (messageAnnotations.getValue().containsKey(Symbol.valueOf(AMQP_QOS_ANNOTATION))) {
                int value = (int) messageAnnotations.getValue().get(Symbol.valueOf(AMQP_QOS_ANNOTATION));
                qos = MqttQoS.valueOf(value);
            } else {

                if (message.getHeader() != null) {
                    // if qos annotation isn't present, fallback to "durable" header field
                    qos = ((message.getHeader().getDurable() == null) || !message.getHeader().getDurable())
                            ? MqttQoS.AT_MOST_ONCE : MqttQoS.AT_LEAST_ONCE;
                } else {
                    qos = MqttQoS.AT_MOST_ONCE;
                }
            }

            boolean isDup = (message.getDeliveryCount() > 0);

            String topic = message.getAddress();

            Section section = message.getBody();
            if ((section != null) && (section instanceof Data)) {

                Buffer payload = Buffer.buffer(((Data) section).getValue().getArray());
                return new AmqpPublishMessage(message.getMessageId(), qos, isDup, isRetain, topic, payload);

            } else {
                throw new IllegalArgumentException("AMQP message wrong body type");
            }
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

        Map<Symbol, Object> map = new HashMap<>();
        map.put(Symbol.valueOf(AMQP_RETAIN_ANNOTATION), this.isRetain);
        map.put(Symbol.valueOf(AMQP_QOS_ANNOTATION), this.qos.value());
        MessageAnnotations messageAnnotations = new MessageAnnotations(map);
        message.setMessageAnnotations(messageAnnotations);

        message.setAddress(this.topic);

        Header header = new Header();
        header.setDurable(this.qos != MqttQoS.AT_MOST_ONCE);
        message.setHeader(header);

        message.setDeliveryCount(this.isDup ? 1 : 0);

        // the payload could be null (or empty)
        if (this.payload != null)
            message.setBody(new Data(new Binary(this.payload.getBytes())));

        return message;
    }

    /**
     * Message identifier
     * @return
     */
    public Object messageId() {
        return messageId;
    }

    /**
     * MQTT QoS level
     * @return
     */
    public MqttQoS qos() {
        return this.qos;
    }

    /**
     * If the message is a duplicate
     * @return
     */
    public boolean isDup() {
        return this.isDup;
    }

    /**
     * If the message needs to be retained
     * @return
     */
    public boolean isRetain() {
        return this.isRetain;
    }

    /**
     * Topic on which the message is published
     * @return
     */
    public String topic() {
        return this.topic;
    }

    /**
     * Message payload
     * @return
     */
    public Buffer payload() {
        return this.payload;
    }
}
