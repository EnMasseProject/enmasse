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

import enmasse.mqtt.messages.AmqpSessionMessage;
import enmasse.mqtt.messages.AmqpSessionPresentMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subscription Service (SS) endpoint class
 */
public class AmqpSubscriptionServiceEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpSubscriptionServiceEndpoint.class);

    public static final String SUBSCRIPTION_SERVICE_ENDPOINT = "$mqtt.subscriptionservice";
    public static final String CLIENT_ENDPOINT_TEMPLATE = "$mqtt.to.%s";

    private ProtonSender sender;
    private ProtonReceiver receiver;

    // handler called when AMQP_SESSION_PRESENT is received
    private Handler<AmqpSessionPresentMessage> sessionHandler;

    /**
     * Constructor
     *
     * @param sender    ProtonSender instance related to control address
     * @param receiver  ProtonReceiver instance related to unique client address
     */
    public AmqpSubscriptionServiceEndpoint(ProtonSender sender, ProtonReceiver receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    /**
     * Send the AMQP_SESSION message to the Subscription Service
     *
     * @param amqpSessionMessage    AMQP_SESSION message
     * @param handler   callback called on message delivered
     */
    public void sendCleanSession(AmqpSessionMessage amqpSessionMessage, Handler<AsyncResult<ProtonDelivery>> handler) {

        // send AMQP_SESSION message with clean session info
        this.sender.send(amqpSessionMessage.toAmqp(), delivery -> {

            if (delivery.getRemoteState() == Accepted.getInstance()) {
                LOG.info("AMQP clean session delivery {}", delivery.getRemoteState());
                handler.handle(Future.succeededFuture(delivery));
            } else {
                handler.handle(Future.failedFuture(String.format("AMQP clean session delivery %s", delivery.getRemoteState())));
            }
        });
    }

    public void sendSubscribe(/* Subscribe info */) {
        // TODO: send AMQP_SUBSCRIBE message
    }

    public void sendUnsubscribe(/* Unsubscribe info */) {
        // TODO: send AMQP_UNSUBSCRIBE message
    }

    /**
     * Set the session handler called when AMQP_SESSION_PRESENT is received
     *
     * @param handler   the handler
     * @return  the current AmqpSubscriptionServiceEndpoint instance
     */
    public AmqpSubscriptionServiceEndpoint sessionHandler(Handler<AmqpSessionPresentMessage> handler) {

        this.sessionHandler = handler;
        return this;
    }

    public void publishHandler(/* Handler */) {
        // TODO: set handler called when AMQP_PUBLISH message is received
    }

    public void subackHandler(/* Handler */) {
        // TODO: set handler called when AMQP_SUBACK message is received
    }

    public void unsubackHandler(/* Handler */) {
        // TODO: set handler called when AMQP_UNSUBACK message is received
    }

    /**
     * Handler for the receiver for handling incoming raw AMQP message
     * from the Subscription Service
     *
     * @param delivery  AMQP delivery information
     * @param message   raw AMQP message
     */
    private void messageHandler(ProtonDelivery delivery, Message message) {
        // TODO:

        switch (message.getSubject()) {

            case AmqpSessionPresentMessage.AMQP_SUBJECT:
                this.handleSession(AmqpSessionPresentMessage.from(message));
                break;
        }
    }

    /**
     * Open the endpoint, attaching the links
     */
    public void open() {

        // attach receiver link on the $mqtt.to.<client-id> address for receiving messages (from SS)
        // define handler for received messages
        // - AMQP_SESSION_PRESENT after sent AMQP_SESSION -> for writing CONNACK (session-present)
        // - AMQP_SUBACK after sent AMQP_SUBSCRIBE
        // - AMQP_UNSUBACK after sent AMQP_UNSUBSCRIBE
        // - AMQP_PUBLISH for every AMQP published message
        this.receiver
                .handler(this::messageHandler)
                .open();

        // attach sender link to $mqtt.subscriptionservice
        this.sender.open();
    }

    /**
     * Close the endpoint, detaching the links
     */
    public void close() {

        // detach links
        this.sender.close();
        this.receiver.close();
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
}
