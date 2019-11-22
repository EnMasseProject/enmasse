/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.metrics.api.MetricsFormatter;
import io.enmasse.metrics.api.PrometheusMetricsFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class HTTPServer {
    private final HttpServer server;

    public HTTPServer(int port, Metrics metrics) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/healthz", new HealthHandler());
        server.createContext("/metrics", new MetricsHandler(metrics));
        server.setExecutor(null); // creates a default executor
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private static class HealthHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            byte [] response = "OK".getBytes(StandardCharsets.UTF_8);
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    private static class MetricsHandler implements HttpHandler {
        private final Metrics metrics;
        private static final MetricsFormatter metricsFormatter = new PrometheusMetricsFormatter();

        private MetricsHandler(Metrics metrics) {
            this.metrics = metrics;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            byte [] response = metricsFormatter.format(metrics.getMetrics(), System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().add("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }
}
