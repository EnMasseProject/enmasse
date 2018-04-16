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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

public class Receiver extends ClientHandlerBase<List<Message>> {

    private static Logger log = CustomLogger.getLogger();
    private final List<Message> messages = new ArrayList<>();
    private final Predicate<Message> done;
    private final CountDownLatch connectLatch;

    public Receiver(AmqpConnectOptions clientOptions, Predicate<Message> done, CompletableFuture<List<Message>> promise, LinkOptions linkOptions, CountDownLatch connectLatch) {
        super(clientOptions, linkOptions, promise);
        this.done = done;
        this.connectLatch = connectLatch;
    }

    @Override
    protected void connectionOpened(ProtonConnection conn) {
        connectionOpened(conn, linkOptions.getLinkName().orElse(linkOptions.getSource().getAddress()), linkOptions.getSource());
    }

    private void connectionOpened(ProtonConnection conn, String linkName, Source source) {
        ProtonReceiver receiver = conn.createReceiver(source.getAddress(), new ProtonLinkOptions().setLinkName(linkName));
        receiver.setSource(source);
        receiver.setPrefetch(0);
        receiver.handler((protonDelivery, message) -> {
            messages.add(message);
            protonDelivery.disposition(Accepted.getInstance(), true);
            if (done.test(message)) {
                promise.complete(messages);
                conn.close();
            } else {
                receiver.flow(1);
            }
        });
        receiver.openHandler(result -> {
            log.info("Receiver link '" + source.getAddress() + "' opened, granting credits");
            receiver.flow(1);
            connectLatch.countDown();
        });

        receiver.closeHandler(closed -> {
            if (receiver.getRemoteCondition() != null && LinkError.REDIRECT.equals(receiver.getRemoteCondition().getCondition())) {
                String relocated = (String) receiver.getRemoteCondition().getInfo().get("address");
                log.info("Receiver link redirected to '" + relocated + "'");
                Source newSource = linkOptions.getSource();
                newSource.setAddress(relocated);
                String newLinkName = linkOptions.getLinkName().orElse(newSource.getAddress());

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
        if (!promise.isDone()) {
            promise.completeExceptionally(new RuntimeException("Connection closed (" + messages.size() + " messages received"));
        }
    }

    @Override
    protected void connectionDisconnected(ProtonConnection conn) {
        conn.close();
        if (!promise.isDone()) {
            promise.completeExceptionally(new RuntimeException("Connection disconnected (" + messages.size() + " messages received"));
        }
    }
}
