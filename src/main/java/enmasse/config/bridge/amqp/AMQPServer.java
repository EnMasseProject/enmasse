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

package enmasse.config.bridge.amqp;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonSession;
import enmasse.config.bridge.model.ConfigMapDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AMQP server endpoint that handles connections to the service and propagates config for a config map specified
 * as the address to which the client wants to receive.
 *
 * TODO: Handle disconnects and unsubscribe
 */
public class AMQPServer {
    private static final Logger log = LoggerFactory.getLogger(AMQPServer.class.getName());

    private final Vertx vertx = Vertx.vertx();
    private final ConfigMapDatabase database;
    private final ProtonServer server;
    private final String hostname;
    private final int port;

    public AMQPServer(String hostname, int port, ConfigMapDatabase database)
    {
        this.hostname = hostname;
        this.port = port;
        this.database = database;
        this.server = ProtonServer.create(vertx);

        server.connectHandler(this::connectHandler);
    }

    private void connectHandler(ProtonConnection connection) {
        connection.setContainer("server");
        connection.openHandler(conn -> {
            log.info("Connection opened");
        }).closeHandler(conn -> {
            connection.close();
            connection.disconnect();
            log.info("Connection closed");
        }).disconnectHandler(protonConnection -> {
            connection.disconnect();
            log.info("Disconnected");
        }).open();

        connection.sessionOpenHandler(ProtonSession::open);
        connection.senderOpenHandler(sender -> senderOpenHandler(connection, sender));
    }

    private void senderOpenHandler(ProtonConnection connection, ProtonSender sender) {
        sender.setSource(sender.getRemoteSource());
        sender.open();
        database.subscribe(sender.getRemoteSource().getAddress(), new AMQPConfigSubscriber(sender));
        log.info("Added subscriber {} for config {}", connection.getRemoteContainer(), sender.getRemoteSource().getAddress());
    }

    public void run() {
        log.info("Starting server on {}:{}", hostname, port);
        server.listen(port, hostname);
    }

    public int port() {
        return server.actualPort();
    }

    public void close() {
        server.close();
        vertx.close();
    }
}
