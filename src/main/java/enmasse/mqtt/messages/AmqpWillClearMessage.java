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
import org.apache.qpid.proton.message.Message;

/**
 * Represents an AMQP_WILL_CLEAR message
 */
public class AmqpWillClearMessage {

    public static final String SUBJECT = "will-clear";

    /**
     * Return an AMQP_WILL_CLEAR message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_WILL_CLEAR message
     */
    public static AmqpWillClearMessage from(Message message) {

        // TODO:
        return new AmqpWillClearMessage();
    }

    /**
     * Return a raw AMQP message
     *
     * @return
     */
    public Message toAmqp() {

        Message message = ProtonHelper.message();

        message.setSubject(SUBJECT);

        return message;
    }
}
