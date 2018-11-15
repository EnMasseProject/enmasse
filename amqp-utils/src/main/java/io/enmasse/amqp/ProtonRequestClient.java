/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.amqp;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.reactor.Reactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * A simple client for doing request-response over AMQP.
 */
public class ProtonRequestClient implements SyncRequestClient, AutoCloseable, Runnable {
    private static final Logger log = LoggerFactory.getLogger(ProtonRequestClient.class);
    private String replyTo;
    private String remoteContainer;
    private ReactorClient client;
    private Reactor reactor;
    private final Thread thread = new Thread(this);
    private final BlockingQueue<Message> toSend = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> replies = new LinkedBlockingQueue<>();
    private volatile boolean running = false;

    public String getRemoteContainer() {
        return remoteContainer;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void connect(String host, int port, CompletableFuture<Void> promise) {
        connect(host, port, new ProtonRequestClientOptions(), null, promise);
    }

    public void connect(String host, int port, ProtonRequestClientOptions clientOptions, String address, CompletableFuture<Void> promise) {
        if (client != null) {
            log.info("Already connected");
            promise.complete(null);
            return;
        }

        client = new ReactorClient(host, port, clientOptions, address, new ClientHandler() {
            @Override
            public void onReceiverAttached(String remoteContainer, String replyTo) {
                ProtonRequestClient.this.remoteContainer = remoteContainer;
                ProtonRequestClient.this.replyTo = replyTo;
                promise.complete(null);
            }


            @Override
            public void onMessage(Message message, Delivery delivery) {
                try {
                    replies.put(message);
                    delivery.disposition(new Accepted());
                } catch (Exception e) {
                    log.error("Error handling client reply", e);
                    delivery.disposition(new Rejected());
                }
                delivery.settle();
            }

            @Override
            public void onTransportError(ErrorCondition condition) {
                if (!promise.isDone()) {
                    log.error("Transport error: {}", condition);
                    promise.completeExceptionally(new RuntimeException(condition.getDescription()));
                }
            }
        });

        try {
            reactor = Proton.reactor(client);
            running = true;
            thread.start();
        } catch (IOException e) {
            log.error("Error connecting client", e);
        }

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
        try {
            toSend.put(message);
            return replies.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        running = false;
    }

    @Override
    public void run() {
        reactor.setTimeout(3141);
        reactor.start();
        while (reactor.process()) {
            if (!toSend.isEmpty()) {
                Message message = toSend.remove();
                if (message != null) {
                    client.sendMessage(message);
                }
            }
            if (!running) {
                client.close();
            }
        }
        reactor.stop();
        reactor.free();
    }
}
