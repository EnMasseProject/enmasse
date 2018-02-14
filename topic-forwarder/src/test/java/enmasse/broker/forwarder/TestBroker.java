/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.forwarder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.message.Message;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class TestBroker extends AbstractVerticle {
    private final String address;
    private ProtonClient protonClient;
    private ProtonServer protonServer;
    private final int id;
    private final AsyncQueue queue = new AsyncQueue();
    private final AtomicInteger numConsumers = new AtomicInteger(0);
    private final AtomicInteger numProducers = new AtomicInteger(0);

    public TestBroker(int id, String address) {
        this.id = id;
        this.address = address;
    }

    @Override
    public void start(Future<Void> promise) throws Exception {
        protonClient = ProtonClient.create(vertx);
        protonServer = ProtonServer.create(vertx)
                .connectHandler(connnection -> {
                    connnection.setContainer("broker-" + id);
                    connnection.openHandler(conn -> {
                        if (conn.succeeded()) {
                            conn.result().open();
                        } else {
                            System.err.println("Error on open: " + conn.cause().getMessage());
                        }
                    });
                    connnection.sessionOpenHandler(ProtonSession::open);
                    connnection.receiverOpenHandler(receiver -> {
                        receiver.handler((delivery, message) -> queue.add(message));
                        receiver.closeHandler(r -> numProducers.decrementAndGet());
                        receiver.open();
                        numProducers.incrementAndGet();
                    });

                    connnection.senderOpenHandler(sender -> {
                        queue.registerListener(sender::send);
                        sender.closeHandler(s -> numConsumers.decrementAndGet());
                        sender.open();
                        numConsumers.incrementAndGet();
                    });
                }).listen(0,"127.0.0.1", result -> {
                    if (result.succeeded()) {
                        promise.complete();
                    } else {
                        promise.fail(result.cause());
                    }
                });
    }

    public int getId() {
        return id;
    }

    private static class AsyncQueue {
        private final List<AsyncQueueListener> listeners = new ArrayList<>();

        public synchronized void add(Message message){
            for (AsyncQueueListener listener : listeners) {
                listener.messageAdded(message);
            }
        }

        public synchronized void registerListener(AsyncQueueListener listener) {
            this.listeners.add(listener);
        }
    }

    private interface AsyncQueueListener {
        void messageAdded(Message message);
    }


    public void sendMessage(String messageBody, long timeout, TimeUnit timeUnit) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        protonClient.connect("127.0.0.1", protonServer.actualPort(), event -> {
            ProtonConnection connection = event.result().open();
            Target target = new Target();
            target.setAddress(address);
            target.setCapabilities(Symbol.getSymbol("topic"));
            ProtonSender sender = connection.createSender(address);
            sender.setTarget(target);
            sender.open();
            Message message = Message.Factory.create();
            message.setBody(new AmqpValue(messageBody));
            message.setAddress(address);
            sender.send(message, delivery -> latch.countDown());
        });
        latch.await(timeout, timeUnit);
    }

    public CompletableFuture<List<String>> recvMessages(long numMessages, long attachTimeout, TimeUnit timeUnit) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        List<String> messages = new ArrayList<>();
        protonClient.connect("localhost", protonServer.actualPort(), event -> {
            ProtonConnection connection = event.result().open();
            Source source = new Source();
            source.setAddress(address);
            source.setCapabilities(Symbol.getSymbol("topic"));
            connection.createReceiver(address)
                    .openHandler(opened -> latch.countDown())
                    .setSource(source)
                    .handler((delivery, message) -> {
                        messages.add((String) ((AmqpValue) message.getBody()).getValue());
                        if (messages.size() == numMessages) {
                            future.complete(new ArrayList<>(messages));
                        }
                    })
                    .open();
        });
        latch.await(attachTimeout, timeUnit);
        return future;
    }

    public int numConnected() {
        return numConsumers.get() + numProducers.get();
    }

    @Override
    public void stop(Future<Void> promise) throws Exception {
        if (protonServer != null) {
            vertx.runOnContext(id -> {
                protonServer.close();
            });
        }
    }

    public int getPort() {
        if (protonServer != null) {
            return protonServer.actualPort();
        }
        return -1;
    }
}
