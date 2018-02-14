/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.messages;

import io.vertx.proton.ProtonHelper;
import org.apache.qpid.proton.message.Message;

/**
 * Represents an AMQP_PUBREL message
 */
public class AmqpPubrelMessage {

    public static final String AMQP_SUBJECT = "pubrel";

    private final Object messageId;

    /**
     * Constructor
     *
     * @param messageId message identifier
     */
    public AmqpPubrelMessage(Object messageId) {

        this.messageId = messageId;
    }

    /**
     * Return an AMQP_PUBREL message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_PUBREL message
     */
    public static AmqpPubrelMessage from(Message message) {

        if (!message.getSubject().equals(AMQP_SUBJECT)) {
            throw new IllegalArgumentException(String.format("AMQP message subject is no s%", AMQP_SUBJECT));
        }

        return new AmqpPubrelMessage(message.getMessageId());
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

        return message;
    }

    /**
     * Message identifier
     * @return
     */
    public Object messageId() {
        return messageId;
    }

    @Override
    public String toString() {

        return "AmqpPubrelMessage{" +
                "messageId=" + this.messageId +
                "}";
    }
}
