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
import org.apache.qpid.proton.amqp.UnsignedByte;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.message.Message;

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
    private final Buffer payload;

    /**
     * Constructor
     *
     * @param isRetain  will retain flag
     * @param topic will topic
     * @param amqpQos AMQP QoS level made of sender and receiver settle modes
     * @param payload   will message payload
     */
    public AmqpWillMessage(boolean isRetain, String topic, AmqpQos amqpQos, Buffer payload) {

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

            SenderSettleMode sndSettleMode = null;
            if (messageAnnotations.getValue().containsKey(Symbol.valueOf(AMQP_DESIRED_SND_SETTLE_MODE_ANNOTATION))) {
                // TODO: check on this https://issues.apache.org/jira/browse/PROTON-1352
                UnsignedByte value = (UnsignedByte) messageAnnotations.getValue().get(Symbol.valueOf(AMQP_DESIRED_SND_SETTLE_MODE_ANNOTATION));
                sndSettleMode = (value != null) ? SenderSettleMode.values()[value.intValue()] : null;
            }

            ReceiverSettleMode rcvSettleMode = null;
            if (messageAnnotations.getValue().containsKey(Symbol.valueOf(AMQP_DESIRED_RCV_SETTLE_MODE_ANNOTATION))) {
                // TODO: check on this https://issues.apache.org/jira/browse/PROTON-1352
                UnsignedByte value = (UnsignedByte) messageAnnotations.getValue().get(Symbol.valueOf(AMQP_DESIRED_RCV_SETTLE_MODE_ANNOTATION));
                rcvSettleMode = (value != null) ? ReceiverSettleMode.values()[value.intValue()] : null;
            }

            String topic = message.getAddress();

            Section section = message.getBody();
            if ((section != null) && (section instanceof Data)) {

                Buffer payload = Buffer.buffer(((Data) section).getValue().getArray());
                return new AmqpWillMessage(isRetain, topic, new AmqpQos(sndSettleMode, rcvSettleMode), payload);

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
        map.put(Symbol.valueOf(AMQP_DESIRED_SND_SETTLE_MODE_ANNOTATION),
                this.amqpQos.sndSettleMode() != null ? this.amqpQos.sndSettleMode().getValue() : null);
        map.put(Symbol.valueOf(AMQP_DESIRED_RCV_SETTLE_MODE_ANNOTATION),
                this.amqpQos.rcvSettleMode() != null ? this.amqpQos.rcvSettleMode().getValue() : null);
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
    public Buffer payload() {
        return this.payload;
    }
}
