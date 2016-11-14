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

import org.apache.qpid.proton.message.Message;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Represents an AMQP_UNSUBACK message
 */
public class AmqpUnsubackMessage {

    public static final String AMQP_SUBJECT = "unsuback";

    private final Object messageId;

    /**
     * Constructor
     *
     * @param messageId message identifier
     */
    private AmqpUnsubackMessage(Object messageId) {

        this.messageId = messageId;
    }

    /**
     * Return an AMQP_UNSUBACK message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_UNSUBACK message
     */
    public static AmqpUnsubackMessage from(Message message) {

        if (!message.getSubject().equals(AMQP_SUBJECT)) {
            throw new IllegalArgumentException(String.format("AMQP message subject is no s%", AMQP_SUBJECT));
        }

        return new AmqpUnsubackMessage(message.getMessageId());
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
}
