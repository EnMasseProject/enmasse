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

import enmasse.mqtt.messages.AmqpWillMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will Service (WS) endpoint class
 */
public class AmqpWillServiceEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpWillServiceEndpoint.class);

    private static final Symbol AMQP_DETACH_FORCED = Symbol.valueOf("amqp:link:detach-forced");

    public static final String WILL_SERVICE_ENDPOINT = "$mqtt.willservice";

    private ProtonSender sender;

    /**
     * Constructor
     *
     * @param sender    ProtonSender instance related to control address
     */
    public AmqpWillServiceEndpoint(ProtonSender sender) {
        this.sender = sender;
    }

    /**
     * Open the endpoint, attaching the link
     */
    public void open() {

        // attach sender link to $mqtt.willservice
        this.sender
                .setQoS(ProtonQoS.AT_LEAST_ONCE)
                .open();
    }

    /**
     * Send the AMQP_WILL message to the Will Service
     *
     * @param amqpWillMessage   AMQP_WILL message
     * @param handler   callback called on message delivered
     */
    public void sendWill(AmqpWillMessage amqpWillMessage, Handler<AsyncResult<ProtonDelivery>> handler) {

        // send AMQP_WILL message with will information
        this.sender.send(amqpWillMessage.toAmqp(), delivery -> {

            if (delivery.getRemoteState() == Accepted.getInstance()) {
                LOG.info("AMQP will delivery {}", delivery.getRemoteState());
                handler.handle(Future.succeededFuture(delivery));
            } else {
                handler.handle(Future.failedFuture(String.format("AMQP will delivery %s", delivery.getRemoteState())));
            }
        });
    }

    /**
     * Close the endpoint, detaching the link
     *
     * @param isDetachForced    if link should be detached with error
     */
    public void close(boolean isDetachForced) {

        if (this.sender.isOpen()) {

            if (isDetachForced) {
                ErrorCondition errorCondition =
                        new ErrorCondition(AMQP_DETACH_FORCED, "Link detached due to a brute MQTT client disconnection");
                this.sender.setCondition(errorCondition);
            }

            // detach link
            this.sender.close();
        }
    }
}
