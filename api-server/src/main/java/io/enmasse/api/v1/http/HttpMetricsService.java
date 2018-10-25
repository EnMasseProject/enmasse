/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.metrics.api.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(HttpMetricsService.BASE_URI)
public class HttpMetricsService {
    public static final String BASE_URI = "/metrics";
    private final String version;
    private final Metrics metrics;
    private final MetricsFormatter formatter = new PrometheusMetricsFormatter();

    public HttpMetricsService(String version, Metrics metrics) {
        this.version = version;
        this.metrics = metrics;
    }

    @GET
    @Produces({MediaType.TEXT_HTML})
    public Response getMetrics() {
        long now = System.currentTimeMillis();
        metrics.reportMetric(new Metric("version", new MetricValue(0, now, new MetricLabel("name", "api-server"), new MetricLabel("version", version))));
        metrics.reportMetric(new Metric("health", new MetricValue(0, now, new MetricLabel("status", "ok"), new MetricLabel("summary", "api-server is healthy"))));
        return Response.ok(formatter.format(metrics.snapshot())).build();
    }
}
