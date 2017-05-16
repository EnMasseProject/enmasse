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
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MqttClient implements AutoCloseable {

    private final Endpoint endpoint;
    private final List<ClientHandlerBase> clients = new ArrayList<>();

    public MqttClient(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Future<List<String>> recvMessages(String topic, int numMessages) throws InterruptedException {
        return this.recvMessages(topic, numMessages, 1, TimeUnit.MINUTES);
    }

    public Future<Integer> sendMessages(String topic, List<String> messages) throws InterruptedException {
        return this.sendMessages(topic, messages, 1, TimeUnit.MINUTES);
    }

    public Future<List<String>> recvMessages(String topic, int numMessages, long connectTimeout, TimeUnit timeUnit) throws InterruptedException {

        CompletableFuture<List<String>> promise = new CompletableFuture<>();
        CountDownLatch connectLatch = new CountDownLatch(1);

        Subscriber subscriber = new Subscriber(this.endpoint, topic, new Count(numMessages), promise, connectLatch);
        subscriber.start();

        this.clients.add(subscriber);
        if (!connectLatch.await(connectTimeout, timeUnit)) {
            throw new RuntimeException("Timeout waiting for client to connect");
        }
        return promise;
    }

    public Future<Integer> sendMessages(String topic, List<String> messages, long connectTimeout, TimeUnit timeUnit) throws InterruptedException {

        MqttMessage[] messageList =
                messages.stream()
                        .map(body -> {
                            MqttMessage message = new MqttMessage();
                            message.setPayload(body.getBytes());
                            message.setQos(0);
                            return message;
                        }).collect(Collectors.toList()).toArray(new MqttMessage[0]);

        Queue<MqttMessage> messageQueue = new LinkedList<>(Arrays.asList(messageList));

        CompletableFuture<Integer> promise = new CompletableFuture<>();
        CountDownLatch connectLatch = new CountDownLatch(1);

        Publisher publisher = new Publisher(this.endpoint, topic, messageQueue, promise, connectLatch);
        publisher.start();

        this.clients.add(publisher);
        if (!connectLatch.await(connectTimeout, timeUnit)) {
            throw new RuntimeException("Timeout waiting for client to connect");
        }
        return promise;
    }

    @Override
    public void close() throws Exception {

        for (ClientHandlerBase client : this.clients) {
            client.close();
        }
    }

    private static class Count implements Predicate<MqttMessage> {

        private final int expected;
        private int actual;

        Count(int expected) {
            this.expected = expected;
        }

        @Override
        public boolean test(MqttMessage mqttMessage) {
            return ++actual == expected;
        }
    }
}
