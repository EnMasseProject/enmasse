/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.endpoints;

import enmasse.mqtt.messages.AmqpPublishMessage;
import enmasse.mqtt.messages.AmqpPubrelMessage;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Publisher endpoint
 */
public class AmqpPublishEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpPublishEndpoint.class);

    public static final String AMQP_CLIENT_PUBREL_ENDPOINT_TEMPLATE = "$mqtt.%s.pubrel";

    // all delivery for published messages if they need settlement (messageId -> delivery)
    private Map<Object, ProtonDelivery> deliveries;
    // links for publishing message on topic (topic -> link/senders couple)
    private Map<String, AmqpPublisher> publishers;
    // sender for PUBREL messages
    private ProtonSender senderPubrel;

    /**
     * Constructor
     *
     * @param senderPubrel  ProtonSender instance related to client PUBREL address
     */
    public AmqpPublishEndpoint(ProtonSender senderPubrel) {
        this.senderPubrel = senderPubrel;
    }

    /**
     * Open the endpoint
     */
    public void open() {

        this.deliveries = new HashMap<>();
        this.publishers = new HashMap<>();
    }

    /**
     * Check if a publisher already exists for the specified topic
     *
     * @param topic topic to check the publisher
     * @return  if publisher already exists
     */
    public boolean isPublisher(String topic) {

        return this.publishers.containsKey(topic);
    }

    /**
     * Add a publisher to the endpoint
     *
     * @param topic topic for which adding the publisher
     * @param amqpPublisher publisher to add
     */
    public void addPublisher(String topic, AmqpPublisher amqpPublisher) {

        if (this.publishers.containsKey(topic)) {
            throw new IllegalStateException(String.format("AMQP publisher for %s already exists !", topic));
        }
        this.publishers.put(topic, amqpPublisher);
    }

    /**
     * Send the AMQP_PUBLISH to the attached topic/address
     *
     * @param amqpPublishMessage    AMQP_PUBLISH message
     */
    public void publish(AmqpPublishMessage amqpPublishMessage, Handler<AsyncResult<ProtonDelivery>> handler) {

        // send AMQP_PUBLISH message

        AmqpPublisher publisher = this.publishers.get(amqpPublishMessage.topic());

        // use sender for QoS 0/1 messages
        if (amqpPublishMessage.qos() != MqttQoS.EXACTLY_ONCE) {

            // attach sender link on "topic" (if doesn't exist yet)
            if (!publisher.senderQoS01().isOpen()) {

                publisher.senderQoS01()
                        .setQoS(ProtonQoS.AT_LEAST_ONCE)
                        .open();

                // TODO: think about starting a timer for inactivity on this link for detaching ?
            }

            if (amqpPublishMessage.qos() == MqttQoS.AT_MOST_ONCE) {

                publisher.senderQoS01().send(amqpPublishMessage.toAmqp());
                handler.handle(Future.succeededFuture(null));

            } else {

                publisher.senderQoS01().send(amqpPublishMessage.toAmqp(), delivery -> {

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

            // attach sender link on "topic" (if doesn't exist yet)
            if (!publisher.senderQoS2().isOpen()) {

                publisher.senderQoS2()
                        // TODO: Vert.x Proton doesn't support EXACTLY_ONCE
                        .open();

                // TODO: think about starting a timer for inactivity on this link for detaching ?
            }

            publisher.senderQoS2().send(amqpPublishMessage.toAmqp(), delivery -> {

                if (delivery.getRemoteState() == Accepted.getInstance()) {
                    LOG.info("AMQP publish delivery {}", delivery.getRemoteState());

                    // received disposition not settled, store for future settlement
                    if (!delivery.remotelySettled()) {
                        this.deliveries.put(amqpPublishMessage.messageId(), delivery);
                    }

                    handler.handle(Future.succeededFuture(delivery));
                } else {
                    handler.handle(Future.failedFuture(String.format("AMQP publish delivery %s", delivery.getRemoteState())));
                }
            });
        }

    }

    /**
     * Send the AMQP_PUBREL to the related client pubrel address
     *
     * @param amqpPubrelMessage    AMQP_PUBREL message
     */
    public void publish(AmqpPubrelMessage amqpPubrelMessage, Handler<AsyncResult<ProtonDelivery>> handler) {

        // send AMQP_PUBREL message

        if (!this.senderPubrel.isOpen()) {

            this.senderPubrel
                    .setQoS(ProtonQoS.AT_LEAST_ONCE)
                    .open();

            // TODO: think about starting a timer for inactivity on this link for detaching ?
        }

        this.senderPubrel.send(amqpPubrelMessage.toAmqp(), delivery -> {

            if (delivery.getRemoteState() == Accepted.getInstance()) {
                LOG.info("AMQP pubrel delivery {}", delivery.getRemoteState());
                handler.handle(Future.succeededFuture(delivery));
            } else {
                handler.handle(Future.failedFuture(String.format("AMQP pubrel delivery %s", delivery.getRemoteState())));
            }

        });
    }

    /**
     * Close the endpoint, detaching the links
     */
    public void close() {

        // detach links
        for (Map.Entry<String, AmqpPublisher> entry: this.publishers.entrySet()) {

            AmqpPublisher publisher = entry.getValue();
            if (publisher.isOpen()) {
                publisher.close();
            }
        }

        if (this.senderPubrel.isOpen()) {
            this.senderPubrel.close();
        }

        this.publishers.clear();
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
}
