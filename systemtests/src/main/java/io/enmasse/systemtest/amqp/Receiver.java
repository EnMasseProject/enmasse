/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.amqp;

import io.enmasse.systemtest.CustomLogger;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.LinkError;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class Receiver extends ClientHandlerBase<List<Message>> {

    private static final Logger log = CustomLogger.getLogger();
    private final List<Message> messages = new ArrayList<>();
    private final AtomicInteger messageCount = new AtomicInteger();
    private final Predicate<Message> done;

    public Receiver(AmqpConnectOptions clientOptions, Predicate<Message> done, LinkOptions linkOptions, CompletableFuture<Void> connectPromise, CompletableFuture<List<Message>> resultPromise, String containerId) {
        super(clientOptions, linkOptions, connectPromise, resultPromise, containerId);
        this.done = done;
    }

    @Override
    protected void connectionOpened(ProtonConnection conn) {
        connectionOpened(conn, linkOptions.getLinkName().orElse(UUID.randomUUID().toString()), linkOptions.getSource());
    }

    private void connectionOpened(ProtonConnection conn, String linkName, Source source) {
        ProtonReceiver receiver = conn.createReceiver(source.getAddress(), new ProtonLinkOptions().setLinkName(linkName));
        receiver.setSource(source);
        receiver.setPrefetch(0);
        receiver.handler((protonDelivery, message) -> {
            messages.add(message);
            messageCount.incrementAndGet();
            protonDelivery.disposition(Accepted.getInstance(), true);
            if (done.test(message)) {
                resultPromise.complete(messages);
                conn.close();
            } else {
                receiver.flow(1);
            }
        });
        receiver.openHandler(result -> {
            if (result.succeeded()) {
                log.info("Receiver link '{}' opened, granting credits", source.getAddress());
                receiver.flow(1);
                connectPromise.complete(null);
            } else {
                handleError(conn, receiver.getRemoteCondition());
            }
        });

        receiver.closeHandler(closed -> {
            if (receiver.getRemoteCondition() != null && LinkError.REDIRECT.equals(receiver.getRemoteCondition().getCondition())) {
                String relocated = (String) receiver.getRemoteCondition().getInfo().get("address");
                log.info("Receiver link redirected to '{}'", relocated);
                Source newSource = linkOptions.getSource();
                newSource.setAddress(relocated);
                String newLinkName = linkOptions.getLinkName().orElse(UUID.randomUUID().toString());

                vertx.runOnContext(id -> connectionOpened(conn, newLinkName, newSource));
            } else {
                handleError(conn, receiver.getRemoteCondition());
            }
            receiver.close();
        });
        receiver.open();
    }

    @Override
    protected void connectionClosed(ProtonConnection conn) {
        conn.close();
        resultPromise.completeExceptionally(new RuntimeException("Connection closed (" + messages.size() + " messages received"));
        connectPromise.completeExceptionally(new RuntimeException("Connection closed (" + messages.size() + " messages received"));
    }

    @Override
    protected void connectionDisconnected(ProtonConnection conn) {
        conn.close();
        resultPromise.completeExceptionally(new RuntimeException("Connection disconnected (" + messages.size() + " messages received"));
        connectPromise.completeExceptionally(new RuntimeException("Connection disconnected (" + messages.size() + " messages received"));
    }

    int getNumReceived() {
        return messageCount.get();
    }
}
