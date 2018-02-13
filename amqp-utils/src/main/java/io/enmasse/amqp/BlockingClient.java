/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.amqp;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.message.Message;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Client that sends and receives messages blocking
 */
public class BlockingClient implements AutoCloseable {
    private final String host;
    private final int port;
    private final Vertx vertx;

    public BlockingClient(String host, int port) {
        this(host, port, Vertx.vertx());
    }

    public BlockingClient(String host, int port, Vertx vertx) {
        this.host = host;
        this.port = port;
        this.vertx = vertx;
    }

    public void send(String address, List<Message> messages, long timeout, TimeUnit timeUnit) throws InterruptedException {
        ProtonClient client = ProtonClient.create(vertx);
        CountDownLatch latch = new CountDownLatch(1);
        Queue<Message> messageQueue = new ArrayDeque<>(messages);
        client.connect(host, port, connectEvent -> {
            if (connectEvent.succeeded()) {
                ProtonConnection connection = connectEvent.result();
                connection.open();

                ProtonSender sender = connection.createSender(address);
                sender.openHandler(senderOpenEvent -> {
                    if (senderOpenEvent.succeeded()) {
                        sendNext(connection, sender, messageQueue, latch);
                    }
                });
                sender.open();
            }
        });
        boolean ok = latch.await(timeout, timeUnit);
        if (!ok) {
            throw new RuntimeException("Sending messages timed out, " + messageQueue.size() + " messages unsent");
        }
    }

    private void sendNext(ProtonConnection connection, ProtonSender sender, Queue<Message> messageQueue, CountDownLatch latch) {
        Message message = messageQueue.poll();

        if (message == null) {
            connection.close();
            latch.countDown();
        } else {
            sender.send(message, protonDelivery -> sendNext(connection, sender, messageQueue, latch));
        }
    }

    public List<Message> recv(String address, int numMessages, long timeout, TimeUnit timeUnit) throws InterruptedException {
        ProtonClient client = ProtonClient.create(vertx);
        CountDownLatch latch = new CountDownLatch(1);
        List<Message> messages = new ArrayList<>();
        client.connect(host, port, connectEvent -> {
            if (connectEvent.succeeded()) {
                ProtonConnection connection = connectEvent.result();
                connection.open();

                ProtonReceiver receiver = connection.createReceiver(address);
                receiver.setPrefetch(0);
                receiver.openHandler(r -> receiver.flow(1));
                receiver.handler((delivery, message) -> {
                    messages.add(message);
                    if (messages.size() == numMessages) {
                        vertx.runOnContext(h -> {
                            connection.close();
                            latch.countDown();
                        });
                    } else {
                        receiver.flow(1);
                    }
                });
                receiver.open();
            }
        });
        boolean ok = latch.await(timeout, timeUnit);
        if (!ok) {
            throw new RuntimeException("Sending messages timed out, " + messages.size() + " out of " + numMessages + " messages received");
        }
        return messages;
    }

    @Override
    public void close() {
        vertx.close();
    }
}
