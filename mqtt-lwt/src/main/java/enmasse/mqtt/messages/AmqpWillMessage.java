/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
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
 * Represents an AMQP_WILL message
 */
public class AmqpWillMessage {

    public static final String AMQP_SUBJECT = "will";

    private static final String AMQP_RETAIN_ANNOTATION = "x-opt-retain-message";
    private static final String AMQP_QOS_ANNOTATION = "x-opt-mqtt-qos";

    private final boolean isRetain;
    private final String topic;
    private final MqttQoS qos;
    private final Buffer payload;

    /**
     * Constructor
     *
     * @param isRetain  will retain flag
     * @param topic will topic
     * @param qos MQTT QoS level
     * @param payload   will message payload
     */
    public AmqpWillMessage(boolean isRetain, String topic, MqttQoS qos, Buffer payload) {

        this.isRetain = isRetain;
        this.topic = topic;
        this.qos = qos;
        this.payload = payload;
    }

    /**
     * Return an AMQP_WILL message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_WILL message
     */
    public static AmqpWillMessage from(Message message) {

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

            String topic = message.getAddress();

            Section section = message.getBody();
            if ((section != null) && (section instanceof Data)) {

                Buffer payload = Buffer.buffer(((Data) section).getValue().getArray());
                return new AmqpWillMessage(isRetain, topic, qos, payload);

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

        Map<Symbol, Object> map = new HashMap<>();
        map.put(Symbol.valueOf(AMQP_RETAIN_ANNOTATION), this.isRetain);
        map.put(Symbol.valueOf(AMQP_QOS_ANNOTATION), this.qos.value());
        MessageAnnotations messageAnnotations = new MessageAnnotations(map);
        message.setMessageAnnotations(messageAnnotations);

        message.setAddress(this.topic);

        Header header = new Header();
        header.setDurable(this.qos != MqttQoS.AT_MOST_ONCE);
        message.setHeader(header);

        // the payload could be null (or empty)
        if (this.payload != null)
            message.setBody(new Data(new Binary(this.payload.getBytes())));

        return message;
    }

    /**
     * Will retain flag
     * @return
     */
    public boolean isRetain() {
        return this.isRetain;
    }

    /**
     * Will topic
     * @return
     */
    public String topic() {
        return this.topic;
    }

    /**
     * MQTT QoS level
     * @return
     */
    public MqttQoS qos() {
        return this.qos;
    }

    /**
     * Will message payload
     * @return
     */
    public Buffer payload() {
        return this.payload;
    }

    @Override
    public String toString() {

        return "AmqpWillMessage{" +
                "isRetain=" + this.isRetain +
                ", topic=" + this.topic +
                ", qos=" + this.qos +
                ", payload=" + this.payload +
                "}";
    }
}
