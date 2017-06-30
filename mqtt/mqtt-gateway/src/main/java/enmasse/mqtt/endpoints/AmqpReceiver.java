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
