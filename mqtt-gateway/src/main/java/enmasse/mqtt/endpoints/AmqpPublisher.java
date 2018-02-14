/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.endpoints;

import io.vertx.proton.ProtonSender;

/**
 * AMQP publisher with links couple for publishing with QoS 0/1 and QoS 2
 */
public class AmqpPublisher {

    private final ProtonSender senderQoS01;
    private final ProtonSender senderQoS2;

    /**
     * Constructor
     *
     * @param senderQoS01   ProtonSender instance related to the publishing address for QoS 0 and 1
     * @param senderQoS2    ProtonSender instance related to the publishing address for QoS 2
     */
    public AmqpPublisher(ProtonSender senderQoS01, ProtonSender senderQoS2) {
        this.senderQoS01 = senderQoS01;
        this.senderQoS2 = senderQoS2;
    }

    /**
     * Close and detach the links
     */
    public void close() {

        if (this.senderQoS01.isOpen()) {
            this.senderQoS01.close();
        }

        if (this.senderQoS2.isOpen()) {
            this.senderQoS2.close();
        }
    }

    /**
     * If the publisher is opened
     * @return
     */
    public boolean isOpen() {

        return (this.senderQoS01.isOpen() || this.senderQoS2.isOpen());
    }

    /**
     * ProtonSender instance related to the publishing address for QoS 0 and 1
     * @return
     */
    public ProtonSender senderQoS01() {
        return this.senderQoS01;
    }

    /**
     * ProtonSender instance related to the publishing address for QoS 2
     * @return
     */
    public ProtonSender senderQoS2() {
        return this.senderQoS2;
    }
}
