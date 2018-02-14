/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.messages;

import io.vertx.proton.ProtonHelper;
import org.apache.qpid.proton.message.Message;

/**
 * Represents an AMQP_LIST message
 */
public class AmqpListMessage {

    public static final String AMQP_SUBJECT = "list";

    private final String clientId;

    /**
     * Constructor
     *
     * @param clientId  client identifier
     */
    public AmqpListMessage(String clientId) {

        this.clientId = clientId;
    }

    /**
     * Return an AMQP_LIST message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_LIST message
     */
    public static AmqpListMessage from(Message message) {

        if (!message.getSubject().equals(AMQP_SUBJECT)) {
            throw new IllegalArgumentException(String.format("AMQP message subject is no s%", AMQP_SUBJECT));
        }

        return new AmqpListMessage(AmqpHelper.getClientIdFromPublishAddress((String) message.getCorrelationId()));
    }

    /**
     * Return a raw AMQP message
     *
     * @return
     */
    public Message toAmqp() {

        Message message = ProtonHelper.message();

        message.setSubject(AMQP_SUBJECT);

        message.setCorrelationId(String.format(AmqpHelper.AMQP_CLIENT_PUBLISH_ADDRESS_TEMPLATE, this.clientId));
        message.setReplyTo(String.format(AmqpHelper.AMQP_CLIENT_CONTROL_ADDRESS_TEMPLATE, this.clientId));

        return message;
    }

    /**
     * Client identifier
     * @return
     */
    public String clientId() {
        return this.clientId;
    }

    @Override
    public String toString() {

        return "AmqpListMessage{" +
                "clientId=" + this.clientId +
                "}";
    }
}
