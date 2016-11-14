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
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

/**
 * Represents an AMQP_UNSUBSCRIBE message
 */
public class AmqpUnsubscribeMessage {

    public static final String AMQP_SUBJECT = "unsubscribe";

    private final String clientId;
    private final Object messageId;
    private final List<String> topics;

    /**
     * Constructor
     *
     * @param clientId  client identifier
     * @param messageId message identifier
     * @param topics    topics to subscribe
     */
    public AmqpUnsubscribeMessage(Object messageId, String clientId, List<String> topics) {

        this.messageId = messageId;
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

        message.setMessageId(this.messageId);

        message.setReplyTo(String.format(AmqpHelper.AMQP_CLIENT_ADDRESS_TEMPLATE, this.clientId));

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
     * Message identifier
     * @return
     */
    public Object messageId() {
        return messageId;
    }

    /**
     * Topics to subscribe
     * @return
     */
    public List<String> topics() {
        return this.topics;
    }
}
