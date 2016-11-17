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

import enmasse.mqtt.messages.AmqpWillClearMessage;
import enmasse.mqtt.messages.AmqpWillMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will Service (WS) endpoint class
 */
public class AmqpWillServiceEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpWillServiceEndpoint.class);

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
        // TODO:

        // attach sender link to $mqtt.willservice
        this.sender.open();
    }

    /**
     * Send the AMQP_WILL message to the Will Service
     *
     * @param amqpWillMessage   AMQP_WILL message
     * @param handler   callback called on message delivered
     */
    public void sendWill(AmqpWillMessage amqpWillMessage, Handler<AsyncResult<ProtonDelivery>> handler) {
        // TODO: send AMQP_WILL message with will information

        this.sender.send(amqpWillMessage.toAmqp(), delivery -> {
            // TODO:
            LOG.info("AMQP will delivered");
            handler.handle(Future.succeededFuture(delivery));
        });
    }

    /**
     * Send the AMQP_WILL_CLEAR message to the Will Service
     *
     * @param amqpWillClearMessage  AMQP_WILL_CLEAR message
     * @param handler   callback called on messahe delivered
     */
    public void clearWill(AmqpWillClearMessage amqpWillClearMessage, Handler<AsyncResult<ProtonDelivery>> handler) {
        // TODO: send AMQP_WILL_CLEAR message

        this.sender.send(amqpWillClearMessage.toAmqp(), delivery -> {
            // TODO:
            LOG.info("AMQP will clear delivered");
            handler.handle(Future.succeededFuture(delivery));
        });
    }

    /**
     * Close the endpoint, detaching the link
     */
    public void close() {
        // TODO:

        this.sender.close();
    }
}
