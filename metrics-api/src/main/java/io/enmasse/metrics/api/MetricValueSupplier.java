/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import java.util.List;
import java.util.function.Supplier;

public interface MetricValueSupplier extends Supplier<List<MetricValue>> {
}
