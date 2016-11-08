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
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.util.List;

/**
 * Represents an AMQP_UNSUBSCRIBE message
 */
public class AmqpUnsubscribeMessage {

    public static final String SUBJECT = "unsubscribe";

    private String clientId;
    private List<String> topics;

    /**
     * Constructor
     *
     * @param clientId  client identifier
     * @param topics    topics to subscribe
     */
    public AmqpUnsubscribeMessage(String clientId, List<String> topics) {

        this.clientId = clientId;
        this.topics = topics;
    }

    /**
     * Return an AMQP_UNSUBSCRIBE message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_UNSUBSCRIBE message
     */
    public static AmqpUnsubscribeMessage from(Message message) {

        // TODO:
        return new AmqpUnsubscribeMessage(null, null);
    }

    /**
     * Return a raw AMQP message
     *
     * @return
     */
    public Message toAmqp() {

        Message message = ProtonHelper.message();

        message.setSubject(SUBJECT);

        message.setReplyTo(String.format(AmqpCommons.AMQP_CLIENT_ADDRESS_TEMPLATE, this.clientId));

        message.setBody(new AmqpValue(this.topics));

        return message;
    }

    /**
     * Client identifier
     * @return
     */
    public String clientId() {
        return this.clientId;
    }

    /**
     * Topics to subscribe
     * @return
     */
    public List<String> topics() {
        return this.topics;
    }
}
