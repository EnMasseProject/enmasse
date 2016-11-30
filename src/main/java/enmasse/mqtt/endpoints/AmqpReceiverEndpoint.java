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
import enmasse.mqtt.messages.AmqpPubrelMessage;
import enmasse.mqtt.messages.AmqpSessionPresentMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Receiver endpoint
 */
public class AmqpReceiverEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpReceiverEndpoint.class);

    public static final String CLIENT_ENDPOINT_TEMPLATE = "$mqtt.to.%s";

    private ProtonReceiver receiver;

    // handler called when AMQP_SESSION_PRESENT is received
    private Handler<AmqpSessionPresentMessage> sessionHandler;
    // handler called when AMQP_PUBLISH is received
    private Handler<AmqpPublishMessage> publishHandler;
    // handler called when AMQP_PUBREL is received
    private Handler<AmqpPubrelMessage> pubrelHandler;
    // all delivery for received messages if they need settlement (messageId -> delivery)
    private Map<Object, ProtonDelivery> deliveries;

    /**
     * Constructor
     *
     * @param receiver  ProtonReceiver instance related to unique client address
     */
    public AmqpReceiverEndpoint(ProtonReceiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Set the session handler called when AMQP_SESSION_PRESENT is received
     *
     * @param handler   the handler
     * @return  the current AmqpReceiverEndpoint instance
     */
    public AmqpReceiverEndpoint sessionHandler(Handler<AmqpSessionPresentMessage> handler) {

        this.sessionHandler = handler;
        return this;
    }

    /**
     * Set the session handler called when AMQP_PUBLISH is received
     *
     * @param handler   the handler
     * @return  the current AmqpReceiverEndpoint instance
     */
    public AmqpReceiverEndpoint publishHandler(Handler<AmqpPublishMessage> handler) {

        this.publishHandler = handler;
        return this;
    }

    /**
     * Set the session handler called when AMQP_PUBREL is received
     *
     * @param handler   the handler
     * @return  the current AmqpReceiverEndpoint instance
     */
    public AmqpReceiverEndpoint pubrelHandler(Handler<AmqpPubrelMessage> handler) {

        this.pubrelHandler = handler;
        return this;
    }

    /**
     * Handler for the receiver for handling incoming raw AMQP message
     * from the Subscription Service
     *
     * @param delivery  AMQP delivery information
     * @param message   raw AMQP message
     */
    private void messageHandler(ProtonDelivery delivery, Message message) {

        LOG.info("Received {}", message);

        if (message.getSubject() != null) {

            switch (message.getSubject()) {

                case AmqpSessionPresentMessage.AMQP_SUBJECT:

                    this.handleSession(AmqpSessionPresentMessage.from(message));
                    delivery.disposition(Accepted.getInstance(), true);

                    break;

                case AmqpPublishMessage.AMQP_SUBJECT:

                    AmqpPublishMessage amqpPublishMessage = AmqpPublishMessage.from(message);

                    // QoS 0 : immediate disposition (settle), then passing to the bridge handler
                    if (amqpPublishMessage.qos() == MqttQoS.AT_MOST_ONCE) {

                        if (!delivery.remotelySettled()) {
                            delivery.disposition(Accepted.getInstance(), true);
                        }
                        this.handlePublish(amqpPublishMessage);

                    // QoS 1 : passing to the bridge handle first, added to deliveries (be settled after bridge handling)
                    } else if (amqpPublishMessage.qos() == MqttQoS.AT_LEAST_ONCE) {

                        this.handlePublish(amqpPublishMessage);
                        if (!delivery.remotelySettled()) {
                            this.deliveries.put(message.getMessageId(), delivery);
                        }

                    // QoS 2 :
                    } else {

                        // TODO: handling QoS 2

                        this.handlePublish(amqpPublishMessage);
                        if (!delivery.remotelySettled()) {
                            this.deliveries.put(message.getMessageId(), delivery);
                        }
                    }

                    break;

                case AmqpPubrelMessage.AMQP_SUBJECT:

                    this.handlePubrel(AmqpPubrelMessage.from(message));
                    if (!delivery.remotelySettled()) {
                        this.deliveries.put(message.getMessageId(), delivery);
                    }

                    break;
            }

        } else {

            // TODO: published message (i.e. from native AMQP clients) could not have subject "publish" and all needed annotations !!!
            message.setSubject(AmqpPublishMessage.AMQP_SUBJECT);
            this.handlePublish(AmqpPublishMessage.from(message));
            if (!delivery.remotelySettled()) {
                this.deliveries.put(message.getMessageId(), delivery);
            }
        }
    }

    /**
     * Open the endpoint, attaching the links
     */
    public void open() {

        this.deliveries = new HashMap<>();

        // attach receiver link on the $mqtt.to.<client-id> address for receiving messages (from SS)
        // define handler for received messages
        // - AMQP_SESSION_PRESENT after sent AMQP_SESSION -> for writing CONNACK (session-present)
        // - AMQP_PUBLISH for every AMQP published message
        this.receiver
                .setQoS(ProtonQoS.AT_LEAST_ONCE)
                .handler(this::messageHandler)
                .open();
    }

    /**
     * Close the endpoint, detaching the link
     */
    public void close() {

        // detach link
        this.receiver.close();
        this.deliveries.clear();
    }

    /**
     * Settle the delivery for a received message
     *
     * @param messageId message identifier to settle
     */
    public void settle(Object messageId) {

        if (this.deliveries.containsKey(messageId)) {
            ProtonDelivery delivery = this.deliveries.remove(messageId);
            delivery.disposition(Accepted.getInstance(), true);
        }
    }

    /**
     * Used for calling the session handler when AMQP_SESSION_PRESENT is received
     *
     * @param amqpSessionPresentMessage AMQP_SESSION_PRESENT message
     */
    private void handleSession(AmqpSessionPresentMessage amqpSessionPresentMessage) {

        if (this.sessionHandler != null) {
            this.sessionHandler.handle(amqpSessionPresentMessage);
        }
    }

    /**
     * Used for calling the session handler when AMQP_PUBLISH is received
     *
     * @param amqpPublishMessage AMQP_PUBLISH message
     */
    private void handlePublish(AmqpPublishMessage amqpPublishMessage) {

        if (this.publishHandler != null) {
            this.publishHandler.handle(amqpPublishMessage);
        }
    }

    /**
     * Used for calling the pubrel handler when AMQP_PUBREL is received
     *
     * @param amqpPubrelMessage AMQP_PUBREL message
     */
    private void handlePubrel(AmqpPubrelMessage amqpPubrelMessage) {

        if (this.pubrelHandler != null) {
            this.pubrelHandler.handle(amqpPubrelMessage);
        }
    }
}
