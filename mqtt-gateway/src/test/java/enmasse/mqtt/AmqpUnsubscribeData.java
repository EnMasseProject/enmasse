/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
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
