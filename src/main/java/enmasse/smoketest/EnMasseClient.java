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

package enmasse.smoketest;

import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EnMasseClient {
    private final ProtonClient client;
    private final Endpoint endpoint;
    private final boolean isTopic;

    public EnMasseClient(ProtonClient protonClient, Endpoint endpoint, boolean isTopic) {
        this.client = protonClient;
        this.endpoint = endpoint;
        this.isTopic = isTopic;
    }

    public Future<List<String>> recvMessages(String address, int numMessages) throws InterruptedException {
        return recvMessages(address, numMessages, 1, TimeUnit.MINUTES);
    }

    public Future<List<String>> recvMessages(String address, int numMessages, long connectTimeout, TimeUnit timeUnit) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> messages = new ArrayList<>();
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        client.connect(endpoint.getHost(), endpoint.getPort(), event -> {
            ProtonConnection connection = event.result().open();
            Source source = new Source();
            source.setAddress(address);
            if (isTopic) {
                source.setCapabilities(Symbol.getSymbol("topic"));
            }
            connection.createReceiver(address)
                .openHandler(opened -> {
                    latch.countDown();
                    System.out.println("Receiving messages from " + connection.getRemoteContainer());
                })
                .setSource(source)
                .handler((delivery, message) -> {
                    messages.add((String)((AmqpValue)message.getBody()).getValue());
                    if (messages.size() == numMessages) {
                        future.complete(messages);
                    }
                })
                .open();
        });
        boolean success = latch.await(connectTimeout, timeUnit);
        if (!success) {
            future.completeExceptionally(new RuntimeException("Unable to connect within timeout"));
        }
        return future;
    }

    public Future<Integer> sendMessages(String address, List<String> messages) {
        AtomicInteger count = new AtomicInteger(0);
        CompletableFuture<Integer> future = new CompletableFuture<>();
        client.connect(endpoint.getHost(), endpoint.getPort(), event -> {
            ProtonConnection connection = event.result();
            connection.openHandler(result -> System.out.println("Sending messages to " + result.result().getRemoteContainer()));
            connection.open();
            Target target = new Target();
            target.setAddress(address);
            if (isTopic) {
                target.setCapabilities(Symbol.getSymbol("topic"));
            }
            ProtonSender sender = connection.createSender(address);
            sender.setTarget(target);
            sender.open();
            for (String body : messages) {
                Message message = Message.Factory.create();
                message.setBody(new AmqpValue(body));
                message.setAddress(address);
                sender.send(message, delivery -> {
                    if (count.incrementAndGet() == messages.size()) {
                        future.complete(count.get());
                    }
                });
            }
        });
        return future;
    }
}
