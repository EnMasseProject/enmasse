/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.address.controller;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.api.v3.ApiHandler;
import enmasse.address.controller.api.v3.amqp.AddressingService;
import enmasse.address.controller.api.v3.amqp.ResponseHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.*;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AMQPServer for triggering deployments through AMQP
 */
public class AMQPServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(AMQPServer.class.getName());
    private final int port;
    private final AddressingService addressingService;
    private final Map<String, HandlerContext> replyHandlers = new ConcurrentHashMap<>();
    private ProtonServer server;

    public AMQPServer(AddressManager addressManager, int port) {
        this.port = port;
        this.addressingService = new AddressingService(new ApiHandler(addressManager));
    }

    public void start() {
        server = ProtonServer.create(vertx);
        server.connectHandler(connection -> {
            connection.setContainer("address-controller");
            connection.openHandler(conn -> {
                log.info("Connection opened");
            }).closeHandler(conn -> {
                closeHandlers(connection);
                connection.close();
                connection.disconnect();
                log.info("Connection closed");
            }).disconnectHandler(protonConnection -> {
                closeHandlers(connection);
                connection.disconnect();
                log.info("Disconnected");
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
            receiver.handler((delivery, message) -> {
                vertx.executeBlocking(future -> {
                    try {
                        onAddressConfig(message);
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
        } else {
            receiver.close();
        }
    }

    private void onAddressConfig(Message message) throws IOException {
        ApplicationProperties properties = message.getApplicationProperties();
        if (properties == null) {
            throw new IllegalArgumentException("Missing message properties");
        }
        Map propertyMap = properties.getValue();

        if (!propertyMap.containsKey("method")) {
            throw new IllegalArgumentException("Property 'method' is missing");
        }
        String method = (String) propertyMap.get("method");

        Optional<ResponseHandler> responseHandler = Optional.ofNullable(replyHandlers.get(message.getReplyTo()))
                .map(context -> (ResponseHandler) response -> vertx.runOnContext(v -> context.sender.send(response)));

        if ("GET".equals(method)) {
            addressingService.handleGet(message, responseHandler);
        } else if ("PUT".equals(method)) {
            addressingService.handlePut(message, responseHandler);
        } else if ("POST".equals(method)) {
            addressingService.handleAppend(message, responseHandler);
        } else if ("DELETE".equals(method)) {
            addressingService.handleDelete(message, responseHandler);
        }
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
}

