/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.amqp;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * A simple client for doing request-response over AMQP.
 */
public class ProtonRequestClient implements SyncRequestClient {
    private static final Logger log = LoggerFactory.getLogger(ProtonRequestClient.class);
    private final Vertx vertx;
    private final int maxRetries;
    private final String containerId;
    private final BlockingQueue<Message> replies = new LinkedBlockingQueue<>();
    private Context context;
    private ProtonConnection connection;
    private ProtonSender sender;
    private ProtonReceiver receiver;
    private String replyTo;

    public ProtonRequestClient(Vertx vertx, String containerId) {
        this(vertx, containerId, 0);
    }

    public ProtonRequestClient(Vertx vertx, String containerId, int maxRetries) {
        this.vertx = vertx;
        this.containerId = containerId;
        this.maxRetries = maxRetries;
    }

    public String getRemoteContainer() {
        return connection.getRemoteContainer();
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void connect(String host, int port, CompletableFuture<Void> promise) {
        connect(host, port, new ProtonClientOptions(), null, promise);
    }

    public void connect(String host, int port, ProtonClientOptions clientOptions, String address, CompletableFuture<Void> promise) {
        if (connection != null) {
            log.debug("Already connected");
            promise.complete(null);
            return;
        }
        ProtonClient client = ProtonClient.create(vertx);
        log.debug("Connecting to {}:{}", host, port);
        client.connect(clientOptions, host, port, result -> {
            if (result.succeeded()) {
                log.debug("Connected to {}:{}", host, port);
                connection = result.result();
                connection.setContainer(containerId);
                createSender(vertx, address, promise, 0);
                connection.open();
            } else {
                log.info("Connection to {}:{} failed", host, port, result.cause());
                promise.completeExceptionally(result.cause());
            }
        });
    }

    private void createSender(Vertx vertx, String address, CompletableFuture<Void> promise, int retries) {

        sender = connection.createSender(address);
        sender.openHandler(result -> {
            if (result.succeeded()) {
                createReceiver(vertx, address, promise, 0);
            } else {
                if (retries > maxRetries) {
                    promise.completeExceptionally(result.cause());
                } else {
                    log.info("Error creating sender, retries = {}", retries);
                    vertx.setTimer(1000, id -> createSender(vertx, address, promise, retries + 1));
                }
            }
        });
        sender.open();
    }

    private void createReceiver(Vertx vertx, String address, CompletableFuture<Void> promise, int retries) {
        receiver = connection.createReceiver(address);
        Source source = new Source();
        source.setDynamic(true);
        receiver.setSource(source);
        receiver.openHandler(h -> {
            if (h.succeeded()) {
                context = vertx.getOrCreateContext();
                replyTo = receiver.getRemoteSource().getAddress();
                promise.complete(null);
            } else {
                if (retries > maxRetries) {
                    promise.completeExceptionally(h.cause());
                } else {
                    log.info("Error creating receiver, retries = {}", retries);
                    vertx.setTimer(1000, id -> createReceiver(vertx, address, promise, retries + 1));
                }
            }
        });
        receiver.handler(((protonDelivery, message) -> {
            try {
                replies.put(message);
                ProtonHelper.accepted(protonDelivery, true);
            } catch (Exception e) {
                ProtonHelper.rejected(protonDelivery, true);
            }
        }));
        receiver.open();
    }

    public Message request(Message message, long timeout, TimeUnit timeUnit) {
        Map<String, Object> properties = new HashMap<>();
        if (message.getApplicationProperties() != null) {
            properties.putAll(message.getApplicationProperties().getValue());
        }
        message.setApplicationProperties(new ApplicationProperties(properties));

        if (message.getReplyTo() == null) {
            message.setReplyTo(replyTo);
        }
        context.runOnContext(h -> sender.send(message));
        try {
            return replies.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (context != null) {
            CompletableFuture.allOf(
                    runOnContext(context, () -> {
                        if (sender != null) {
                            sender.close();
                        }
                    }),
                    runOnContext(context, () -> {
                        if (receiver != null) {
                            receiver.close();
                        }
                    }),
                    runOnContext(context, () -> {
                        if (connection != null) {
                            connection.close();
                        }
                    })).get(1, TimeUnit.MINUTES);
        }
        sender = null;
        receiver = null;
        connection = null;
        replies.clear();
        replyTo = null;
        context = null;
    }

    private static CompletableFuture<Void> runOnContext(Context context, Runnable runnable) {
        CompletableFuture<Void> promise = new CompletableFuture<>();
        context.runOnContext(h -> {
            try {
                runnable.run();
                promise.complete(null);
            } catch (Exception e) {
                promise.completeExceptionally(e);
            }
        });
        return promise;
    }
}
