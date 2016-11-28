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

package enmasse.mqtt;

import enmasse.mqtt.messages.AmqpUnsubscribeMessage;
import io.vertx.core.shareddata.Shareable;

/**
 * Class for bringing AMQP_UNSUBSCRIBE through verticles in a shared data map
 */
public class AmqpUnsubscribeData implements Shareable {

    private final Object messageId;
    private final AmqpUnsubscribeMessage unsubscribe;

    /**
     * Constructor
     *
     * @param messageId AMQP_UNSUBSCRIBE message identifier
     * @param unsubscribe AMQP_UNSUBSCRIBE message
     */
    public AmqpUnsubscribeData(Object messageId, AmqpUnsubscribeMessage unsubscribe) {
        this.messageId = messageId;
        this.unsubscribe = unsubscribe;
    }

    /**
     * AMQP_UNSUBSCRIBE message identifier
     * @return
     */
    public Object messageId() {
        return this.messageId;
    }

    /**
     * AMQP_UNSUBSCRIBE  message
     * @return
     */
    public AmqpUnsubscribeMessage unsubscribe() {
        return this.unsubscribe;
    }
}
