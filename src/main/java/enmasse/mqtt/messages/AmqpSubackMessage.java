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

import org.apache.qpid.proton.amqp.UnsignedByte;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents an AMQP_SUBACK message
 */
public class AmqpSubackMessage {

    public static final String AMQP_SUBJECT = "suback";

    private final Object messageId;
    private final List<AmqpQos> grantedQoSLevels;

    /**
     * Constructor
     *
     * @param messageId message identifier
     * @param grantedQoSLevels  granted QoS levels for requested topic subscriptions
     */
    private AmqpSubackMessage(Object messageId, List<AmqpQos> grantedQoSLevels) {

        this.messageId = messageId;
        this.grantedQoSLevels = grantedQoSLevels;
    }

    /**
     * Return an AMQP_SUBACK message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_SUBACK message
     */
    public static AmqpSubackMessage from(Message message) {

        if (!message.getSubject().equals(AMQP_SUBJECT)) {
            throw new IllegalArgumentException(String.format("AMQP message subject is no s%", AMQP_SUBJECT));
        }

        Section section = message.getBody();
        if ((section != null) && (section instanceof AmqpValue)) {

            List<List<UnsignedByte>> grantedQoSLevels = (List<List<UnsignedByte>>) ((AmqpValue)message.getBody()).getValue();
            return new AmqpSubackMessage(message.getMessageId(), grantedQoSLevels.stream().map(c -> AmqpQos.toAmqpQos(c)).collect(Collectors.toList()));
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

        // do you really need this ?
        throw new NotImplementedException();
    }

    /**
     * Message identifier
     * @return
     */
    public Object messageId() {
        return messageId;
    }

    /**
     * Granted QoS levels for requested topic subscriptions
     * @return
     */
    public List<AmqpQos> grantedQoSLevels() {
        return this.grantedQoSLevels;
    }
}
