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

import enmasse.mqtt.messages.AmqpSessionPresentMessage;
import enmasse.mqtt.messages.AmqpSubackMessage;
import enmasse.mqtt.messages.AmqpUnsubackMessage;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receiver endpoint
 */
public class AmqpReceiverEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpReceiverEndpoint.class);

    public static final String CLIENT_ENDPOINT_TEMPLATE = "$mqtt.to.%s";

    private ProtonReceiver receiver;

    // handler called when AMQP_SESSION_PRESENT is received
    private Handler<AmqpSessionPresentMessage> sessionHandler;
    // handler called when AMQP_SUBACK is received
    private Handler<AmqpSubackMessage> subackHandler;
    // handler called when AMQP_UNSUBACK is received
    private Handler<AmqpUnsubackMessage> unsubackHandler;

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

    public void publishHandler(/* Handler */) {
        // TODO: set handler called when AMQP_PUBLISH message is received
    }

    /**
     * Set the session handler called when AMQP_SUBACK is received
     *
     * @param handler   the handler
     * @return  the current AmqpReceiverEndpoint instance
     */
    public AmqpReceiverEndpoint subackHandler(Handler<AmqpSubackMessage> handler) {

        this.subackHandler = handler;
        return this;
    }

    /**
     * Set the session handler called when AMQP_UNSUBACK is received
     *
     * @param handler   the handler
     * @return  the current AmqpReceiverEndpoint instance
     */
    public AmqpReceiverEndpoint unsubackHandler(Handler<AmqpUnsubackMessage> handler) {

        this.unsubackHandler = handler;
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
        // TODO:

        switch (message.getSubject()) {

            case AmqpSessionPresentMessage.AMQP_SUBJECT:
                this.handleSession(AmqpSessionPresentMessage.from(message));
                break;

            case AmqpSubackMessage.AMQP_SUBJECT:
                this.handleSuback(AmqpSubackMessage.from(message));
                break;

            case AmqpUnsubackMessage.AMQP_SUBJECT:
                this.handleUnsuback(AmqpUnsubackMessage.from(message));
                break;

            default:
                this.publishHandler();
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
    }

    /**
     * Close the endpoint, detaching the link
     */
    public void close() {

        // detach link
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

    /**
     * Used for calling the session handler when AMQP_SUBACK is received
     *
     * @param amqpSubackMessage AMQP_SUBACK message
     */
    private void handleSuback(AmqpSubackMessage amqpSubackMessage) {

        if (this.subackHandler != null) {
            this.subackHandler.handle(amqpSubackMessage);
        }
    }

    /**
     * Used for calling the session handler when AMQP_UNSUBACK is received
     *
     * @param amqpUnsubackMessage AMQP_UNSUBACK message
     */
    private void handleUnsuback(AmqpUnsubackMessage amqpUnsubackMessage) {

        if (this.unsubackHandler != null) {
            this.unsubackHandler.handle(amqpUnsubackMessage);
        }
    }
}
