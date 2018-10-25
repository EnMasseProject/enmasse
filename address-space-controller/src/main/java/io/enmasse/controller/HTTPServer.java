/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.metrics.api.Metric;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.metrics.api.MetricsFormatter;
import io.enmasse.metrics.api.PrometheusMetricsFormatter;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HTTPServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class);

    private HttpServer server;
    private final int port;
    private final Metrics metrics;
    private final MetricsFormatter metricsFormatter = new PrometheusMetricsFormatter();

    public HTTPServer(int port, Metrics metrics) {
        this.port = port;
        this.metrics = metrics;
    }

    @Override
    public void start(Future<Void> startPromise) {

        server = vertx.createHttpServer();
        server.requestHandler(request -> {
                    if (request.path().equals("/healthz")) {
                        request.response().setStatusCode(HttpResponseStatus.OK.code()).end();
                    } else if (request.path().equals("/metrics")) {
                        request.response()
                                .setStatusCode(HttpResponseStatus.OK.code())
                                .putHeader("content-type", "text/html")
                                .end(buildMetricsResponse());

                    } else {
                        request.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
                    }
                });

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

    private String buildMetricsResponse() {
        List<Metric> metricsSnapshot = metrics.snapshot();
        return metricsFormatter.format(metricsSnapshot);
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
