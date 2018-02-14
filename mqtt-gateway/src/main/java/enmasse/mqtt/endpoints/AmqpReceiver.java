/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.endpoints;

import io.vertx.proton.ProtonReceiver;

/**
 * AMQP receiver with links couple for receiving on control and publish addresses
 */
public class AmqpReceiver {

    private final ProtonReceiver receiverControl;
    private final ProtonReceiver receiverPublish;

    /**
     * Constructor
     *
     * @param receiverControl   ProtonReceiver instance related to the control address
     * @param receiverPublish   ProtonReceiver instance related to the publish address
     */
    public AmqpReceiver(ProtonReceiver receiverControl, ProtonReceiver receiverPublish) {
        this.receiverControl = receiverControl;
        this.receiverPublish = receiverPublish;
    }

    /**
     * Close and detach the links
     */
    public void close() {

        if (this.receiverControl.isOpen()) {
            this.receiverControl.close();
        }

        if (this.receiverPublish.isOpen()) {
            this.receiverPublish.close();
        }
    }

    /**
     * If the receiver is opened
     * @return
     */
    public boolean isOpen() {

        return (this.receiverControl.isOpen() || this.receiverPublish.isOpen());
    }

    /**
     * ProtonReceiver instance related to the control address
     * @return
     */
    public ProtonReceiver receiverControl() {
        return this.receiverControl;
    }

    /**
     * ProtonReceiver instance related to the publish address
     * @return
     */
    public ProtonReceiver receiverPublish() {
        return this.receiverPublish;
    }
}
