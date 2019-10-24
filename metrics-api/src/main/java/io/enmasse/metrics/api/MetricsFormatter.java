/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import java.util.Collection;

public interface MetricsFormatter {
    String format(Collection<Metric> metricList, long timestamp);
}
