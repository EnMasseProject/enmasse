/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import enmasse.mqtt.messages.AmqpSubscribeMessage;
import io.vertx.core.shareddata.Shareable;

/**
 * Class for bringing AMQP_SUBSCRIBE through verticles in a shared data map
 */
public class AmqpSubscribeData implements Shareable {

    private final Object messageId;
    private final AmqpSubscribeMessage subscribe;

    /**
     * Constructor
     *
     * @param messageId AMQP_SUBSCRIBE message identifier
     * @param subscribe AMQP_SUBSCRIBE message
     */
    public AmqpSubscribeData(Object messageId, AmqpSubscribeMessage subscribe) {
        this.messageId = messageId;
        this.subscribe = subscribe;
    }

    /**
     * AMQP_SUBSCRIBE message identifier
     * @return
     */
    public Object messageId() {
        return this.messageId;
    }

    /**
     * AMQP_SUBSCRIBE message
     * @return
     */
    public AmqpSubscribeMessage subscribe() {
        return this.subscribe;
    }
}
