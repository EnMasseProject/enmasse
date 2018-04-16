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
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public abstract class ClientHandlerBase<T> extends AbstractVerticle {

    private static Logger log = CustomLogger.getLogger();
    protected final AmqpConnectOptions clientOptions;
    protected final LinkOptions linkOptions;
    protected final CompletableFuture<T> promise;

    public ClientHandlerBase(AmqpConnectOptions clientOptions, LinkOptions linkOptions, CompletableFuture<T> promise) {
        this.clientOptions = clientOptions;
        this.linkOptions = linkOptions;
        this.promise = promise;
    }

    @Override
    public void start() {
        ProtonClient client = ProtonClient.create(vertx);
        Endpoint endpoint = clientOptions.getEndpoint();
        client.connect(clientOptions.getProtonClientOptions(), endpoint.getHost(), endpoint.getPort(), clientOptions.getUsername(), clientOptions.getPassword(), connection -> {
            if (connection.succeeded()) {
                ProtonConnection conn = connection.result();
                conn.setContainer("enmasse-systemtest-client");
                conn.openHandler(result -> {
                    if (result.failed()) {
                        conn.close();
                        promise.completeExceptionally(result.cause());
                    } else {
                        connectionOpened(conn);
                    }
                });
                conn.closeHandler(result -> {
                    if (result.failed()) {
                        conn.close();
                        promise.completeExceptionally(result.cause());
                    } else {
                        connectionClosed(conn);
                    }
                });
                conn.disconnectHandler(result -> connectionDisconnected(conn));
                conn.open();
            } else {
                log.info("Connection to " + endpoint.getHost() + ":" + endpoint.getPort() + " failed: " + connection.cause().getMessage());
                promise.completeExceptionally(connection.cause());
            }
        });
    }

    protected abstract void connectionOpened(ProtonConnection conn);

    protected abstract void connectionClosed(ProtonConnection conn);

    protected abstract void connectionDisconnected(ProtonConnection conn);

    protected void handleError(ProtonConnection connection, ErrorCondition error) {
        if (error == null || error.getCondition() == null) {
            log.info("Link closed without error");
        } else {
            log.info("Link closed with " + error);
            connection.close();
            promise.completeExceptionally(new RuntimeException(error.getDescription()));
        }
    }
}
