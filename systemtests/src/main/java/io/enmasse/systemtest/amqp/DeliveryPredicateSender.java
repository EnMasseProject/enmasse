/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.amqp;

import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.message.Message;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class DeliveryPredicateSender extends AbstractSender<Integer> {
    protected final Iterator<Message> messageQueue;
    protected final Predicate<ProtonDelivery> predicate;
    private final AtomicInteger numSent = new AtomicInteger(0);

    public DeliveryPredicateSender(AmqpConnectOptions clientOptions,
                  LinkOptions linkOptions,
                  Iterable<Message> messages,
                  final Predicate<ProtonDelivery> predicate,
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
            sender.send(message, protonDelivery -> {
                if (predicate.test(protonDelivery)) {
                    resultPromise.complete(numSent.get());
                    connection.close();
                } else {
                    numSent.incrementAndGet();
                    vertx.runOnContext(id -> sendNext(connection, sender));
                }
            });
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
