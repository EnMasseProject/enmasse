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

import io.vertx.core.buffer.Buffer;
import io.vertx.proton.ProtonHelper;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an AMQP_PUBLISH message
 */
public class AmqpPublishMessage {

    public static final String SUBJECT = "publish";

    public static final String AMQP_RETAIN_ANNOTATION = "x-retain";
    public static final String AMQP_DESIDERED_SND_SETTLE_MODE_ANNOTATION = "x-desidered-snd-settle-mode";

    private int qos;
    private boolean isDup;
    private boolean isRetain;
    private String topic;
    private Buffer payload;

    /**
     * Constructor
     *
     * @param qos   QoS level
     * @param isDup if the message is a duplicate
     * @param isRetain  if the message needs to be retained
     * @param topic topic on which the message is published
     * @param payload   message payload
     */
    public AmqpPublishMessage(int qos, boolean isDup, boolean isRetain, String topic, Buffer payload) {

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

        if (!message.getSubject().equals(SUBJECT)) {
            throw new IllegalArgumentException("AMQP message subject is no 'publish'");
        }

        MessageAnnotations messageAnnotations = message.getMessageAnnotations();
        if (messageAnnotations == null) {
            throw new IllegalArgumentException("AMQP message has no annotations");
        } else {

            boolean isRetain = false;
            if (messageAnnotations.getValue().containsKey(Symbol.valueOf(AMQP_RETAIN_ANNOTATION))) {
                isRetain = (boolean) messageAnnotations.getValue().get(Symbol.valueOf(AMQP_RETAIN_ANNOTATION));
            }

            int qos = 0;
            if (messageAnnotations.getValue().containsKey(Symbol.valueOf(AMQP_DESIDERED_SND_SETTLE_MODE_ANNOTATION))) {
                // TODO : qos is not unique from settle mode
            }

            boolean isDup = (message.getDeliveryCount() > 0);

            String topic = message.getAddress();

            Section section = message.getBody();
            if ((section != null) && (section instanceof Data)) {

                Buffer payload = Buffer.buffer(((Data) section).getValue().getArray());
                return new AmqpPublishMessage(qos, isDup, isRetain, topic, payload);

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

        message.setSubject(SUBJECT);

        Map<Symbol, Object> map = new HashMap<>();
        map.put(Symbol.valueOf(AMQP_RETAIN_ANNOTATION), this.isRetain);
        map.put(Symbol.valueOf(AMQP_DESIDERED_SND_SETTLE_MODE_ANNOTATION), AmqpHelper.toSenderSettleMode(this.qos));
        MessageAnnotations messageAnnotations = new MessageAnnotations(map);
        message.setMessageAnnotations(messageAnnotations);

        message.setAddress(this.topic);

        message.setDeliveryCount(this.isDup ? 1 : 0);

        // the payload could be null (or empty)
        if (this.payload != null)
            message.setBody(new Data(new Binary(this.payload.getBytes())));

        return message;
    }

    /**
     * QoS level
     * @return
     */
    public int qos() {
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
