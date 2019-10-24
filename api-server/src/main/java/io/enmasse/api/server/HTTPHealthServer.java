/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.server;

import io.enmasse.metrics.api.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.Collections;

public class HTTPHealthServer extends AbstractVerticle {
    public static final int PORT = 8080;
    private static final Logger log = LoggerFactory.getLogger(HTTPHealthServer.class);
    private final Metrics metrics;
    private final int port;
    private HttpServer httpServer;
    private final MetricsFormatter formatter = new PrometheusMetricsFormatter();

    public HTTPHealthServer(String version, Metrics metrics) {
        this.metrics = metrics;
        this.port = PORT;
        metrics.registerMetric(new ScalarMetric(
                "version",
                "The version of the api-server",
                MetricType.gauge,
                () -> Collections.singletonList(new MetricValue(0, new MetricLabel("name", "api-server"), new MetricLabel("version", version)))));
    }

    @Override
    public void start(Future<Void> startPromise) {
        httpServer = vertx.createHttpServer()
            .requestHandler(request -> {
                if (request.path().startsWith("/metrics")) {
                    handleMetrics(request);
                } else {
                    request.response().setStatusCode(200).putHeader("Content-Type", MediaType.TEXT_PLAIN).end("OK");
                }
            }).listen(this.port, ar -> {
                if (ar.succeeded()) {
                    int actualPort = ar.result().actualPort();
                    log.info("Started HTTP server. Listening on port {}", actualPort);
                    startPromise.complete();
                } else {
                    log.info("Error starting HTTP server");
                    startPromise.fail(ar.cause());
                }
            });
    }

    @Override
    public void stop() {
        if (httpServer != null) {
            httpServer.close();
        }
    }

    private void handleMetrics(HttpServerRequest request) {
        String data = formatter.format(metrics.getMetrics(), System.currentTimeMillis());
        request.response().setStatusCode(200).putHeader("Content-Type", MediaType.TEXT_HTML).end(data);
    }
}
