/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */


package io.enmasse.amqp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonSession;

public class PubSubBroker extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(PubSubBroker.class.getName());

    private volatile ProtonServer server;
    private final String containerId;
    private final Map<String, Queue<Message>> queues = new HashMap<>();
    private final Map<String, SenderInfo> subscribers = new HashMap<>();

    public PubSubBroker(String containerId) {
        this.containerId = containerId;
    }

    private static class SenderInfo {
        final ProtonSender sender;
        final Context senderContext;

        public SenderInfo(ProtonSender sender, Context senderContext) {
            this.sender = sender;
            this.senderContext = senderContext;
        }
    }

    private void connectHandler(ProtonConnection connection) {
        connection.setContainer(containerId);
        connection.openHandler(conn -> {
            log.info("[{}]: Connection opened", containerId);
        }).closeHandler(conn -> {
            connection.close();
            connection.disconnect();
            log.info("[{}]: Connection closed", containerId);
        }).disconnectHandler(protonConnection -> {
            connection.disconnect();
            log.debug("Disconnected");
        }).open();

        connection.sessionOpenHandler(ProtonSession::open);
        connection.senderOpenHandler(sender -> senderOpenHandler(connection, sender));
        connection.receiverOpenHandler(receiver -> receiverOpenHandler(connection, receiver));
    }

    private void receiverOpenHandler(ProtonConnection connection, ProtonReceiver receiver) {
        Target target = (Target) receiver.getRemoteTarget();
        receiver.setTarget(target);
        log.info("[{}]: Got publish request from {} on {}", containerId, connection.getRemoteContainer(), target.getAddress());
        receiver.handler((delivery, message) -> {
            vertx.executeBlocking(promise -> {
                try {
                    synchronized (this) {
                        Queue<Message> queue = queues.get(target.getAddress());
                        queue.add(message);
                        SenderInfo senderInfo = subscribers.get(target.getAddress());
                        if (senderInfo != null) {
                            checkQueue(senderInfo.senderContext, senderInfo.sender);
                        }
                    }
                    promise.complete();
                } catch (Exception e) {
                    promise.fail(e);
                }
            }, result -> {
                if (result.succeeded()) {
                    ProtonHelper.accepted(delivery, true);
                } else {
                    ProtonHelper.rejected(delivery, true);
                }
            });
        });
        vertx.executeBlocking(promise -> {
            try {
                synchronized (this) {
                    Queue<Message> queue = queues.get(target.getAddress());
                    if (queue == null) {
                        queue = new ArrayDeque<>();
                        queues.put(target.getAddress(), queue);
                    }
                }
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                receiver.open();
            } else {
                receiver.close();
                log.info("[{}]: Failed creating publisher {} for address {}", containerId, connection.getRemoteContainer(), receiver.getRemoteTarget().getAddress(), result.cause());
            }

        });
    }

    private void senderOpenHandler(ProtonConnection connection, ProtonSender sender) {
        Source source = (Source) sender.getRemoteSource();
        sender.setSource(source);
        log.info("[{}]: Got subscription request from {} on {}", containerId, connection.getRemoteContainer(), source.getAddress());

        Context senderContext = vertx.getOrCreateContext();
        sender.closeHandler(handle -> {
            vertx.executeBlocking(promise -> {
                synchronized (this) {
                    subscribers.remove(source.getAddress());
                }
                promise.complete();
            }, result -> {
                sender.close();
            });
        });

        vertx.executeBlocking(promise -> {
            try {
                synchronized (this) {
                    if (queues.get(source.getAddress()) == null || subscribers.get(source.getAddress()) != null) {
                        senderContext.runOnContext(id -> sender.close());
                    }
                    subscribers.put(source.getAddress(), new SenderInfo(sender, senderContext));
                    if (queues.get(source.getAddress()) != null) {
                        checkQueue(senderContext, sender);
                    }
                }
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                sender.open();
                log.info("[{}]: Opened sender for subscriber {} for address {}", containerId, connection.getRemoteContainer(), sender.getRemoteSource().getAddress());
            } else {
                sender.close();
                log.info("[{}]: Failed creating subscriber {} for address {}", containerId, connection.getRemoteContainer(), sender.getRemoteSource().getAddress(), result.cause());
            }
        });
    }

    private void checkQueue(Context senderContext, ProtonSender sender) {
        String address = sender.getSource().getAddress();
        log.debug("[{}]: Has {} messages in store on address {}", containerId, queues.get(address).size(), address);
        Message message = queues.get(address).poll();
        if (message != null) {
            senderContext.runOnContext(v -> sender.send(message, delivery -> {
                checkQueue(senderContext, sender);
            }));
        }
    }

    public synchronized List<Message> getMessages(String address) {
        return new ArrayList<>(queues.get(address));
    }

    @Override
    public void start(Promise<Void> startPromise) {
        server = ProtonServer.create(vertx);
        server.connectHandler(this::connectHandler);
        server.listen(0, result -> {
            if (result.succeeded()) {
                log.info("[{}]: Starting server on port {}", containerId, server.actualPort());
                startPromise.complete();
            } else {
                log.error("[{}]: Error starting server", containerId, result.cause());
                startPromise.fail(result.cause());
            }
        });
    }

    public int port() {
        if (server == null) {
            return 0;
        }
        return server.actualPort();
    }

    @Override
    public void stop() {
        if (server != null) {
            server.close();
        }
    }

    public synchronized  int numMessages(String address) {
        return queues.get(address).size();
    }

    public synchronized void sendMessages(String address, List<String> messages) {
        Queue<Message> queue = queues.computeIfAbsent(address, a -> new ArrayDeque<>());
        for (String data : messages) {
            Message message = Proton.message();
            message.setBody(new AmqpValue(data));
            queue.add(message);
        }
    }

    public synchronized List<String> recvMessages(String address, int numMessages) {
        Queue<Message> queue = queues.get(address);
        if (queue == null) {
            return null;
        }
        List<String> messages = new ArrayList<>();
        while (numMessages > 0) {
            Message message = queue.poll();
            if (message == null) {
                throw new RuntimeException("No more messages, " + numMessages + " remains");
            }
            messages.add((String)((AmqpValue)message.getBody()).getValue());
            numMessages--;
        }
        return messages;
    }
}
