/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.*;
import io.enmasse.metrics.api.*;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.enmasse.controller.common.ControllerReason.AddressSpaceSyncFailed;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

/**
 * The main controller loop that monitors k8s address spaces
 */
public class ControllerChain implements Watcher<AddressSpace> {
    private static final Logger log = LoggerFactory.getLogger(ControllerChain.class.getName());

    private final Kubernetes kubernetes;
    private final AddressSpaceApi addressSpaceApi;

    private Watch watch;

    private final List<Controller> chain = new ArrayList<>();
    private final SchemaProvider schemaProvider;
    private final EventLogger eventLogger;
    private final Metrics metrics;
    private final String version;
    private final Duration recheckInterval;
    private final Duration resyncInterval;
    private ResourceChecker<AddressSpace> checker;

    public ControllerChain(Kubernetes kubernetes,
                           AddressSpaceApi addressSpaceApi,
                           SchemaProvider schemaProvider,
                           EventLogger eventLogger,
                           Metrics metrics,
                           String version, Duration recheckInterval,
                           Duration resyncInterval) {
        this.kubernetes = kubernetes;
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
        this.eventLogger = eventLogger;
        this.metrics = metrics;
        this.version = version;
        this.recheckInterval = recheckInterval;
        this.resyncInterval = resyncInterval;
    }

    public void addController(Controller controller) {
        chain.add(controller);
    }

    public void start() throws Exception {
        checker = new ResourceChecker<>(this, recheckInterval);
        checker.start();
        this.watch = addressSpaceApi.watchAddressSpaces(checker, resyncInterval);
    }

    public void stop() throws Exception {
        if (watch != null) {
            watch.close();
            watch = null;
        }
        if (checker != null) {
            checker.stop();
            checker = null;
        }
    }

    @Override
    public void onUpdate(List<AddressSpace> resources) throws Exception {
        log.info("Check address spaces: {}", resources.stream().map(a -> a.getNamespace()+":"+a.getName()).collect(Collectors.toSet()));

        if (schemaProvider.getSchema() == null) {
            log.info("No schema available");
            return;
        }

        for (AddressSpace addressSpace : resources) {
            try {
                log.info("Checking address space {}:{}", addressSpace.getNamespace(), addressSpace.getName());
                for (Controller controller : chain) {
                    log.info("Controller {}", controller);
                    log.debug("Address space input: {}", addressSpace);
                    addressSpace = controller.handle(addressSpace);
                }

                log.debug("Controller chain output: {}", addressSpace);

                addressSpaceApi.replaceAddressSpace(addressSpace);
            } catch (KubernetesClientException e) {
                log.warn("Error syncing address space {}", addressSpace.getName(), e);
                eventLogger.log(AddressSpaceSyncFailed, "Error syncing address space: " + e.getMessage(), Warning, ControllerKind.AddressSpace, addressSpace.getName());
            } catch (Exception e) {
                log.warn("Error processing address space {}", addressSpace.getName(), e);
            }
        }
        retainAddressSpaces(resources);

        long now = System.currentTimeMillis();
        metrics.reportMetric(new Metric("version", new MetricValue(0, now, new MetricLabel("name", "address-space-controller"), new MetricLabel("version", version))));
        metrics.reportMetric(new Metric("health", new MetricValue(0, now, new MetricLabel("status", "ok"), new MetricLabel("summary", "address-space-controller is healthy"))));

        List<MetricValue> healthValues = new ArrayList<>();
        for (AddressSpace addressSpace : resources) {
            List<MetricLabel> healthLabels = new ArrayList<>();
            healthLabels.add(new MetricLabel("name", addressSpace.getName()));
            healthLabels.add(new MetricLabel("messages", String.join(",", addressSpace.getStatus().getMessages())));
            healthValues.add(new MetricValue(addressSpace.getStatus().isReady() ? 1 : 0, now, healthLabels));
        }
        metrics.reportMetric(new Metric(
                "address_spaces_ready",
                "Address Spaces Ready",
                MetricType.gauge,
                healthValues));
        metrics.reportMetric(new Metric(
                "address_spaces_total",
                "Total number of address spaces",
                MetricType.gauge,
                new MetricValue(resources.size(), now)));

    }

    private void retainAddressSpaces(List<AddressSpace> desiredAddressSpaces) {
        String [] uuids = desiredAddressSpaces.stream()
                .map(a -> a.getAnnotation(AnnotationKeys.INFRA_UUID))
                .toArray(String[]::new);
        kubernetes.deleteResourcesNotIn(uuids);
    }
}
