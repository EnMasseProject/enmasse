/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;

public class MetricsAssertions<T> implements Predicate<T> {

    private T metric;
    private Predicate<T> predicate;

    public MetricsAssertions(T metric, Predicate<T> predicate) {
        this.metric = metric;
        this.predicate = predicate;
    }

    @Override
    public boolean test(T t) {
        return predicate.test(t);
    }

    public static <T> MetricsAssertions<T> isPresent(Optional<T> t) {
        Predicate<T> p = o -> {
            return o != null;
        };
        return new MetricsAssertions<>(t.orElse(null), p);
    }

    public static <T> MetricsAssertions<T> isNotPresent(Optional<T> t) {
        return isPresent(t).negate();
    }

    public void assertTrue(String message) {
        Assertions.assertTrue(predicate.test(metric), message);
    }

    @Override
    public MetricsAssertions<T> and(Predicate<? super T> other) {
        return new MetricsAssertions<>(metric, Predicate.super.and(other));
    }

    @Override
    public MetricsAssertions<T> negate() {
        return new MetricsAssertions<>(metric, Predicate.super.negate());
    }

    @Override
    public MetricsAssertions<T> or(Predicate<? super T> other) {
        return new MetricsAssertions<>(metric, Predicate.super.or(other));
    }

}
