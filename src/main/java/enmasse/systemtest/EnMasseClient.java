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

package enmasse.systemtest;

import io.vertx.core.Vertx;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EnMasseClient {
    private final Vertx vertx;
    private final Endpoint endpoint;
    private final TerminusFactory terminusFactory;
    private final ProtonClientOptions options;
    private volatile ProtonConnection connection;

    public EnMasseClient(Vertx vertx, Endpoint endpoint, TerminusFactory terminusFactory, ProtonClientOptions options) {
        this.vertx = vertx;
        this.endpoint = endpoint;
        this.terminusFactory = terminusFactory;
        this.options = options;
    }

    public EnMasseClient(Vertx vertx, Endpoint endpoint, TerminusFactory terminusFactory) {
        this(vertx, endpoint, terminusFactory, new ProtonClientOptions());
    }

    public Future<List<String>> recvMessages(String address, int numMessages) throws InterruptedException {
        return recvMessages(address, numMessages, 1, TimeUnit.MINUTES);
    }

    public Future<List<String>> recvMessages(String address, int numMessages, long connectTimeout, TimeUnit timeUnit) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> messages = new ArrayList<>();
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(options, endpoint.getHost(), endpoint.getPort(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.setContainer("enmasse-systemtest-client");
                connection.disconnectHandler(disconnected -> {
                    System.out.println("Receiver connection disconnected");
                });
                connection.closeHandler(closed -> {
                    System.out.println("Receiver connection closed");
                });
                connection.open();
                connection.createReceiver(address, new ProtonLinkOptions().setLinkName("enmasse-systemtest-client"))
                        .openHandler(opened -> {
                            latch.countDown();
                            System.out.println("Receiving messages from " + connection.getRemoteContainer());
                        })
                        .closeHandler(closed -> {
                            System.out.println("Receiver link closed");
                        })
                        .setSource(terminusFactory.getSource(address))
                        .handler((delivery, message) -> {
                            messages.add((String) ((AmqpValue) message.getBody()).getValue());
                            if (messages.size() == numMessages) {
                                connection.close();
                                vertx.runOnContext((id) -> future.complete(messages));
                            }
                        })
                        .open();
            } else {
                event.cause().printStackTrace();
                System.out.println("Connection open failed: " + event.cause().getMessage());
            }
        });
        boolean success = latch.await(connectTimeout, timeUnit);
        if (!success) {
            future.completeExceptionally(new RuntimeException("Unable to connect within timeout"));
        }
        return future;
    }

    public Future<Integer> sendMessages(String address, List<String> messages) {
        Message [] messageList = messages.stream()
                .map(body -> {
                    Message message = Message.Factory.create();
                    message.setBody(new AmqpValue(body));
                    message.setAddress(address);
                    return message;
                })
                .collect(Collectors.toList()).toArray(new Message[0]);
        return sendMessages(address, messageList);
    }

    public Future<Integer> sendMessages(String address, Message ... messages) {
        AtomicInteger count = new AtomicInteger(0);
        CompletableFuture<Integer> future = new CompletableFuture<>();
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(options, endpoint.getHost(), endpoint.getPort(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.openHandler(result -> {
                    ProtonSender sender = connection.createSender(address);
                    sender.setTarget(terminusFactory.getTarget(address));
                    sender.closeHandler(closed -> System.out.println("Sender link closed"));
                    sender.openHandler(remoteOpenResult -> {
                        for (Message message : messages) {
                            System.out.println("Sending message");
                            sender.send(message, delivery -> {
                                if (count.incrementAndGet() == messages.length) {
                                    connection.close();
                                    vertx.runOnContext((id) -> future.complete(count.get()));
                                }
                            });
                        }
                    });
                    sender.open();
                });
                connection.closeHandler(closed -> System.out.println("Sender connection closed"));
                connection.disconnectHandler(closed -> System.out.println("Sender connection disconnected"));
                connection.open();
            } else {
                event.cause().printStackTrace();
                System.out.println("Connection open failed: " + event.cause().getMessage());
            }
        });
        return future;
    }
}
