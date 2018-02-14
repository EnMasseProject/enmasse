/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import enmasse.mqtt.messages.AmqpWillMessage;
import io.vertx.core.shareddata.Shareable;

/**
 * Class for bringing AMQP_WILL through verticles in a shared data map
 */
public class AmqpWillData implements Shareable {

    private final String linkName;
    private final AmqpWillMessage will;

    /**
     * Constructor
     *
     * @param linkName AMQP_WILL message related client link name
     * @param will  AMQP_WILL message
     */
    public AmqpWillData(String linkName, AmqpWillMessage will) {
        this.linkName = linkName;
        this.will = will;
    }

    /**
     * AMQP_WILL message related client link name
     * @return
     */
    public String linkName() {
        return this.linkName;
    }

    /**
     * AMQP_WILL message
     * @return
     */
    public AmqpWillMessage will() {
        return this.will;
    }
}
