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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
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

    private static final Symbol AMQP_LINK_REDIRECT = Symbol.valueOf("amqp:link:redirect");
    private class RedirectableReceiver implements Handler<AsyncResult<ProtonReceiver>> {
        private ProtonConnection connection;
        private final Source source;
        private final String name;
        private final Handler<AsyncResult<ProtonReceiver>> openHandler;
        private final ProtonMessageHandler messageHandler;
        private ProtonReceiver receiver;

        RedirectableReceiver(ProtonConnection connection, Source source, String name,
                             Handler<AsyncResult<ProtonReceiver>> openHandler,
                             ProtonMessageHandler messageHandler) {
            this.connection = connection;
            this.source = source;
            this.name = name;
            this.openHandler = openHandler;
            this.messageHandler = messageHandler;
        }

        ProtonReceiver open() {
            if (receiver != null) {
                receiver = connection.open().createReceiver(source.getAddress(), new ProtonLinkOptions().setLinkName(name));
            } else {
                receiver = connection.createReceiver(source.getAddress(), new ProtonLinkOptions().setLinkName(name));
            }
            receiver.openHandler(openHandler);
            receiver.closeHandler(this);
            receiver.setSource(source);
            receiver.handler(messageHandler);
            receiver.open();
            return receiver;
        }

        ProtonReceiver get() {
            return receiver;
        }

        void recreateConnectionAndReattach() {
            ProtonClient client = ProtonClient.create(vertx);
            client.connect(options, endpoint.getHost(), endpoint.getPort(), event -> {
                    if (event.succeeded()) {
                        System.out.println("opened new connection for link redirect");
                        connection = event.result();
                        connection.setContainer("enmasse-systemtest-client");
                        connection.disconnectHandler(disconnected -> {
                                System.out.println("Receiver connection disconnected");
                            });
                        connection.closeHandler(closed -> {
                                System.out.println("Receiver connection closed");
                            });
                        connection.open();
                        open();
                    } else {
                        System.out.println("failed to open new connection for link redirect: " + event.cause());
                    }
                });
        }

        public void handle(AsyncResult<ProtonReceiver> closed) {
            receiver.close();
            ErrorCondition error = receiver.getRemoteCondition();
            if (error != null && AMQP_LINK_REDIRECT.equals(error.getCondition())) {
                String relocated = (String) error.getInfo().get("address");
                System.out.println("Receiver link redirected to " + relocated);
                source.setAddress(relocated);
                open();
                //recreateConnectionAndReattach();
            } else {
                handleReceiverClose(receiver);
            }
        }

        protected void handleReceiverClose(ProtonReceiver receiver) {
            ErrorCondition error = receiver.getRemoteCondition();
            if (error == null || error.getCondition() == null) {
                System.out.println("Receiver link closed without error");
            } else {
                System.out.println("Receiver link closed with " + error);
            }
        }
    }

    public Future<List<String>> recvMessages(String address, int numMessages) throws InterruptedException {
        return recvMessages(address, numMessages, 1, TimeUnit.MINUTES);
    }

    public Future<List<String>> recvMessages(String address, int numMessages, long connectTimeout, TimeUnit timeUnit) throws InterruptedException {
        return recvMessages(terminusFactory.getSource(address), numMessages, connectTimeout, timeUnit);
    }

    public Future<List<String>> recvMessages(Source source, int numMessages) throws InterruptedException {
        return recvMessages(source, numMessages, 1, TimeUnit.MINUTES);
    }

    public Future<List<String>> recvMessages(Source source, Predicate<Message> done) throws InterruptedException {
        return recvMessages(source, done, 1, TimeUnit.MINUTES);
    }

    public Future<List<String>> recvMessages(Source source, Predicate<Message> done, long connectTimeout, TimeUnit timeUnit) throws InterruptedException {
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
                String linkName = "enmasse-systemtest-client";
                new RedirectableReceiver(connection, source, "enmasse-systemtest-client",
                                         opened -> {
                                             if (opened.succeeded()) {
                                                 Source remote = (Source) opened.result().getRemoteSource();
                                                 if (remote != null && remote.getAddress() != null) {
                                                     latch.countDown();
                                                     System.out.println("Receiving messages from " + connection.getRemoteContainer());
                                                 }
                                             } else {
                                                 System.out.println("receiver failed to open: " + opened.cause());
                                             }
                                         },
                                         (delivery, message) -> {
                                             messages.add((String) ((AmqpValue) message.getBody()).getValue());
                                             if (done.test(message)) {
                                                 delivery.disposition(Accepted.getInstance(), true);
                                                 connection.close();
                                                 vertx.runOnContext((id) -> {
                                                         future.complete(messages);
                                                     });
                                             }
                                         }).open();
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

    private static class Count implements Predicate<Message> {
        private final int expected;
        private int actual;

        Count(int expected) {
            this.expected = expected;
        }

        public boolean test(Message message) {
            return ++actual == expected;
        }
    }

    public Future<List<String>> recvMessages(Source source, int numMessages, long connectTimeout, TimeUnit timeUnit) throws InterruptedException {
        return recvMessages(source, new Count(numMessages), connectTimeout, timeUnit);
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
                                System.out.println("message confirmed");
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
