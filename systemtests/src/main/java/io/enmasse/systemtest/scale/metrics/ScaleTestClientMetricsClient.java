/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hawkular.agent.prometheus.PrometheusDataFormat;
import org.hawkular.agent.prometheus.PrometheusScraper;
import org.hawkular.agent.prometheus.types.Counter;
import org.hawkular.agent.prometheus.types.Histogram;
import org.hawkular.agent.prometheus.types.MetricFamily;
import org.slf4j.Logger;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;

public abstract class ScaleTestClientMetricsClient {

    private final static Logger log = CustomLogger.getLogger();

    private static final int SCRAPE_RETRIES = 2;
    public static final long METRICS_UPDATE_PERIOD_MILLIS = 11000;

    private PrometheusScraper scraper;

    private long lastUpdate;
    private List<MetricFamily> lastMetrics;
    private AtomicBoolean failedScrape = new AtomicBoolean(false);

    protected ScaleTestClientMetricsClient(Endpoint metricsEndpoint) throws IOException {
        var url = new URL("http", metricsEndpoint.getHost(), metricsEndpoint.getPort(), "/metrics");
        log.debug("Scrapping from {}", url);
        scraper = new PrometheusScraper(url, PrometheusDataFormat.TEXT);
    }

    protected void afterScrape(List<MetricFamily> metrics) {
        //do nothing
    }

    private void scrape() throws IOException {
        lastMetrics = scraper.scrape();
        if (lastMetrics == null) {
            log.warn("Scraper returned null metrics");
            lastMetrics = new ArrayList<>();
        }
        lastUpdate = System.currentTimeMillis();
        afterScrape(lastMetrics);
    }

    protected List<MetricFamily> getMetrics() {
        int retries = SCRAPE_RETRIES;
        IOException error = null;
        do {
            try {
                boolean scrape = false;
                if (lastMetrics == null) {
                    log.debug("Initializing metrics");
                    scrape = true;
                }
                if (!scrape && (System.currentTimeMillis()-METRICS_UPDATE_PERIOD_MILLIS) > lastUpdate) {
                    log.debug("Metrics update period elapsed, scraping metrics");
                    scrape = true;
                }
                if (scrape) {
                    scrape();
                }
                return lastMetrics;
            } catch (IOException e) {
                retries--;
                error = e;
                log.warn("Scraping failed {}", e.getMessage());
            }
        } while(retries > 0);
        if (failedScrape.get()) {
            throw new UncheckedIOException(error);
        } else {
            log.warn("Returning previous metrics after scraping failure");
            failedScrape.set(true);
            return lastMetrics;
        }
    }

    protected IllegalStateException metricNotFound(String name) {
        return new IllegalStateException("Metric " + name + " not found");
    }

    protected Counter getCounter(String name) {
        return getCounter(name, null, null).orElseThrow(() -> metricNotFound(name));
    }

    protected Optional<Counter> getCounter(String name, AddressType addressType) {
        return getCounter(name, "addressType", addressType.toString());
    }

    protected Optional<Counter> getCounter(String name, String label, String labelValue) {
        return getCounter(getMetrics(), name, label, labelValue);
    }


    protected Optional<Histogram> getHistogram(String name) {
        return getMetrics()
                .stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> metricNotFound(name))
                .getMetrics()
                .stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .map(h -> (Histogram)h);
    }

    protected Optional<Counter> getCounter(List<MetricFamily> metrics, String name, String label, String labelValue) {
        var stream = metrics
                .stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> metricNotFound(name))
                .getMetrics()
                .stream()
                .filter(m -> m.getName().equals(name));
        if (label != null && labelValue != null) {
            stream = stream.filter(m -> m.getLabels().containsKey(label) && m.getLabels().containsValue(labelValue));
        }
        return stream.findFirst().map(m -> (Counter)m);
    }

}