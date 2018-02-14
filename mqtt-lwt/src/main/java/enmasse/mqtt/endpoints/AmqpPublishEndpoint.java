/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.endpoints;

import enmasse.mqtt.messages.AmqpPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonConnection;
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

    private ProtonConnection connection;

    /**
     * Constructor
     *
     * @param connection    ProtonConnection instance
     */
    public AmqpPublishEndpoint(ProtonConnection connection) {
        this.connection = connection;
    }

    /**
     * Open the endpoint, opening the connection
     */
    public void open() {

        this.connection
                .sessionOpenHandler(session -> session.open())
                .open();
    }

    /**
     * Send the AMQP_PUBLISH to the attached topic/address
     *
     * @param amqpPublishMessage   AMQP_PUBLISH message
     */
    public void publish(AmqpPublishMessage amqpPublishMessage, Handler<AsyncResult<ProtonDelivery>> handler) {

        // send AMQP_PUBLISH message

        LOG.info("Will ready for publishing on topic [{}]", amqpPublishMessage.topic());

        // use sender for QoS 0/1 messages
        if (amqpPublishMessage.qos() != MqttQoS.EXACTLY_ONCE) {

            ProtonSender sender = this.connection.createSender(amqpPublishMessage.topic());

            sender.setQoS(ProtonQoS.AT_LEAST_ONCE)
                  .open();

            if (amqpPublishMessage.qos() == MqttQoS.AT_MOST_ONCE) {

                sender.send(amqpPublishMessage.toAmqp());
                sender.close();
                LOG.info("AMQP published on {}", amqpPublishMessage.topic());

                handler.handle(Future.succeededFuture(null));

            } else {

                sender.send(amqpPublishMessage.toAmqp(), delivery -> {

                    if (delivery.getRemoteState() == Accepted.getInstance()) {
                        LOG.info("AMQP publish delivery {}", delivery.getRemoteState());
                        handler.handle(Future.succeededFuture(delivery));
                    } else {
                        handler.handle(Future.failedFuture(String.format("AMQP publish delivery %s", delivery.getRemoteState())));
                    }

                    sender.close();
                });
            }

        // use sender for QoS 2 messages
        } else {

            // TODO
        }
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
}
