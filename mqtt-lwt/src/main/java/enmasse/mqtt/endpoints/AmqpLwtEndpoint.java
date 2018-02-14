/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.endpoints;

import enmasse.mqtt.DisconnectionData;
import enmasse.mqtt.WillData;
import enmasse.mqtt.messages.AmqpWillMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LWT endpoint
 */
public class AmqpLwtEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpLwtEndpoint.class);

    public static final int AMQP_WILL_CREDITS = 1;
    public static final String LWT_SERVICE_ENDPOINT = "$lwt";

    private ProtonConnection connection;

    private Handler<WillData> willHandler;
    private Handler<DisconnectionData> disconnectionHandler;

    /**
     * Constructor
     *
     * @param connection    ProtonConnection instance
     */
    public AmqpLwtEndpoint(ProtonConnection connection) {
        this.connection = connection;
    }

    /**
     * Open the endpoint, opening the connection
     */
    public void open() {

        if ((this.willHandler == null) ||
            (this.disconnectionHandler == null)) {
            throw new IllegalStateException("Handlers for received will and disconnection must be set");
        }

        this.connection
                .sessionOpenHandler(session -> session.open())
                .receiverOpenHandler(this::receiverHandler)
                .open();
    }

    /**
     * Close the endpoint, closing the connection
     */
    public void close() {

        // TODO : check what to close other than connection while this class evolves
        if (this.connection != null) {
            this.connection.close();
        }
    }

    private void receiverHandler(ProtonReceiver receiver) {

        LOG.info("Attaching link request");

        // the LWT service supports only the control address
        if (!receiver.getRemoteTarget().getAddress().equals(LWT_SERVICE_ENDPOINT)) {

            ErrorCondition errorCondition =
                    new ErrorCondition(AmqpError.NOT_FOUND, "The provided address isn't supported");

            receiver.setCondition(errorCondition)
                    .close();
        } else {

            receiver.setTarget(receiver.getRemoteTarget())
                    .setQoS(ProtonQoS.AT_LEAST_ONCE)
                    .handler((delivery, message) -> {
                        this.messageHandler(receiver, delivery, message);
                    })
                    .closeHandler(ar -> {
                        this.closeHandler(receiver, ar);
                    })
                    .detachHandler(ar -> {
                        this.closeHandler(receiver, ar);
                    })
                    .setPrefetch(0)
                    .open();

            receiver.flow(AMQP_WILL_CREDITS);
        }
    }

    private void messageHandler(ProtonReceiver receiver, ProtonDelivery delivery, Message message) {

        try {

            AmqpWillMessage amqpWillMessage = AmqpWillMessage.from(message);

            LOG.info("Received will on topic [{}] by client [{}]", amqpWillMessage.topic(), receiver.getName());

            // TODO : having a callback to check if handling went well and send right disposition ?
            this.willHandler.handle(new WillData(receiver.getName(), amqpWillMessage));

            delivery.disposition(Accepted.getInstance(), true);

            // NOTE : after receiving the AMQP_WILL, a new credit is issued because
            //        with AMQP we want to change the "will" message during the client life
            receiver.flow(AMQP_WILL_CREDITS);

        } catch (IllegalArgumentException ex) {

            LOG.error("Error decoding will message", ex);

            ErrorCondition errorCondition =
                    new ErrorCondition(AmqpError.DECODE_ERROR, "Received message is not a will message");

            receiver.setCondition(errorCondition)
                    .close();
        }
    }

    private void closeHandler(ProtonReceiver receiver, AsyncResult<ProtonReceiver> ar) {

        // link detached without error, so the "will" should be cleared and not sent
        if (ar.succeeded()) {

            LOG.info("Clean disconnection from {}", receiver.getName());

            // TODO: for now nothing to do ?

        // link detached with error, so the "will" should be sent
        } else {

            LOG.info("Brute disconnection from {}", receiver.getName());

            ErrorCondition errorCondition = new ErrorCondition(receiver.getRemoteCondition().getCondition(),
                    String.format("client detached with: %s", receiver.getRemoteCondition().getDescription()));

            receiver.setCondition(errorCondition);
        }

        receiver.close();

        if (this.disconnectionHandler != null) {
            this.disconnectionHandler.handle(new DisconnectionData(receiver.getName(), ar.failed()));
        }
    }

    /**
     * Set the handler called when an AMQP_WILL message is received on the endpoint
     *
     * @param handler   the handler
     * @return  a reference to the current AmqpLwtEndpoint instance
     */
    public AmqpLwtEndpoint willHandler(Handler<WillData> handler) {

        this.willHandler = handler;
        return this;
    }

    /**
     * Set the handler called when a client disconnect on this endpoint
     *
     * @param handler   the handler
     * @return  a reference to the current AmqpLwtEndpoint instance
     */
    public AmqpLwtEndpoint disconnectionHandler(Handler<DisconnectionData> handler) {

        this.disconnectionHandler = handler;
        return this;
    }
}
