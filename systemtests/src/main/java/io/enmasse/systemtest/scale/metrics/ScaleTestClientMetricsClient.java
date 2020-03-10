/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.hawkular.agent.prometheus.PrometheusDataFormat;
import org.hawkular.agent.prometheus.PrometheusScraper;
import org.hawkular.agent.prometheus.types.Counter;
import org.hawkular.agent.prometheus.types.MetricFamily;
import org.slf4j.Logger;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.logs.CustomLogger;

public class ScaleTestClientMetricsClient {

    private final static Logger log = CustomLogger.getLogger();

    private static final long METRICS_UPDATE_PERIOD_MILLIS = 5000;

    private PrometheusScraper scraper;

    private long lastUpdate;
    private List<MetricFamily> lastMetrics;

    protected ScaleTestClientMetricsClient(Endpoint metricsEndpoint) throws IOException {
        var url = new URL("http", metricsEndpoint.getHost(), metricsEndpoint.getPort(), "/metrics");
        log.info("Scrapping from {}", url);
        scraper = new PrometheusScraper(url, PrometheusDataFormat.TEXT);
    }

    private void scrape() throws IOException {
        lastMetrics = scraper.scrape();
        if (lastMetrics == null) {
            log.warn("Scraper returned null metrics");
            lastMetrics = new ArrayList<>();
        }
        lastUpdate = System.currentTimeMillis();
    }

    protected List<MetricFamily> getMetrics() throws IOException {
        boolean scrape = false;
        if (lastMetrics == null) {
            log.info("Initializing metrics");
            scrape = true;
        }
        if (!scrape && (System.currentTimeMillis()-METRICS_UPDATE_PERIOD_MILLIS) > lastUpdate) {
            log.info("Metrics update period elapsed, scraping metrics");
            scrape = true;
        }
        if (scrape) {
            scrape();
        }
        return lastMetrics;
    }

    protected IllegalStateException metricNotFound(String name) {
        return new IllegalStateException("Metric " + name + " not found");
    }

    protected Counter getCounter(String name) throws IOException {
        return (Counter) getMetrics()
                .stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> metricNotFound(name))
                .getMetrics()
                .stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> metricNotFound(name));
    }

}
