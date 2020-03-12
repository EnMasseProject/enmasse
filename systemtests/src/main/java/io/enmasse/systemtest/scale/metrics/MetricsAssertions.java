/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;

public class MetricsAssertions<X> implements Predicate<X> {

    private X metric;
    private Predicate<X> predicate;

    public MetricsAssertions(X metric, Predicate<X> predicate) {
        this.metric = metric;
        this.predicate = predicate;
    }

    @Override
    public boolean test(X t) {
        return predicate.test(t);
    }

    public static <X> MetricsAssertions<X> isPresent(Optional<X> t) {
        Predicate<X> p = o -> {
            return o != null;
        };
        return new MetricsAssertions<X>(t.orElse(null), p);
    }

    public static <X> MetricsAssertions<X> isNotPresent(Optional<X> t) {
        return isPresent(t).negate();
    }

    public void assertTrue(String message) {
        Assertions.assertTrue(predicate.test(metric), message);
    }

    @Override
    public MetricsAssertions<X> and(Predicate<? super X> other) {
        return new MetricsAssertions<X>(metric, Predicate.super.and(other));
    }

    @Override
    public MetricsAssertions<X> negate() {
        return new MetricsAssertions<X>(metric, Predicate.super.negate());
    }

    @Override
    public MetricsAssertions<X> or(Predicate<? super X> other) {
        return new MetricsAssertions<X>(metric, Predicate.super.or(other));
    }

}
