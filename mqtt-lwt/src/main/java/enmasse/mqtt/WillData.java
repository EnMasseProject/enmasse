/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import enmasse.mqtt.messages.AmqpWillMessage;

/**
 * Provides "will" information
 */
public class WillData {

    private AmqpWillMessage amqpWillMessage;
    private String clientId;

    /**
     * Constructor
     *
     * @param clientId  client identifier related to the AMQP_WILL message
     * @param amqpWillMessage   AMQP_WILL message
     */
    public WillData(String clientId, AmqpWillMessage amqpWillMessage) {
        this.clientId = clientId;
        this.amqpWillMessage = amqpWillMessage;
    }

    /**
     * AMQP_WILL message
     *
     * @return
     */
    public AmqpWillMessage amqpWillMessage() {
        return this.amqpWillMessage;
    }

    /**
     * Client identifier related to the AMQP_WILL message
     *
     * @return
     */
    public String clientId() {
        return this.clientId;
    }
}
