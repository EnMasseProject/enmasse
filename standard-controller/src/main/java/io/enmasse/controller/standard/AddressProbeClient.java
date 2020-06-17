/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AddressProbeClient implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AddressProbeClient.class);

    private final Vertx vertx;
    private final String containerId;

    private Context context;
    private ProtonConnection connection;
    private final List<ProtonSender> senders = new ArrayList<>();
    private final List<ProtonReceiver> receivers = new ArrayList<>();

    public AddressProbeClient(Vertx vertx, String containerId) {
        this.vertx = vertx;
        this.containerId = containerId;
    }

    public void connect(String host, int port, ProtonClientOptions clientOptions, CompletableFuture<Void> promise) {
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
                context = vertx.getOrCreateContext();
                connection.setContainer(containerId);
                connection.open();
                promise.complete(null);
            } else {
                log.info("Connection to {}:{} failed", host, port, result.cause());
                promise.completeExceptionally(result.cause());
            }
        });
    }

    public Set<String> probeAddresses(Set<String> addresses, Duration timeout) {
        if (connection == null) {
            throw new RuntimeException("HelathProbeClient not connected");
        }

        String payload = String.format("PING %s", UUID.randomUUID().toString());
        Map<String, Object> properties = Collections.singletonMap("probe", "true");
        Message message = Proton.message();
        message.setBody(new AmqpValue(payload));
        message.setApplicationProperties(new ApplicationProperties(properties));

        Set<String> passed = new ConcurrentHashSet<>();
        CountDownLatch done = new CountDownLatch(addresses.size());

        runOnContext(context, () -> {
            for (String address : addresses) {
                ProtonReceiver receiver = connection.createReceiver(address);
                receiver.handler((delivery, m) -> {
                    String data = (String) ((AmqpValue) m.getBody()).getValue();
                    delivery.disposition(Accepted.getInstance(), true);
                    if (payload.equals(data)) {
                        passed.add(address);
                        done.countDown();
                    }
                });
                receiver.openHandler(protonReceiverAsyncResult -> {
                    if (protonReceiverAsyncResult.succeeded()) {
                        ProtonSender sender = connection.createSender(address);
                        sender.openHandler(protonSenderAsyncResult -> {
                            if (protonSenderAsyncResult.succeeded()) {
                                sender.send(message, protonDelivery -> {
                                    log.debug("Message sent to {}!", address);
                                });
                            }
                        });
                        sender.closeHandler(remoteCloseHandler -> {
                            if (remoteCloseHandler.failed()) {
                                log.warn("Error closing sender for {}: {}", address, sender.getRemoteCondition().getDescription());
                            }
                        });
                        vertx.setTimer(4000, h -> context.runOnContext(p -> {
                            log.debug("Opening sender for {}", address);
                            sender.open();
                            synchronized (this.senders) {
                                this.senders.add(sender);
                            }
                        }));
                    }
                });
                receiver.closeHandler(remoteCloseHandler -> {
                    if (remoteCloseHandler.failed()) {
                        log.warn("Error closing receiver for {}: {}", address, receiver.getRemoteCondition().getDescription());
                    }
                });
                synchronized (this.receivers) {
                    this.receivers.add(receiver);
                }
                log.debug("Opening receiver for {}", address);
                receiver.open();
            }
        });

        try {
            done.await(timeout.getSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        return new HashSet<>(passed);
    }

    @Override
    public void close() throws Exception {
        CountDownLatch latch = new CountDownLatch((connection != null ? 1 : 0) + senders.size() + receivers.size());
        if (context != null) {
            runOnContext(context, () -> {
                synchronized (senders) {
                    for (ProtonSender sender : senders) {
                        if (sender != null) {
                            sender.close();
                        }
                        latch.countDown();
                    }
                }
            });
            runOnContext(context, () -> {
                synchronized (receivers) {
                    for (ProtonReceiver receiver : receivers) {
                        if (receiver != null) {
                            receiver.close();
                        }
                        latch.countDown();
                    }
                }
            });
            runOnContext(context, () -> {
                if (connection != null) {
                    connection.close();
                }
                latch.countDown();
            });
        }
        latch.await(1, TimeUnit.MINUTES);
        senders.clear();
        receivers.clear();
        connection = null;
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
