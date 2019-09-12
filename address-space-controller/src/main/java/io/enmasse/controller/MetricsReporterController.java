/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceStatusConnector;
import io.enmasse.metrics.api.*;

import java.util.ArrayList;
import java.util.List;

public class MetricsReporterController implements Controller {
    private final Metrics metrics;
    private final String version;

    public MetricsReporterController(Metrics metrics, String version) {
        this.metrics = metrics;
        this.version = version;
    }

    public void reconcileAll(List<AddressSpace> addressSpaces) throws Exception {
        long now = System.currentTimeMillis();
        metrics.reportMetric(new Metric(
            "version",
            "The version of the address-space-controller",
            MetricType.gauge,
            new MetricValue(0, now, new MetricLabel("name", "address-space-controller"), new MetricLabel("version", version))));

        List<MetricValue> readyValues = new ArrayList<>();
        List<MetricValue> notReadyValues = new ArrayList<>();
        List<MetricValue> readyConnectorValues = new ArrayList<>();
        List<MetricValue> notReadyConnectorValues = new ArrayList<>();
        List<MetricValue> numConnectors = new ArrayList<>();
        for (AddressSpace addressSpace : addressSpaces) {
            MetricLabel[] labels = new MetricLabel[]{new MetricLabel("name", addressSpace.getMetadata().getName()), new MetricLabel("namespace", addressSpace.getMetadata().getNamespace())};
            readyValues.add(new MetricValue(addressSpace.getStatus().isReady() ? 1 : 0, now, labels));
            notReadyValues.add(new MetricValue(addressSpace.getStatus().isReady() ? 0 : 1, now, labels));
            numConnectors.add(new MetricValue(addressSpace.getStatus().getConnectorStatuses().size(), now, labels));

            for (AddressSpaceStatusConnector connectorStatus : addressSpace.getStatus().getConnectorStatuses()) {

                MetricLabel[] connectorLabels = new MetricLabel[]{new MetricLabel("name", addressSpace.getMetadata().getName()), new MetricLabel("namespace", addressSpace.getMetadata().getNamespace())};
                readyConnectorValues.add(new MetricValue(connectorStatus.isReady() ? 1 : 0, now, connectorLabels));
                notReadyConnectorValues.add(new MetricValue(connectorStatus.isReady() ? 0 : 1, now, connectorLabels));
            }
        }

        metrics.reportMetric(new Metric(
                "address_space_status_ready",
                "Describes whether the address space is in a ready state",
                MetricType.gauge,
                readyValues));
        metrics.reportMetric(new Metric(
                "address_space_status_not_ready",
                "Describes whether the address space is in a not_ready state",
                MetricType.gauge,
                notReadyValues));
        metrics.reportMetric(new Metric(
                "address_spaces_total",
                "Total number of address spaces",
                MetricType.gauge,
                new MetricValue(addressSpaces.size(), now)));
        metrics.reportMetric(new Metric(
                "address_space_connector_status_ready",
                "Describes whether the connector in an address space is in a ready state",
                MetricType.gauge,
                readyConnectorValues));
        metrics.reportMetric(new Metric(
                "address_space_connector_status_not_ready",
                "Describes whether the connector in an address space is in a not_ready state",
                MetricType.gauge,
                notReadyConnectorValues));
        metrics.reportMetric(new Metric(
                "address_space_connectors_total",
                "Total number of connectors of address spaces",
                MetricType.gauge,
                numConnectors));
    }
}
