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
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

public class Subscriber extends ClientHandlerBase<List<String>> {

    private final List<String> messages = new ArrayList<>();
    private final int qos;
    private final Predicate<MqttMessage> done;
    private final CountDownLatch connectLatch;

    public Subscriber(Endpoint endpoint,
                      final MqttConnectOptions options,
                      String topic,
                      int qos,
                      Predicate<MqttMessage> done,
                      CompletableFuture<List<String>> promise,
                      CountDownLatch connectLatch) {
        super(endpoint, options, topic, promise);
        this.qos = qos;
        this.done = done;
        this.connectLatch = connectLatch;
    }

    @Override
    protected void connectionOpened() {

        try {

            this.client.subscribe(this.getTopic(), this.qos, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken iMqttToken) {

                    Logging.log.info("Subscription response code {}", iMqttToken.getGrantedQos()[0]);

                    if (iMqttToken.getGrantedQos()[0] == 0x80) {
                        getPromise().completeExceptionally(new RuntimeException("Subscription refused"));
                    }

                    connectLatch.countDown();
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {

                    Logging.log.info("Subscribe to '{}' failed: {}", iMqttToken.getTopics()[0], throwable.getMessage());
                    getPromise().completeExceptionally(throwable);
                    connectLatch.countDown();
                }
            }, new IMqttMessageListener() {

                @Override
                public void messageArrived(String s, MqttMessage message) throws Exception {

                    Logging.log.info("Arrived message-id {}", message.getId());
                    messages.add(String.valueOf(message.getPayload()));
                    if (done.test(message)) {

                        // NOTE : Eclipse Paho doesn't allow to call "disconnect" from a callback
                        //        an exception is raised for that !
                        /* if (this.client.isConnected()) {
                            this.client.disconnect();
                        } */
                        getPromise().complete(messages);
                    }
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void connectionOpenFailed(Throwable throwable) {
        Logging.log.info("Connection to " + getEndpoint().getHost() + ":" + getEndpoint().getPort() + " failed: " + throwable.getMessage());
        getPromise().completeExceptionally(throwable);
        connectLatch.countDown();
    }
}
