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
