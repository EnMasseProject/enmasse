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
import io.netty.handler.codec.mqtt.MqttQoS;
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

    private ProtonSender senderQoS01;
    private ProtonSender senderQoS2;

    /**
     * Constructor
     *
     * @param senderQoS01    ProtonSender instance related to the publishing address for QoS 0 and 1
     * @param senderQoS2    ProtonSender instance related to the publishing address for QoS 2
     */
    public AmqpPublishEndpoint(ProtonSender senderQoS01, ProtonSender senderQoS2) {
        this.senderQoS01 = senderQoS01;
        this.senderQoS2 = senderQoS2;
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

        // send AMQP_PUBLISH message

        // use sender for QoS 0/1 messages
        if (amqpPublishMessage.qos() != MqttQoS.EXACTLY_ONCE) {

            // attach sender link on "topic" (if doesn't exist yet)
            if (!this.senderQoS01.isOpen()) {

                this.senderQoS01
                        .setQoS(ProtonQoS.AT_LEAST_ONCE)
                        .open();

                // TODO: think about starting a timer for inactivity on this link for detaching ?
            }


            if (amqpPublishMessage.qos() == MqttQoS.AT_MOST_ONCE) {

                this.senderQoS01.send(amqpPublishMessage.toAmqp());
                handler.handle(Future.succeededFuture(null));

            } else {

                this.senderQoS01.send(amqpPublishMessage.toAmqp(), delivery -> {

                    if (delivery.getRemoteState() == Accepted.getInstance()) {
                        LOG.info("AMQP publish delivery {}", delivery.getRemoteState());
                        handler.handle(Future.succeededFuture(delivery));
                    } else {
                        handler.handle(Future.failedFuture(String.format("AMQP publish delivery %s", delivery.getRemoteState())));
                    }
                });

            }

        // use sender for QoS 2 messages
        } else {

            // TODO: handling publish QoS 2 messages
        }

    }

    /**
     * Close the endpoint, detaching the links
     */
    public void close() {

        this.senderQoS01.close();
        this.senderQoS2.close();
    }
}
