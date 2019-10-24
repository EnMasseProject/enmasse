/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.server;

import io.enmasse.metrics.api.HistogramMetric;
import io.enmasse.metrics.api.MetricLabel;
import io.enmasse.metrics.api.MetricType;
import io.enmasse.metrics.api.MetricValue;
import io.enmasse.metrics.api.MetricValueSupplier;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.metrics.api.ScalarMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RequestLogger implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLogger.class);
    private static final String PROP_NAME = "request.start";

    private static final List<String> methods = List.of("POST", "PUT", "GET", "DELETE", "PATCH");
    private static final List<String> services = List.of("addressspaces", "addresses", "messagingusers", "other");

    // Assume status code above 599 is not used
    private static final int MAX_CODE = 600;

    // Buckets:
    // 0-9 ms
    // 10-99 ms
    // 100-999 ms
    // 1000-9999 ms
    // 10000-+Inf ms
    private static final int NUM_BUCKETS = 5;
    private static final Long latencyBucketLimits[] = new Long[] {
            10L,
            100L,
            1000L,
            10000L,
            null};

    private final Map<String, Map<String, ServiceMetrics>> serviceMetrics = new HashMap<>();

    private static class ServiceMetrics {
        private final String service;
        private final String method;
        private final AtomicLongArray statusCodeCounter = new AtomicLongArray(MAX_CODE);
        private final AtomicLong latencySum = new AtomicLong(0);
        private final AtomicLongArray latencyBucketsCount = new AtomicLongArray(NUM_BUCKETS);

        private ServiceMetrics(String service, String method) {
            this.service = service;
            this.method = method;
        }

        public void update(int status, long latencyMs) {
            if (status < 0 || status >= MAX_CODE) {
                return;
            }

            statusCodeCounter.incrementAndGet(status);
            latencySum.addAndGet(latencyMs);

            for (int i = 0; i < NUM_BUCKETS; i++) {
                if (latencyBucketLimits[i] == null || latencyMs < latencyBucketLimits[i]) {
                    latencyBucketsCount.incrementAndGet(i);
                }
            }
        }
    }

    public RequestLogger(Metrics metrics) {
        for (String service : services) {
            serviceMetrics.put(service, new HashMap<>());
            for (String method : methods) {
                serviceMetrics.get(service).put(method, new ServiceMetrics(service, method));
            }
        }
        List<ServiceMetrics> list = serviceMetrics.values().stream()
                .flatMap(m -> m.values().stream())
                .collect(Collectors.toList());
        metrics.registerMetric(new ScalarMetric(
                "http_requests_total",
                "The number of HTTP requests",
                MetricType.counter,
                () -> {
                    List<MetricValue> values = new ArrayList<>();
                    for (ServiceMetrics service : list) {
                        for (int i = 0; i < MAX_CODE; i++) {
                            long count = service.statusCodeCounter.get(i);
                            if (count > 0) {
                                values.add(new MetricValue(count,
                                        new MetricLabel("status", String.format("%3d", i)),
                                        new MetricLabel("service", service.service),
                                        new MetricLabel("method", service.method)));
                            }
                        }
                    }
                    return values;
                }));

        metrics.registerMetric(new HistogramMetric(
                "api_request_duration_milliseconds",
                "The request duration of HTTP requests in milliseconds",
                MetricType.histogram,
                () -> {
                    List<MetricValue> values = new ArrayList<>();
                    for (ServiceMetrics service : list) {
                        values.add(new MetricValue(service.latencySum.get(),
                                new MetricLabel("service", service.service),
                                new MetricLabel("method", service.method)));
                    }
                    return values;
                },
                () -> {
                    List<MetricValue> values = new ArrayList<>();
                    for (ServiceMetrics service : list) {
                        values.add(new MetricValue(service.latencyBucketsCount.get(NUM_BUCKETS - 1),
                                    new MetricLabel("service", service.service),
                                    new MetricLabel("method", service.method)));
                    }
                    return values;
                },
                IntStream.range(0, NUM_BUCKETS)
                        .mapToObj(bucketIdx -> (MetricValueSupplier) () -> {
                            List<MetricValue> values = new ArrayList<>();
                            for (ServiceMetrics service : list) {
                                Number limit = latencyBucketLimits[bucketIdx];
                                values.add(new MetricValue(service.latencyBucketsCount.get(bucketIdx),
                                        new MetricLabel("le", limit == null ? "+Inf" : String.valueOf(limit)),
                                        new MetricLabel("service", service.service),
                                        new MetricLabel("method", service.method)));
                            }
                            return values;
                        }).collect(Collectors.toList())));
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        containerRequestContext.setProperty(PROP_NAME, System.currentTimeMillis());
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        long start = (long) containerRequestContext.getProperty(PROP_NAME);
        long end = System.currentTimeMillis();
        int status = containerResponseContext.getStatus();
        long latencyMs = end - start;

        log.info("{} {} {} ({} ms)", containerRequestContext.getMethod(), containerRequestContext.getUriInfo().getPath(), status, latencyMs);
        updateMetrics(containerRequestContext.getMethod(), containerRequestContext.getUriInfo().getPath(), status, latencyMs);
    }

    private void updateMetrics(String method, String path, int status, long latencyMs) {
        String service = detectServiceFromPath(path);
        if (!methods.contains(method) || !services.contains(service)) {
            return;
        }

        serviceMetrics.get(service).get(method).update(status, latencyMs);
    }

    private String detectServiceFromPath(String path) {
        for (String service : services) {
            if (path.contains(service)) {
                return service;
            }
        }
        return "other";
    }
}
