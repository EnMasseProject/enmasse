/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.mqtt;

import io.enmasse.systemtest.Count;
import io.enmasse.systemtest.Endpoint;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class MqttClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MqttClient.class);
    private final List<ClientHandlerBase> clients = new ArrayList<>();
    private Endpoint endpoint;
    private MqttConnectOptions options;

    public MqttClient(Endpoint endpoint, final MqttConnectOptions options) {
        this.endpoint = endpoint;
        this.options = options;
    }

    public MqttClient setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public MqttConnectOptions getMqttConnectOptions() {
        return options;
    }

    public MqttClient setMqttConnectOptions(MqttConnectOptions options) {
        this.options = options;
        return this;
    }

    public Future<List<MqttMessage>> recvMessages(String topic, int numMessages) throws ExecutionException, InterruptedException {
        return this.recvMessages(topic, numMessages, 0, 1, TimeUnit.MINUTES);
    }

    public Future<List<MqttMessage>> recvMessages(String topic, int numMessages, int qos) throws ExecutionException, InterruptedException {
        return this.recvMessages(topic, numMessages, qos, 1, TimeUnit.MINUTES);
    }

    public Future<Integer> sendMessages(String topic, List<String> messages) throws InterruptedException {
        return this.sendMessages(topic, messages, Collections.nCopies(messages.size(), 0), 1, TimeUnit.MINUTES);
    }

    public Future<Integer> sendMessages(String topic, List<String> messages, List<Integer> qos) throws InterruptedException {
        return this.sendMessages(topic, messages, qos, 1, TimeUnit.MINUTES);
    }

    public Future<Integer> sendMessages(String topic, MqttMessage... messages) throws InterruptedException {
        return this.sendMessages(topic, Arrays.asList(messages), 1, TimeUnit.MINUTES);
    }

    public Future<List<MqttMessage>> recvMessages(String topic, int numMessages, int qos, long connectTimeout, TimeUnit timeUnit) throws ExecutionException, InterruptedException {
        CompletableFuture<List<MqttMessage>> promise = new CompletableFuture<>();
        CountDownLatch connectLatch = new CountDownLatch(1);

        Subscriber subscriber = new Subscriber(this.endpoint, this.options, topic, qos, new Count<>(numMessages), promise, connectLatch);
        subscriber.start();

        this.clients.add(subscriber);
        if (!connectLatch.await(connectTimeout, timeUnit)) {
            throw new RuntimeException("Timeout waiting for client to connect");
        }
        if (promise.isCompletedExceptionally()) {
            try {
                promise.get();
            } catch (ExecutionException e) {
                log.error("Error receiving messages", e);
                throw e;
            }
        }
        return promise;
    }

    public Future<Integer> sendMessages(String topic, List<MqttMessage> messages, long connectTimeout, TimeUnit timeUnit) throws InterruptedException {
        Queue<MqttMessage> messageQueue = new LinkedList<>();
        for (MqttMessage message : messages) {
            messageQueue.add(message);
        }
        CompletableFuture<Integer> promise = new CompletableFuture<>();
        publishMessages(topic, messageQueue, promise, connectTimeout, timeUnit);
        return promise;
    }

    public Future<Integer> sendMessages(String topic, List<String> messages, List<Integer> qos, long connectTimeout, TimeUnit timeUnit) throws InterruptedException {

        if (messages.size() != qos.size())
            throw new IllegalArgumentException("Number of messages and qos don't match");

        Iterator<String> messageIterator = messages.iterator();
        Iterator<Integer> qosIterator = qos.iterator();

        Queue<MqttMessage> messageQueue = new LinkedList<>();
        int messageId = 0;
        while (messageIterator.hasNext() && qosIterator.hasNext()) {
            MqttMessage message = new MqttMessage();
            message.setId(++messageId);
            message.setPayload(messageIterator.next().getBytes());
            message.setQos(qosIterator.next());
            messageQueue.add(message);
        }

        CompletableFuture<Integer> promise = new CompletableFuture<>();
        publishMessages(topic, messageQueue, promise, connectTimeout, timeUnit);
        return promise;
    }

    public Future<Integer> publishMessages(String topic, Queue<MqttMessage> messageQueue, CompletableFuture<Integer> promise, long connectTimeout, TimeUnit timeUnit) throws InterruptedException {
        CountDownLatch connectLatch = new CountDownLatch(1);

        Publisher publisher = new Publisher(this.endpoint, this.options, topic, messageQueue, promise, connectLatch);
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

}
