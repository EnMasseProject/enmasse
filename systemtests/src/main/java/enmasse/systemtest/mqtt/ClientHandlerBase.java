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
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class ClientHandlerBase<T> {

    private final String SERVER_URI_TEMPLATE = "tcp://%s:%s";

    private final Endpoint endpoint;
    protected final String topic;
    protected final CompletableFuture<T> promise;

    protected IMqttAsyncClient client;

    public ClientHandlerBase(Endpoint endpoint, String topic, CompletableFuture<T> promise) {
        this.endpoint = endpoint;
        this.topic = topic;
        this.promise = promise;
    }

    public void start() {

        try {

            this.client =
                    new MqttAsyncClient(String.format(SERVER_URI_TEMPLATE, this.endpoint.getHost(), this.endpoint.getPort()),
                            UUID.randomUUID().toString());

            this.client.connect(null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    connectionOpened();
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Logging.log.info("Connection to " + endpoint.getHost() + ":" + endpoint.getPort() + " failed: " + throwable.getMessage());
                    promise.completeExceptionally(throwable);
                }
            });


        } catch (MqttException e) {

            e.printStackTrace();
        }

    }

    public void close() {

        try {

            this.client.disconnect();
            this.client.close();

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    protected abstract void connectionOpened();
}
