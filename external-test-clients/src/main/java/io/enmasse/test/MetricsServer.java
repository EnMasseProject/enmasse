/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.test;

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

public class MetricsServer {
    private final HttpServer server;

    public MetricsServer(int port, Metrics metrics) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/metrics", new MetricsHandler(metrics));
        server.setExecutor(null);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
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
