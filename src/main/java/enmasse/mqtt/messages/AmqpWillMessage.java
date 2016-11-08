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
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.message.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an AMQP_WILL message
 */
public class AmqpWillMessage {

    public static final String SUBJECT = "will";

    public static final String AMQP_RETAIN_ANNOTATION = "x-retain";
    public static final String AMQP_DESIDERED_SND_SETTLE_MODE_ANNOTATION = "x-desidered-snd-settle-mode";

    private boolean isRetain;
    private String topic;
    private int qos;
    private String payload;

    /**
     * Constructor
     *
     * @param isRetain  will retain flag
     * @param topic will topic
     * @param qos   will MQTT QoS
     * @param payload   will message payload
     */
    public AmqpWillMessage(boolean isRetain, String topic, int qos, String payload) {

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

        // TODO:
        return new AmqpWillMessage(false, null, 0, null);
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
     * Will QoS level
     * @return
     */
    public int qos() {
        return this.qos;
    }

    /**
     * Will message payload
     * @return
     */
    public String payload() {
        return this.payload;
    }
}
