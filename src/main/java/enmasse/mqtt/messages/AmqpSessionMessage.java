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
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.message.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an AMQP_SESSION message
 */
public class AmqpSessionMessage {

    public static final String SUBJECT = "session";

    public static final String AMQP_CLEAN_SESSION_ANNOTATION = "x-clean-session";

    private boolean isCleanSession;
    private String clientId;

    /**
     * Constructor
     *
     * @param isCleanSession    clean session flag
     * @param clientId  client identifier
     */
    public AmqpSessionMessage(boolean isCleanSession, String clientId) {

        this.isCleanSession = isCleanSession;
        this.clientId = clientId;
    }

    /**
     * Return an AMQP_SESSION message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_SESSION message
     */
    public static AmqpSessionMessage from(Message message) {

        // TODO:
        return new AmqpSessionMessage(false, null);
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
        map.put(Symbol.valueOf(AMQP_CLEAN_SESSION_ANNOTATION), this.isCleanSession);
        MessageAnnotations messageAnnotations = new MessageAnnotations(map);
        message.setMessageAnnotations(messageAnnotations);

        message.setReplyTo(String.format(AmqpCommons.AMQP_CLIENT_ADDRESS_TEMPLATE, this.clientId));

        return message;
    }

    /**
     * Clean session flag
     * @return
     */
    public boolean isCleanSession() {
        return this.isCleanSession;
    }

    /**
     * Client identifier
     * @return
     */
    public String clientId() {
        return this.clientId;
    }
}
