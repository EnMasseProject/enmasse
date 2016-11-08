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

/**
 * Represents an AMQP_PUBLISH message
 */
public class AmqpPublishMessage {

    public static final String SUBJECT = "publish";

    public static final String AMQP_RETAIN_ANNOTATION = "x-retain";
    public static final String AMQP_DESIDERED_SND_SETTLE_MODE_ANNOTATION = "x-desidered-snd-settle-mode";

    /**
     * Return an AMQP_PUBLISH message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_PUBLISH message
     */
    public static AmqpPublishMessage from(Message message) {

        // TODO:
        return new AmqpPublishMessage();
    }

    /**
     * Return a raw AMQP message
     *
     * @return
     */
    public Message toAmqp() {

        // TODO:
        return null;
    }
}
