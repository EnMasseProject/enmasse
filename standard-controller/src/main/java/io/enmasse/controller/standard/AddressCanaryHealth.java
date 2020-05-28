/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.metrics.api.MetricLabel;
import io.enmasse.metrics.api.MetricType;
import io.enmasse.metrics.api.MetricValue;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.metrics.api.ScalarMetric;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AddressCanaryHealth implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(AddressCanaryHealth.class);

    private final Kubernetes kubernetes;
    private final AddressProber runner;

    private volatile boolean running = false;
    private Thread thread;
    private final Duration checkInterval;

    private volatile List<AddressCanaryResult> latestResult = Collections.emptyList();
    private final AtomicInteger healthCheckFailures = new AtomicInteger(0);

    AddressCanaryHealth(Kubernetes kubernetes, Duration checkInterval, AddressProber runner, Metrics metrics)
    {
        this.kubernetes = kubernetes;
        this.checkInterval = checkInterval;
        this.runner = runner;
        registerMetrics(metrics);
    }

    private void registerMetrics(Metrics metrics) {
        metrics.registerMetric(new ScalarMetric(
                "address_canary_health_failures_total",
                "Total number of health check failures due to failure to send and receive messages to probe addresses.",
                MetricType.gauge,
                () -> latestResult.stream().flatMap(
                        result -> {
                            List<MetricValue> values = new ArrayList<>();
                            values.add(new MetricValue(result.getFailed().size(), new MetricLabel("router", result.getRouterId())));
                            for (String failed : result.getFailed()) {
                                values.add(new MetricValue(1, new MetricLabel("router", result.getRouterId()), new MetricLabel("address", failed)));
                            }

                            for (String passed : result.getPassed()) {
                                values.add(new MetricValue(0, new MetricLabel("router", result.getRouterId()), new MetricLabel("address", passed)));
                            }
                            return values.stream();
                        }).collect(Collectors.toList())));

        metrics.registerMetric(new ScalarMetric(
                "address_canary_health_check_failures_total",
                "Total number of attempted health check runs that failed due to controller errors.",
                MetricType.counter,
                () -> Collections.singletonList(new MetricValue(healthCheckFailures.get()))));
    }

    void checkHealth(List<BrokerCluster> brokerClusters) {
        List<Pod> routers = new ArrayList<>(kubernetes.listRouters());

        List<Pod> brokers = brokerClusters.stream()
                .flatMap(cluster -> kubernetes.listBrokers(cluster.getClusterId()).stream())
                .collect(Collectors.toList());

        // Check if we can send/receive to all brokers through all routers
        Set<String> addresses = new HashSet<>();
        addresses.add("!!HEALTH_CHECK_ROUTER");
        for (Pod broker : brokers) {
            addresses.add(String.format("!!HEALTH_CHECK_BROKER_%s", broker.getMetadata().getName()));
        }

        List<AddressCanaryResult> addressCanaryResults = new ArrayList<>(routers.size());
        for (Pod router : routers) {
            try {
                log.debug("[route {}] Running health check against {}:55671 for addresses {}", router.getMetadata().getName(), router.getStatus().getPodIP(), addresses);
                Set<String> passed = runner.run(router.getStatus().getPodIP(), 55671, addresses);
                Set<String> failed = new HashSet<>(addresses);
                failed.removeAll(passed);
                log.debug("[router {}] Passed: {}. Failed: {}", router.getMetadata().getName(), passed, failed);
                addressCanaryResults.add(new AddressCanaryResult(router.getMetadata().getName(), passed, failed));
            } catch (Exception e) {
                log.warn("Error checking address health", e);
                healthCheckFailures.incrementAndGet();
            }
        }
        this.latestResult = addressCanaryResults;
    }

    public void start() {
        running = true;
        thread = new Thread(this);
        thread.setName("router-status-collector");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        try {
            running = false;
            thread.interrupt();
            thread.join();
        } catch (InterruptedException ignored) {
            log.warn("Interrupted while stopping", ignored);
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(checkInterval.toMillis());
                checkHealth(kubernetes.listClusters());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("Exception in collector task", e);
            }
        }
    }
}
