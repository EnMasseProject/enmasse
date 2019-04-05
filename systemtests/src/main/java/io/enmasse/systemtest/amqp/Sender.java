/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.amqp;

import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.message.Message;

import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonSender;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

class Sender extends AbstractSender<Integer> {

    private final AtomicInteger numSent = new AtomicInteger(0);
    protected final Iterator<Message> messageQueue;
    protected final Predicate<Message> predicate;

    public Sender(AmqpConnectOptions clientOptions,
                  LinkOptions linkOptions,
                  Iterable<Message> messages,
                  final Predicate<Message> predicate,
                  CompletableFuture<Void> connectPromise,
                  CompletableFuture<Integer> resultPromise,
                  String containerId) {
        super(clientOptions, linkOptions, connectPromise, resultPromise, containerId);
        this.messageQueue = messages.iterator();
        this.predicate = predicate;
    }

    @Override
    protected String getCurrentState() {
        return "after " + numSent.get() + " messages sent";
    }

    @Override
    protected void sendMessages(ProtonConnection connection, ProtonSender sender) {
        sendNext(connection, sender);
    }

    private void sendNext(ProtonConnection connection, ProtonSender sender) {

        Message message;
        if (messageQueue.hasNext() && (message = messageQueue.next()) != null) {
            if (sender.getQoS().equals(ProtonQoS.AT_MOST_ONCE)) {
                sender.send(message);
                numSent.incrementAndGet();
                if (predicate.test(message)) {
                    resultPromise.complete(numSent.get());
                } else {
                    vertx.runOnContext(id -> sendNext(connection, sender));
                }
            } else {
                sender.send(message, protonDelivery -> {
                    if (protonDelivery.getRemoteState().equals(Accepted.getInstance())) {
                        numSent.incrementAndGet();
                        if (predicate.test(message)) {
                            resultPromise.complete(numSent.get());
                            connection.close();
                        } else {
                            sendNext(connection, sender);
                        }
                    } else {
                        resultPromise.completeExceptionally(new IllegalStateException("Message not accepted (remote state: " + protonDelivery.getRemoteState() + ") after " + numSent.get() + " messages sent"));
                        connectPromise.completeExceptionally(new IllegalStateException("Message not accepted (remote state: " + protonDelivery.getRemoteState() + ") after " + numSent.get() + " messages sent"));
                        connection.close();
                    }
                });
            }
        } else {
            if (predicate.test(null)) {
                resultPromise.complete(numSent.get());
            } else {
                resultPromise.completeExceptionally(new RuntimeException("No more messages to send after + " + numSent.get() + " messages sent"));
                connectPromise.completeExceptionally(new RuntimeException("No more messages to send after + " + numSent.get() + " messages sent"));
            }
            connection.close();
        }
    }
}
