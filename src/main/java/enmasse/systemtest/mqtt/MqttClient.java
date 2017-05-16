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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class MqttClient {

    private final Endpoint endpoint;
    private final List<org.eclipse.paho.client.mqttv3.MqttClient> clients = new ArrayList<>();

    public MqttClient(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Future<List<String>> recvMessages(String address, int numMessages) {

        CompletableFuture<List<String>> promise = new CompletableFuture<>();

        Subscriber subscriber = new Subscriber(this.endpoint);

        return promise;
    }

    public Future<Integer> sendMessages(String address, List<String> messages) {

        CompletableFuture<Integer> promise = new CompletableFuture<>();

        Publisher publisher = new Publisher(this.endpoint);

        return promise;
    }
}
