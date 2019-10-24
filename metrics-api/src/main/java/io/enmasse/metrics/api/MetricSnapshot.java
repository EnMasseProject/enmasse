/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.metrics.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MetricSnapshot {
    private final List<MetricValue> values;
    private final List<MetricValue> sumValues;
    private final List<MetricValue> countValues;

    private MetricSnapshot(List<MetricValue> values, List<MetricValue> sumValues, List<MetricValue> countValues) {
        this.values = new ArrayList<>(values);
        this.sumValues = sumValues;
        this.countValues = countValues;
    }

    public List<MetricValue> getValues() {
        return values;
    }

    public List<MetricValue> getSumValues() {
        return sumValues;
    }

    public List<MetricValue> getCountValues() {
        return countValues;
    }

    public static MetricSnapshot histogram(List<MetricValue> bucketValues, List<MetricValue> sumValues, List<MetricValue> countValues) {
        return new MetricSnapshot(bucketValues, sumValues, countValues);
    }

    public static MetricSnapshot scalar(List<MetricValue> values) {
        return new MetricSnapshot(values, Collections.emptyList(), Collections.emptyList());
    }

}
