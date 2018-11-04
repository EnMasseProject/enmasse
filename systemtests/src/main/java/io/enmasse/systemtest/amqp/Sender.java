/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.amqp;

import io.enmasse.systemtest.CustomLogger;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

class Sender extends ClientHandlerBase<Integer> {
    private static Logger log = CustomLogger.getLogger();
    private final AtomicInteger numSent = new AtomicInteger(0);
    private final Iterator<Message> messageQueue;
    private final Predicate<Message> predicate;

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
    public void connectionOpened(ProtonConnection connection) {
        ProtonSender sender = connection.createSender(linkOptions.getTarget().getAddress());
        sender.setTarget(linkOptions.getTarget());
        sender.setQoS(clientOptions.getQos());
        sender.openHandler(result -> {
            if (result.succeeded()) {
                log.info("Sender link '" + sender.getTarget().getAddress() + "' opened, sending messages");
                connectPromise.complete(null);
                sendNext(connection, sender);
            } else {
                handleError(connection, sender.getRemoteCondition());
            }
        });
        sender.closeHandler(result -> handleError(connection, sender.getRemoteCondition()));
        sender.open();
    }

    @Override
    protected void connectionClosed(ProtonConnection conn) {
        conn.close();
        resultPromise.completeExceptionally(new RuntimeException("Connection closed after " + numSent.get() + " messages sent"));
        connectPromise.completeExceptionally(new RuntimeException("Connection closed after " + numSent.get() + " messages sent"));
    }

    @Override
    protected void connectionDisconnected(ProtonConnection conn) {
        conn.close();
        resultPromise.completeExceptionally(new RuntimeException("Connection disconnected after " + numSent.get() + " messages sent"));
        connectPromise.completeExceptionally(new RuntimeException("Connection disconnected after " + numSent.get() + " messages sent"));
    }

    private void sendNext(ProtonConnection connection, ProtonSender sender) {

        Message message;
        if (messageQueue.hasNext() && (message = messageQueue.next()) != null) {
            if (sender.getQoS().equals(ProtonQoS.AT_MOST_ONCE)) {
                log.info("Sending message {} (QoS 0), has credit {}", numSent.get(), sender.getCredit());
                sender.send(message);
                numSent.incrementAndGet();
                if (predicate.test(message)) {
                    resultPromise.complete(numSent.get());
                } else {
                    vertx.runOnContext(id -> sendNext(connection, sender));
                }
            } else {
                log.info("Sending message {}, has credit {}", numSent.get(), sender.getCredit());
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
