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

package enmasse.config.service.amqp;

import enmasse.config.service.model.ResourceDatabase;
import io.vertx.core.AbstractVerticle;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonSession;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AMQP server endpoint that handles connections to the service and propagates config for a config map specified
 * as the address to which the client wants to receive.
 *
 * TODO: Handle disconnects and unsubscribe
 */
public class AMQPServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(AMQPServer.class.getName());

    private final ResourceDatabase database;
    private final String hostname;
    private final int port;
    private volatile ProtonServer server;

    public AMQPServer(String hostname, int port, ResourceDatabase database)
    {
        this.hostname = hostname;
        this.port = port;
        this.database = database;
    }

    private void connectHandler(ProtonConnection connection) {
        connection.setContainer("configuration-service");
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
        Source source = (Source) sender.getRemoteSource();

        boolean success = database.subscribe(source.getAddress(), createStringFilter(source.getFilter()), sender::send);
        if (success) {
            sender.open();
            log.info("Added subscriber {} for config {}", connection.getRemoteContainer(), sender.getRemoteSource().getAddress());
        } else {
            log.info("Failed creating subscriber {} for config {}", connection.getRemoteContainer(), sender.getRemoteSource().getAddress());
            sender.close();
        }
    }

    private Map<String, String> createStringFilter(Map filter) {
        Map<String, String> filterMap = new LinkedHashMap<>();
        if (filter != null) {
            for (Object key : filter.keySet()) {
                filterMap.put(key.toString(), filter.get(key).toString());
            }
        }
        return filterMap;
    }

    @Override
    public void start() {
        server = ProtonServer.create(vertx);
        server.connectHandler(this::connectHandler);
        log.info("Starting server on {}:{}", hostname, port);
        server.listen(port, hostname);
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
}
