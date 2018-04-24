/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class);

    private HttpServer server;
    private final int port;

    public HTTPServer(int port) {
        this.port = port;
    }

    @Override
    public void start(Future<Void> startPromise) {
        server = vertx.createHttpServer();
        server.requestHandler(request -> request.response().setStatusCode(HttpResponseStatus.OK.code()).end());
        server.listen(port, result -> {
            if (result.succeeded()) {
                log.info("Started HTTP server listening on {}", port);
                startPromise.complete();
            } else {
                log.info("Failed starting HTTP server: {}", result.cause().getMessage());
                startPromise.fail(result.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopPromise) {
        if (server != null) {
            server.close(stopPromise);
        } else {
            stopPromise.complete();
        }
    }
}
