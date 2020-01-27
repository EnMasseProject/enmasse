/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

public interface Metric {
    String getName();
    String getDescription();
    MetricType getType();
    MetricSnapshot getSnapshot();
}
