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
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

public class Subscriber extends ClientHandlerBase<List<String>> {

    private final List<String> messages = new ArrayList<>();
    private final Predicate<MqttMessage> done;
    private final CountDownLatch connectLatch;

    public Subscriber(Endpoint endpoint, String topic, Predicate<MqttMessage> done, CompletableFuture<List<String>> promise, CountDownLatch connectLatch) {
        super(endpoint, topic, promise);
        this.done = done;
        this.connectLatch = connectLatch;
    }

    @Override
    protected void connectionOpened() {

        try {

            this.client.subscribe(this.topic, 0, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken iMqttToken) {

                    connectLatch.countDown();
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {

                }
            }, new IMqttMessageListener() {

                @Override
                public void messageArrived(String s, MqttMessage message) throws Exception {

                    messages.add(String.valueOf(message.getPayload()));
                    if (done.test(message)) {

                        // NOTE : Eclipse Paho doesn't allow to call "disconnect" from a callback
                        //        an exception is raised for that !
                        /* if (this.client.isConnected()) {
                            this.client.disconnect();
                        } */
                        promise.complete(messages);
                    }
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
