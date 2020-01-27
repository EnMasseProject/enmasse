/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.authservice.none;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

public class HealthServer extends AbstractVerticle  {
    private static final Logger log = LoggerFactory.getLogger(HealthServer.class);
    private final HttpServerOptions serverOptions;
    public HealthServer(HttpServerOptions serverOptions) {
        this.serverOptions = serverOptions;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        HttpServer httpServer = vertx.createHttpServer(serverOptions);
        httpServer.requestHandler(httpServerRequest -> httpServerRequest.response().end("OK"));
        httpServer.listen(result -> {
            if (result.succeeded()) {
                log.info("HealthServer started");
                startPromise.complete();
            } else {
                log.error("Error starting HealthServer");
                startPromise.fail(result.cause());
            }
        });

    }
}
