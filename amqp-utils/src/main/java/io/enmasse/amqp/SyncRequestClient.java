/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.amqp;

import io.vertx.core.Vertx;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A simple client for doing request-response over AMQP.
 */
public class SyncRequestClient implements AutoCloseable {
    private final String host;
    private final int port;
    private final Vertx vertx;
    private final ProtonClientOptions clientOptions;

    public SyncRequestClient(String host, int port) {
        this(host, port, Vertx.vertx());
    }

    public SyncRequestClient(String host, int port, Vertx vertx) {
        this(host, port, vertx, new ProtonClientOptions());
    }

    public SyncRequestClient(String host, int port, Vertx vertx, ProtonClientOptions clientOptions) {
        this.host = host;
        this.port = port;
        this.vertx = vertx;
        this.clientOptions = clientOptions;
    }

    public Message request(Message message, long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException, ExecutionException {
        String address = message.getAddress();
        CompletableFuture<Message> response = new CompletableFuture<>();

        ProtonClient client = ProtonClient.create(vertx);
        client.connect(clientOptions, host, port, connectEvent -> {
            if (connectEvent.succeeded()) {
                ProtonConnection connection = connectEvent.result();
                connection.open();

                ProtonSender sender = connection.createSender(address);
                sender.openHandler(senderOpenEvent -> {
                    if (senderOpenEvent.succeeded()) {
                        ProtonReceiver receiver = connection.createReceiver(address);
                        Source source = new Source();
                        source.setDynamic(true);
                        receiver.setSource(source);
                        receiver.setPrefetch(1);
                        receiver.handler(((delivery, msg) -> {
                            response.complete(msg);

                            receiver.close();
                            sender.close();
                            connection.close();
                        }));

                        receiver.openHandler(receiverOpenEvent -> {
                            if (receiverOpenEvent.succeeded()) {
                                if (receiver.getRemoteSource() != null) {
                                    message.setReplyTo(receiver.getRemoteSource().getAddress());
                                }
                                sender.send(message);
                            } else {
                                response.completeExceptionally(receiverOpenEvent.cause());
                            }
                        });
                        receiver.open();
                    }
                });
                sender.open();
            } else {
                response.completeExceptionally(connectEvent.cause());
            }
        });
        return response.get(timeout, timeUnit);
    }

    @Override
    public void close() throws Exception {
        vertx.close();
    }
}
