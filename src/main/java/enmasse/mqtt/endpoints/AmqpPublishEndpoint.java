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

import enmasse.mqtt.messages.AmqpPublishMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publisher endpoint
 */
public class AmqpPublishEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpPublishEndpoint.class);

    private ProtonSender sender;

    /**
     * Constructor
     *
     * @param sender    ProtonSender instance related to the publishing address
     */
    public AmqpPublishEndpoint(ProtonSender sender) {
        this.sender = sender;
    }

    public void open() {
        // TODO:
    }

    /**
     * Send the AMQP_PUBLISH to the attached topic/address
     *
     * @param amqpPublishMessage    AMQP_PUBLISH message
     */
    public void publish(AmqpPublishMessage amqpPublishMessage, Handler<AsyncResult<ProtonDelivery>> handler) {

        // attach sender link on "topic" (if doesn't exist yet)
        // send AMQP_PUBLISH message

        if (!this.sender.isOpen()) {

            // TODO: it should be always AT_LEAST_ONCE
            //this.sender.setQoS(amqpPublishMessage.amqpQos().toProtonQos());
            this.sender.open();

            // TODO: think about starting a timer for inactivity on this link for detaching ?
        }

        // TODO:
        // check if requested QoS is equal to the current (link already opened)
        // could be need to detach and reattach with new QoS ?

        if (this.sender.getQoS() == ProtonQoS.AT_MOST_ONCE) {

            this.sender.send(amqpPublishMessage.toAmqp());
            handler.handle(Future.succeededFuture(null));

        } else {

            this.sender.send(amqpPublishMessage.toAmqp(), delivery -> {

                if (delivery.getRemoteState() == Accepted.getInstance()) {
                    LOG.info("AMQP publish delivery {}", delivery.getRemoteState());
                    handler.handle(Future.succeededFuture(delivery));
                } else {
                    handler.handle(Future.failedFuture(String.format("AMQP publish delivery %s", delivery.getRemoteState())));
                }
            });

        }

    }

    public void close() {

        this.sender.close();
    }
}
