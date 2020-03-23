/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;

public class AssertionPredicate<T> implements Predicate<T> {

    private T value;
    private Predicate<T> predicate;

    public AssertionPredicate(T value, Predicate<T> predicate) {
        this.value = value;
        this.predicate = predicate;
    }

    @Override
    public boolean test(T t) {
        return predicate.test(t);
    }

    public static <T> AssertionPredicate<T> isPresent(Optional<T> value) {
        Predicate<T> p = o -> {
            return o != null;
        };
        return new AssertionPredicate<>(value.orElse(null), p);
    }

    public static <T> AssertionPredicate<T> isNotPresent(Optional<T> value) {
        return isPresent(value).negate();
    }

    public static <T> AssertionPredicate<T> from(T value, Predicate<T> predicate) {
        return new AssertionPredicate<T>(value, predicate);
    }

    public void assertTrue(String message) {
        Assertions.assertTrue(predicate.test(value), message);
    }

    @Override
    public AssertionPredicate<T> and(Predicate<? super T> other) {
        return new AssertionPredicate<>(value, Predicate.super.and(other));
    }

    @Override
    public AssertionPredicate<T> negate() {
        return new AssertionPredicate<>(value, Predicate.super.negate());
    }

    @Override
    public AssertionPredicate<T> or(Predicate<? super T> other) {
        return new AssertionPredicate<>(value, Predicate.super.or(other));
    }

}
