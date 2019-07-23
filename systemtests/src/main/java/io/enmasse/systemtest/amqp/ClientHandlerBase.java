/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.amqp;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import io.vertx.core.AbstractVerticle;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public abstract class ClientHandlerBase<T> extends AbstractVerticle {

    private static final Logger log = CustomLogger.getLogger();
    protected final AmqpConnectOptions clientOptions;
    protected final LinkOptions linkOptions;
    private final String containerId;
    private static final Symbol unauthorizedAccess = Symbol.getSymbol("amqp:unauthorized-access");
    protected final CompletableFuture<Void> connectPromise;
    protected final CompletableFuture<T> resultPromise;

    public ClientHandlerBase(AmqpConnectOptions clientOptions, LinkOptions linkOptions, CompletableFuture<Void> connectPromise, CompletableFuture<T> resultPromise, String containerId) {
        this.clientOptions = clientOptions;
        this.linkOptions = linkOptions;
        this.connectPromise = connectPromise;
        this.resultPromise = resultPromise;
        this.containerId = containerId;
    }

    @Override
    public void start() {
        log.info("Starting verticle: {}", this);

        ProtonClient client = ProtonClient.create(vertx);
        Endpoint endpoint = clientOptions.getEndpoint();
        client.connect(clientOptions.getProtonClientOptions(), endpoint.getHost(), endpoint.getPort(), clientOptions.getUsername(), clientOptions.getPassword(), connection -> {
            if (connection.succeeded()) {
                ProtonConnection conn = connection.result();
                conn.setContainer(containerId);
                conn.openHandler(result -> {
                    if (result.failed()) {
                        conn.close();
                        resultPromise.completeExceptionally(result.cause());
                        connectPromise.completeExceptionally(result.cause());
                    } else {
                        connectionOpened(conn);
                    }
                });
                conn.closeHandler(result -> {
                    if (result.failed()) {
                        conn.close();
                        resultPromise.completeExceptionally(result.cause());
                    } else {
                        connectionClosed(conn);
                    }
                });
                conn.disconnectHandler(result -> connectionDisconnected(conn));
                conn.open();
            } else {
                log.info("Connection to " + endpoint.getHost() + ":" + endpoint.getPort() + " failed: " + connection.cause().getMessage());
                resultPromise.completeExceptionally(connection.cause());
                connectPromise.completeExceptionally(connection.cause());
            }
        });
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping verticle: {}", this);
    }

    protected abstract void connectionOpened(ProtonConnection conn);

    protected abstract void connectionClosed(ProtonConnection conn);

    protected abstract void connectionDisconnected(ProtonConnection conn);

    protected void handleError(ProtonConnection connection, ErrorCondition error) {
        if (error == null || error.getCondition() == null) {
            log.info("{}: link closed without error", containerId);
        } else {
            log.info("{}: link closed with error {}", containerId, error);
            connection.close();
            if (unauthorizedAccess.equals(error.getCondition()) || error.getDescription().contains("not authorized")) {
                resultPromise.completeExceptionally(new UnauthorizedAccessException(error.getDescription()));
                connectPromise.completeExceptionally(new UnauthorizedAccessException(error.getDescription()));
            } else {
                resultPromise.completeExceptionally(new RuntimeException(error.getDescription()));
                connectPromise.completeExceptionally(new RuntimeException(error.getDescription()));
            }
        }
    }
}
