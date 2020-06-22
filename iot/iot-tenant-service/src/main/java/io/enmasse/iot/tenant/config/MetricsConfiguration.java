/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import org.eclipse.hono.service.metric.MetricsTags;
import org.eclipse.hono.service.metric.PrometheusScrapingResource;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

@ApplicationScoped
public class MetricsConfiguration {

    @Singleton
    PrometheusMeterRegistry metricsRegistry() {
        var result = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        result
                .config()
                .commonTags(MetricsTags.forService("tenant-service"));
        return result;
    }

    @Singleton
    PrometheusScrapingResource scrapingResource(final PrometheusMeterRegistry registry) {
        return new PrometheusScrapingResource(registry);
    }

}
