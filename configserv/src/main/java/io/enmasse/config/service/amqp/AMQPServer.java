/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.config.service.amqp;

import java.util.LinkedHashMap;
import java.util.Map;

import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.config.service.model.ResourceDatabase;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonServerOptions;
import io.vertx.proton.ProtonSession;
import io.vertx.proton.sasl.ProtonSaslAuthenticatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;

/**
 * AMQP server endpoint that handles connections to the service and propagates config for a config map specified
 * as the address to which the client wants to receive.
 *
 * TODO: Handle disconnects and unsubscribe
 */
public class AMQPServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(AMQPServer.class.getName());

    private final Map<String, ResourceDatabase> databaseMap;
    private final String hostname;
    private final int port;
    private volatile ProtonServer server;
    private static final Symbol LABELS = Symbol.getSymbol("labels");
    private static final Symbol ANNOTATIONS = Symbol.getSymbol("annotations");
    private ProtonServerOptions options;
    private ProtonSaslAuthenticatorFactory authenticatorFactory;

    public AMQPServer(String hostname, int port, Map<String, ResourceDatabase> databaseMap) {
        this(hostname, port, databaseMap, new ProtonServerOptions());
    }

    public AMQPServer(String hostname, int port, Map<String, ResourceDatabase> databaseMap, ProtonServerOptions options) {
        this.hostname = hostname;
        this.port = port;
        this.databaseMap = databaseMap;
        this.options = options;
    }

    public void setAuthenticatorFactory(final ProtonSaslAuthenticatorFactory authenticatorFactory) {
        this.authenticatorFactory = authenticatorFactory;
    }

    private void connectHandler(ProtonConnection connection) {
        connection.setContainer("configuration-service");
        connection.openHandler(conn -> {
            log.debug("Connection opened");
        }).closeHandler(conn -> {
            connection.close();
            connection.disconnect();
            log.debug("Connection closed");
        }).disconnectHandler(protonConnection -> {
            connection.disconnect();
            log.debug("Disconnected");
        }).open();

        connection.sessionOpenHandler(ProtonSession::open);
        connection.senderOpenHandler(sender -> senderOpenHandler(connection, sender));
    }

    private void senderOpenHandler(ProtonConnection connection, ProtonSender sender) {
        sender.setSource(sender.getRemoteSource());
        Source source = (Source) sender.getRemoteSource();
        log.info("Got subscription request from {} on {}", connection.getRemoteContainer(), source.getAddress());

        Context protonContext = vertx.getOrCreateContext();

        vertx.executeBlocking(promise -> {
            try {
                ResourceDatabase database = lookupDatabase(source.getAddress());
                Map<String, String> labelFilter = createLabelFilter(source.getFilter());
                Map<String, String> annotationFilter = createAnnotationFilter(source.getFilter());
                ObserverKey subscriptionKey = new ObserverKey(labelFilter, annotationFilter);
                database.subscribe(subscriptionKey, new AMQPSubscriber(protonContext, subscriptionKey, connection, sender));

                promise.complete(database);
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                sender.open();
                log.info("Added subscriber {} for config {}", connection.getRemoteContainer(), sender.getRemoteSource().getAddress());
            } else {
                sender.close();
                connection.close();
                log.info("Failed creating subscriber {} for config {}", connection.getRemoteContainer(), sender.getRemoteSource().getAddress(), result.cause());
            }
        });
    }

    private ResourceDatabase lookupDatabase(String address) {
        if (databaseMap.containsKey(address)) {
            return databaseMap.get(address);
        } else {
            throw new IllegalArgumentException("Unknown database for address " + address);
        }
    }

    private Map<String, String> createLabelFilter(Map filter) {
        Map<String, String> labelFilter = new LinkedHashMap<>();
        if (filter != null) {
            if (filter.containsKey(LABELS)) {
                filter = (Map) filter.get(LABELS);
            }
            for (Object key : filter.keySet()) {
                labelFilter.put(key.toString(), filter.get(key).toString());
            }
        }
        return labelFilter;
    }

    public Map<String, String> createAnnotationFilter(Map filter) {
        Map<String, String> annotationFilter = new LinkedHashMap<>();
        if (filter != null) {
            if (filter.containsKey(ANNOTATIONS)) {
                filter = (Map) filter.get(ANNOTATIONS);
                for (Object key : filter.keySet()) {
                    annotationFilter.put(key.toString(), filter.get(key).toString());
                }
            }
        }
        return annotationFilter;
    }

    @Override
    public void start(Future<Void> startPromise) {
        server = ProtonServer.create(vertx, options);
        server.connectHandler(this::connectHandler);
        if(authenticatorFactory != null) {
            server.saslAuthenticatorFactory(authenticatorFactory);
        }
        server.listen(port, hostname, result -> {
            if (result.succeeded()) {
                log.info("Starting server on {}:{}", hostname, port);
                startPromise.complete();
            } else {
                log.error("Error starting server", result.cause());
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
}
