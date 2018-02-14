/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.endpoints;

import enmasse.mqtt.messages.AmqpPublishMessage;
import enmasse.mqtt.messages.AmqpPubrelMessage;
import enmasse.mqtt.messages.AmqpSubscriptionsMessage;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonQoS;
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

    public static final String CLIENT_CONTROL_ENDPOINT_TEMPLATE = "$mqtt.to.%s.control";
    public static final String CLIENT_PUBLISH_ENDPOINT_TEMPLATE = "$mqtt.to.%s.publish";

    private AmqpReceiver receiver;

    // handler called when AMQP_SUBSCRIPTIONS is received
    private Handler<AmqpSubscriptionsMessage> subscriptionsHandler;
    // handler called when AMQP_PUBLISH is received
    private Handler<AmqpPublishData> publishHandler;
    // handler called when AMQP_PUBREL is received
    private Handler<AmqpPubrelMessage> pubrelHandler;
    // all delivery for received messages if they need settlement (messageId -> delivery)
    private Map<Object, ProtonDelivery> deliveries;

    /**
     * Constructor
     *
     * @param receiver  receiver instance related to unique client addresses
     */
    public AmqpReceiverEndpoint(AmqpReceiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Set the session handler called when AMQP_SUBSCRIPTIONS is received
     *
     * @param handler   the handler
     * @return  the current AmqpReceiverEndpoint instance
     */
    public AmqpReceiverEndpoint subscriptionsHandler(Handler<AmqpSubscriptionsMessage> handler) {

        this.subscriptionsHandler = handler;
        return this;
    }

    /**
     * Set the session handler called when AMQP_PUBLISH is received
     *
     * @param handler   the handler
     * @return  the current AmqpReceiverEndpoint instance
     */
    public AmqpReceiverEndpoint publishHandler(Handler<AmqpPublishData> handler) {

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

        // messages without subject are just AMQP_PUBLISH messages
        if (message.getSubject() == null) {

            AmqpPublishData amqpPublishData = new AmqpPublishData();
            amqpPublishData.setAmqpPublishMessage(AmqpPublishMessage.from(message));

            this.handlePublish(amqpPublishData);
            // settlement depends on the QoS levels that could be different from the current one in the
            // publish message. The AMQP bridge checks the granted QoS as well (MQTT 3.1.1)
            if (!delivery.remotelySettled()) {
                this.deliveries.put(amqpPublishData.messageId(), delivery);
            }

        } else {

            switch (message.getSubject()) {

                case AmqpSubscriptionsMessage.AMQP_SUBJECT:

                    this.handleSession(AmqpSubscriptionsMessage.from(message));
                    delivery.disposition(Accepted.getInstance(), true);

                    break;

                case AmqpPubrelMessage.AMQP_SUBJECT:

                    if (!delivery.remotelySettled()) {
                        this.deliveries.put(message.getMessageId(), delivery);
                    }
                    this.handlePubrel(AmqpPubrelMessage.from(message));

                    break;
            }

        }

    }

    /**
     * Open the control endpoint, attaching the link
     */
    public void openControl() {

        this.deliveries = new HashMap<>();

        // attach receiver link on the $mqtt.to.<client-id>.control address for receiving messages (from SS)
        // define handler for received messages
        // - AMQP_SUBSCRIPTIONS after sent AMQP_LIST -> for writing CONNACK (session-present)
        this.receiver.receiverControl()
                .setQoS(ProtonQoS.AT_LEAST_ONCE)
                .handler(this::messageHandler)
                .open();
    }

    /**
     * Open the publish endpoint, attaching the link
     */
    public void openPublish() {

        // attach receiver link on the $mqtt.to.<client-id>.publish address for receiving published messages
        // define handler for received messages
        // - AMQP_PUBLISH for every AMQP published message
        // - AMQP_PUBREL for handling QoS 2
        this.receiver.receiverPublish()
                .setQoS(ProtonQoS.AT_LEAST_ONCE)
                .handler(this::messageHandler)
                .open();
    }

    /**
     * Close the endpoint, detaching the link
     */
    public void close() {

        if (this.receiver.isOpen()) {
            // detach links
            this.receiver.close();
        }

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
            LOG.info("AMQP message [{}] settled", messageId);
        }
    }

    /**
     * Used for calling the session handler when AMQP_SUBSCRIPTIONS is received
     *
     * @param amqpSubscriptionsMessage AMQP_SUBSCRIPTIONS message
     */
    private void handleSession(AmqpSubscriptionsMessage amqpSubscriptionsMessage) {

        if (this.subscriptionsHandler != null) {
            this.subscriptionsHandler.handle(amqpSubscriptionsMessage);
        }
    }

    /**
     * Used for calling the session handler when AMQP_PUBLISH is received
     *
     * @param amqpPublishData object with the AMQP_PUBLISH message
     */
    private void handlePublish(AmqpPublishData amqpPublishData) {

        if (this.publishHandler != null) {
            this.publishHandler.handle(amqpPublishData);
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
