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

package enmasse.systemtest.mqtt;

import enmasse.systemtest.Endpoint;
import enmasse.systemtest.Logging;
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

    public Publisher(Endpoint endpoint, String topic, Queue<MqttMessage> messages, CompletableFuture<Integer> promise, CountDownLatch connectLatch) {
        super(endpoint, topic, promise);
        this.messageQueue = messages;
        this.connectLatch = connectLatch;
    }

    @Override
    protected void connectionOpened() {

        Logging.log.info("Publisher on '{}' connected, publishing messages", this.topic);
        this.connectLatch.countDown();
        this.sendNext();
    }

    private void sendNext() {

        MqttMessage message = this.messageQueue.poll();

        if (message == null) {

            // NOTE : Eclipse Paho doesn't allow to call "disconnect" from a callback
            //        an exception is raised for that !
            /* if (this.client.isConnected()) {
                this.client.disconnect();
            } */
            this.promise.complete(this.numSent.get());

        } else {

            try {

                this.client.publish(this.topic, message);
                this.numSent.incrementAndGet();
                this.sendNext();

            } catch (MqttException e) {
                e.printStackTrace();
            }

        }
    }
}
