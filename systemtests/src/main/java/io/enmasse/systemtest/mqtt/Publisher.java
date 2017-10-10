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

package io.enmasse.systemtest.mqtt;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Logging;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Publisher extends ClientHandlerBase<Integer> {

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

        Logging.log.info("Publisher on '{}' connected, publishing messages", this.getTopic());

        this.client.setCallback(new MqttCallback() {

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                Logging.log.info("Delivered message-id {}", iMqttDeliveryToken.getMessageId());
                numSent.incrementAndGet();
                sendNext();
            }
        });

        this.connectLatch.countDown();
        this.sendNext();
    }

    @Override
    protected void connectionOpenFailed(Throwable throwable) {
        Logging.log.info("Connection to " + getEndpoint().getHost() + ":" + getEndpoint().getPort() + " failed: " + throwable.getMessage());
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
