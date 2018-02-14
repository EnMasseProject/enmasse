/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import io.enmasse.k8s.api.AddressSpaceApi;
import io.vertx.core.AbstractVerticle;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AMQPServer for triggering deployments through AMQP
 */
public class AMQPServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(AMQPServer.class.getName());
    private final int port;
    private final Map<String, HandlerContext> replyHandlers = new ConcurrentHashMap<>();
    private ProtonServer server;

    public AMQPServer(String addressSpaceName, AddressSpaceApi addressSpaceApi, int port) {
        this.port = port;
        /*
        this.addressingService = new AddressingService(addressSpaceId, new AddressApiHelper(addressSpaceApi));
        this.flavorsService = new FlavorsService(repository);
        */
    }

    public void start() {
        server = ProtonServer.create(vertx);
        server.connectHandler(connection -> {
            connection.setContainer("address-controller");
            connection.openHandler(conn -> {
                log.debug("Connection opened");
            }).closeHandler(conn -> {
                closeHandlers(connection);
                connection.close();
                connection.disconnect();
                log.debug("Connection closed");
            }).disconnectHandler(protonConnection -> {
                closeHandlers(connection);
                connection.disconnect();
                log.debug("Disconnected");
            }).open();
            connection.sessionOpenHandler(ProtonSession::open);
            connection.receiverOpenHandler(this::createReceiver);
            connection.senderOpenHandler(sender -> createSender(connection, sender));
        }).listen(port);
    }

    private void closeHandlers(ProtonConnection connection) {
        Collection<HandlerContext> contextList = new ArrayList<>(replyHandlers.values());
        for (HandlerContext context : contextList) {
            if (context.connection == connection) {
                replyHandlers.remove(context.address);
                context.sender.close();
            }
        }
    }

    private void createSender(ProtonConnection connection, ProtonSender sender) {
        Source source = (Source) sender.getRemoteSource();
        if (source.getDynamic()) {
            String replyAddress = UUID.randomUUID().toString();
            replyHandlers.put(replyAddress, new HandlerContext(connection, sender, replyAddress));
            sender.closeHandler(res -> replyHandlers.remove(replyAddress));

            source.setAddress(replyAddress);
            sender.setSource(source);
        }
        sender.open();
    }

    private void createReceiver(ProtonReceiver receiver) {
        String targetAddress = receiver.getRemoteTarget().getAddress();
        if (targetAddress.equals("$address")) {
         //   openReceiverWithHandler(receiver, addressingService::handleMessage);
        } else if (targetAddress.equals("$flavor")) {
          //  openReceiverWithHandler(receiver, flavorsService::handleMessage);
        } else {
            receiver.close();
        }
    }

    private void openReceiverWithHandler(ProtonReceiver receiver, RequestHandler requestHandler) {
        receiver.handler((delivery, message) -> {
            vertx.executeBlocking(future -> {
                try {
                    Optional<HandlerContext> context = Optional.ofNullable(replyHandlers.get(message.getReplyTo()));
                    Message response = requestHandler.handleMessage(message);
                    context.ifPresent(ctx -> vertx.runOnContext(v -> ctx.sender.send(response)));
                } catch (Exception e) {
                    log.warn("Error handling addressing message", e);
                    future.fail(e);
                }
            }, result -> {
                if (result.succeeded()) {
                    delivery.disposition(new Accepted(), true);
                } else {
                    delivery.disposition(new Rejected(), true);
                }
            });
        });
        receiver.open();
    }

    public void stop() {
        if (server != null) {
            server.close();
        }
    }

    public int getPort() {
        if (server != null) {
            return server.actualPort();
        } else {
            return port;
        }
    }

    private static class HandlerContext {
        private final ProtonConnection connection;
        private final ProtonSender sender;
        private final String address;

        private HandlerContext(ProtonConnection connection, ProtonSender sender, String address) {
            this.connection = connection;
            this.sender = sender;
            this.address = address;
        }
    }

    private interface RequestHandler {
        Message handleMessage(Message message) throws Exception;
    }
}

