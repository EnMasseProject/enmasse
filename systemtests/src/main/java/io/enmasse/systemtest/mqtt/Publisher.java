/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.mqtt;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Publisher extends ClientHandlerBase<Integer> {

    private static Logger log = CustomLogger.getLogger();
    private final AtomicInteger numSent = new AtomicInteger(0);
    private final Queue<MqttMessage> messageQueue;
    private final CountDownLatch connectLatch;

    public Publisher(Endpoint endpoint,
                     final MqttConnectOptions options,
                     String topic,
                     Queue<MqttMessage> messages,
                     CompletableFuture<Integer> promise,
                     CountDownLatch connectLatch) {
        super(endpoint, options, topic, promise);
        this.messageQueue = messages;
        this.connectLatch = connectLatch;
    }

    @Override
    protected void connectionOpened() {

        log.info("Publisher on '{}' connected, publishing messages", this.getTopic());

        this.client.setCallback(new MqttCallback() {

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                log.info("Delivered message-id {}", iMqttDeliveryToken.getMessageId());
                numSent.incrementAndGet();
                sendNext();
            }
        });

        this.connectLatch.countDown();
        this.sendNext();
    }

    @Override
    protected void connectionOpenFailed(Throwable throwable) {
        log.info("Connection to " + getEndpoint().getHost() + ":" + getEndpoint().getPort() + " failed: " + throwable.getMessage());
        getPromise().completeExceptionally(throwable);
        this.connectLatch.countDown();
    }

    private void sendNext() {

        MqttMessage message = this.messageQueue.poll();

        if (message == null) {

            // NOTE : Eclipse Paho doesn't allow to call "disconnect" from a callback
            //        an exception is raised for that !
            /* if (this.client.isConnected()) {
                this.client.disconnect();
            } */
            this.getPromise().complete(this.numSent.get());

        } else {

            try {

                this.client.publish(this.getTopic(), message);

            } catch (MqttException e) {
                e.printStackTrace();
            }

        }
    }
}
