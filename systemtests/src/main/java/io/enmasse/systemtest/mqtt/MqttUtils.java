/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.mqtt;

import io.enmasse.systemtest.logs.CustomLogger;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MqttUtils {

    private static Logger log = CustomLogger.getLogger();

    public static <T> int awaitAndReturnCode(List<CompletableFuture<T>> futures, int timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException {
        CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            future.get(timeout, timeUnit);
            return (int) futures.stream().filter(CompletableFuture::isDone).count();
        } catch (TimeoutException e) {
            return (int) futures.stream().filter(CompletableFuture::isDone).count();
        }
    }

    public static List<CompletableFuture<Void>> publish(IMqttClient client, String address, List<MqttMessage> messages) throws MqttException {

        List<CompletableFuture<Void>> futures = Stream.generate(CompletableFuture<Void>::new)
                .limit(messages.size())
                .collect(Collectors.toList());
        Iterator<CompletableFuture<Void>> resultItr = futures.iterator();
        client.setCallback((MqttDeliveryCompleteCallback) token -> resultItr.next().complete(null));
        for (MqttMessage message : messages) {
            client.publish(address, message);
        }

        return futures;
    }

    public static List<CompletableFuture<MqttMessage>> subscribeAndReceiveMessages(IMqttClient client, String address, int size, int qos) throws MqttException {
        List<CompletableFuture<MqttMessage>> receiveFutures = Stream.generate(CompletableFuture<MqttMessage>::new)
                .limit(size)
                .collect(Collectors.toList());

        Iterator<CompletableFuture<MqttMessage>> resultItr = receiveFutures.iterator();
        client.subscribe(address, qos, (t, m) -> {
            assertThat("Unexpected message", resultItr.hasNext(), is(true));
            log.debug("Received expected message: {}, Topic: {}", m, t);
            resultItr.next().complete(m);
        });
        return receiveFutures;
    }
}
