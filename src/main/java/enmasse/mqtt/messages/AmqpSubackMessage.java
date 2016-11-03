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

import java.util.List;

/**
 * Represents an AMQP_SUBACK message
 */
public class AmqpSubackMessage {

    public static final String SUBJECT = "suback";

    private List<Integer> grantedQoSLevels;

    /**
     * Granted QoS levels for requested topic subscriptions
     *
     * @return
     */
    public List<Integer> grantedQoSLevels() {
        return this.grantedQoSLevels;
    }

    /**
     * Constructor
     *
     * @param grantedQoSLevels  granted QoS levels for requested topic subscriptions
     */
    private AmqpSubackMessage(List<Integer> grantedQoSLevels) {
        this.grantedQoSLevels = grantedQoSLevels;
    }

    /**
     * Return an AMQP_SUBACK message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_SUBACK message
     */
    public static AmqpSubackMessage from(Message message) {

        // TODO:
        return new AmqpSubackMessage(null);
    }
}
