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
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an AMQP_WILL message
 */
public class AmqpWillMessage {

    public static final String AMQP_SUBJECT = "will";

    private static final String AMQP_RETAIN_ANNOTATION = "x-retain";
    private static final String AMQP_DESIRED_SND_SETTLE_MODE_ANNOTATION = "x-desired-snd-settle-mode";
    private static final String AMQP_DESIRED_RCV_SETTLE_MODE_ANNOTATION = "x-desired-rcv-settle-mode";

    private final boolean isRetain;
    private final String topic;
    private final AmqpQos amqpQos;
    private final String payload;

    /**
     * Constructor
     *
     * @param isRetain  will retain flag
     * @param topic will topic
     * @param amqpQos AMQP QoS level made of sender and receiver settle modes
     * @param payload   will message payload
     */
    public AmqpWillMessage(boolean isRetain, String topic, AmqpQos amqpQos, String payload) {

        this.isRetain = isRetain;
        this.topic = topic;
        this.amqpQos = amqpQos;
        this.payload = payload;
    }

    /**
     * Return an AMQP_WILL message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_WILL message
     */
    public static AmqpWillMessage from(Message message) {

        // do you really need this ?
        throw new NotImplementedException();
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
        map.put(Symbol.valueOf(AMQP_DESIRED_SND_SETTLE_MODE_ANNOTATION), this.amqpQos.sndSettleMode().getValue());
        map.put(Symbol.valueOf(AMQP_DESIRED_RCV_SETTLE_MODE_ANNOTATION), this.amqpQos.rcvSettleMode().getValue());
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
     * AMQP QoS level (made of sender and receiver settle modes)
     * @return
     */
    public AmqpQos amqpQos() {
        return this.amqpQos;
    }

    /**
     * Will message payload
     * @return
     */
    public String payload() {
        return this.payload;
    }
}
