/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.endpoints;

import enmasse.mqtt.messages.AmqpPublishMessage;

/**
 * Class for bringing AMQP_PUBLISH to an MQTT client and the
 * related message identifier assigned on publishing
 */
public class AmqpPublishData {

    private AmqpPublishMessage amqpPublishMessage;
    private int messageId;

    /**
     * AMQP_PUBLISH message to send
     * @return
     */
    public AmqpPublishMessage amqpPublishMessage() {
        return this.amqpPublishMessage;
    }

    /**
     * Set the AMQP_PUBLISH message to send
     * @param amqpPublishMessage
     * @return  current instance of the AmqpPublishData
     */
    public AmqpPublishData setAmqpPublishMessage(AmqpPublishMessage amqpPublishMessage) {
        this.amqpPublishMessage = amqpPublishMessage;
        return this;
    }

    /**
     * Message identifier assigned to AMQP_PUBLISH message sent
     * @return
     */
    public int messageId() {
        return this.messageId;
    }

    /**
     * Set the message identifier assigned to AMQP_PUBLISH message sent
     * @param messageId message identifier
     * @return  current instance of the AmqpPublishData
     */
    public AmqpPublishData setMessageId(int messageId) {
        this.messageId = messageId;
        return this;
    }
}
