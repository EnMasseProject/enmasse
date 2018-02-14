/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.endpoints;

import enmasse.mqtt.messages.AmqpWillMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.LinkError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Last Will and Testament Service (WS) endpoint class
 */
public class AmqpLwtServiceEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpLwtServiceEndpoint.class);

    public static final String LWT_SERVICE_ENDPOINT = "$lwt";

    private ProtonSender sender;

    /**
     * Constructor
     *
     * @param sender    ProtonSender instance related to control address
     */
    public AmqpLwtServiceEndpoint(ProtonSender sender) {
        this.sender = sender;
    }

    /**
     * Open the endpoint, attaching the link
     */
    public void open() {

        // attach sender link to $lwt
        this.sender
                .setQoS(ProtonQoS.AT_LEAST_ONCE)
                .open();
    }

    /**
     * Send the AMQP_WILL message to the Last Will and Testament Service
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
                        new ErrorCondition(LinkError.DETACH_FORCED, "Link detached due to a brute MQTT client disconnection");
                this.sender.setCondition(errorCondition);
            }

            // detach link
            this.sender.close();
        }
    }
}
