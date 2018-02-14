/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.endpoints;

import enmasse.mqtt.messages.AmqpCloseMessage;
import enmasse.mqtt.messages.AmqpListMessage;
import enmasse.mqtt.messages.AmqpSubscribeMessage;
import enmasse.mqtt.messages.AmqpUnsubscribeMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subscription Service (SS) endpoint class
 */
public class AmqpSubscriptionServiceEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpSubscriptionServiceEndpoint.class);

    public static final String SUBSCRIPTION_SERVICE_ENDPOINT = "$subctrl";
    
    private ProtonSender sender;

    /**
     * Constructor
     *
     * @param sender    ProtonSender instance related to control address
     */
    public AmqpSubscriptionServiceEndpoint(ProtonSender sender) {
        this.sender = sender;
    }

    /**
     * Send the AMQP_LIST message to the Subscription Service
     *
     * @param amqpSessionMessage    AMQP_LIST message
     * @param handler   callback called on message delivered
     */
    public void sendList(AmqpListMessage amqpSessionMessage, Handler<AsyncResult<ProtonDelivery>> handler) {

        // send AMQP_LIST message
        this.sender.send(amqpSessionMessage.toAmqp(), delivery -> {

            if (delivery.getRemoteState() == Accepted.getInstance()) {
                LOG.info("AMQP list delivery {}", delivery.getRemoteState());
                handler.handle(Future.succeededFuture(delivery));
            } else {
                handler.handle(Future.failedFuture(String.format("AMQP list delivery %s", delivery.getRemoteState())));
            }
        });
    }

    /**
     * Send the AMQP_CLOSE message to the Subscription Service
     *
     * @param amqpCloseMessage  AMQP_CLOSE message
     * @param handler   callback called on message delivered
     */
    public void sendClose(AmqpCloseMessage amqpCloseMessage, Handler<AsyncResult<ProtonDelivery>> handler) {

        // send AMQP_CLOSE message
        this.sender.send(amqpCloseMessage.toAmqp(), delivery -> {

            if (delivery.getRemoteState() == Accepted.getInstance()) {
                LOG.info("AMQP close delivery {}", delivery.getRemoteState());
                handler.handle(Future.succeededFuture(delivery));
            } else {
                handler.handle(Future.failedFuture(String.format("AMQP close delivery %s", delivery.getRemoteState())));
            }
        });
    }

    /**
     * Send the AMQP_SUBSCRIBE message to the Subscription Service
     *
     * @param amqpSubscribeMessage  AMQP_SUBSCRIBE message
     * @param handler   callback called on message delivered
     */
    public void sendSubscribe(AmqpSubscribeMessage amqpSubscribeMessage, Handler<AsyncResult<ProtonDelivery>> handler) {

        // send AMQP_SUBSCRIBE message
        this.sender.send(amqpSubscribeMessage.toAmqp(), delivery -> {

            if ((delivery.getRemoteState() == Accepted.getInstance()) ||
                (delivery.getRemoteState() instanceof Rejected)) {
                LOG.info("AMQP subscribe delivery {}", delivery.getRemoteState());
                handler.handle(Future.succeededFuture(delivery));
            } else {
                handler.handle(Future.failedFuture(String.format("AMQP subscribe delivery %s", delivery.getRemoteState())));
            }
        });
    }

    /**
     * Send the AMQP_UNSUBSCRIBE message to the Subscription Service
     *
     * @param amqpUnsubscribeMessage    AMQP_UNSUBSCRIBE message
     * @param handler   callback called on message delivered
     */
    public void sendUnsubscribe(AmqpUnsubscribeMessage amqpUnsubscribeMessage, Handler<AsyncResult<ProtonDelivery>> handler) {

        // send AMQP_UNSUBSCRIBE message
        this.sender.send(amqpUnsubscribeMessage.toAmqp(), delivery -> {

            if (delivery.getRemoteState() == Accepted.getInstance()) {
                LOG.info("AMQP unsubscribe delivery {}", delivery.getRemoteState());
                handler.handle(Future.succeededFuture(delivery));
            } else {
                handler.handle(Future.failedFuture(String.format("AMQP unsubscribe delivery %s", delivery.getRemoteState())));
            }
        });
    }

    /**
     * Open the endpoint, attaching the link
     */
    public void open() {

        // attach sender link to $subctrl
        this.sender
                .setQoS(ProtonQoS.AT_LEAST_ONCE)
                .open();
    }

    /**
     * Close the endpoint, detaching the link
     */
    public void close() {

        if (this.sender.isOpen()) {
            // detach link
            this.sender.close();
        }
    }
}
