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

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.message.Message;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Represents an AMQP_SESSION_PRESENT message
 */
public class AmqpSessionPresentMessage {

    private static final String AMQP_SUBJECT = "session-present";

    private static final String AMQP_CLEAN_SESSION_ANNOTATION = "x-session-present";

    private final boolean isSessionPresent;

    /**
     * Constructor
     *
     * @param isSessionPresent  if session is already present
     */
    public AmqpSessionPresentMessage(boolean isSessionPresent) {

        this.isSessionPresent = isSessionPresent;
    }

    /**
     * Return an AMQP_SESSION_PRESENT message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_SESSION_PRESENT message
     */
    public static AmqpSessionPresentMessage from(Message message) {

        if (!message.getSubject().equals(AMQP_SUBJECT)) {
            throw new IllegalArgumentException(String.format("AMQP message subject is no s%", AMQP_SUBJECT));
        }

        MessageAnnotations messageAnnotations = message.getMessageAnnotations();
        if (messageAnnotations == null) {
            throw new IllegalArgumentException("AMQP message has no annotations");
        } else {

            if (!messageAnnotations.getValue().containsKey(Symbol.valueOf(AMQP_CLEAN_SESSION_ANNOTATION))) {
                throw new IllegalArgumentException("AMQP message has no annotations");
            } else {
                AmqpSessionPresentMessage msg = new AmqpSessionPresentMessage((boolean)messageAnnotations.getValue().get(Symbol.valueOf(AMQP_CLEAN_SESSION_ANNOTATION)));
                return msg;
            }

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
     * If session is already present
     * @return
     */
    public boolean isSessionPresent() {
        return this.isSessionPresent;
    }
}
