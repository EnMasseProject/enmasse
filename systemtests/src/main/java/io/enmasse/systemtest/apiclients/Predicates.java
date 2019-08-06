/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.apiclients;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Predicates {
    private Predicates() {
    }

    public static <T> Predicate<T> is(T value) {
        return new Predicate<>() {

            @Override
            public boolean test(T t) {
                return value.equals(t);
            }

            @Override
            public String toString() {
                return value.toString();
            }
        };
    }

    public static <T> Predicate<T> any() {
        return v -> true;
    }

    public static <T> Predicate<T> in(final T value) {
        return is(value);
    }

    @SafeVarargs
    public static <T> Predicate<T> notIn(final T... values) {
        return in(Arrays.asList(values)).negate();
    }

    @SafeVarargs
    public static <T> Predicate<T> in(final T... values) {
        return in(Arrays.asList(values));
    }

    public static <T> Predicate<T> in(final Collection<T> values) {

        return new Predicate<>() {

            @Override
            public boolean test(T t) {
                return values.contains(t);
            }

            @Override
            public String toString() {
                return values.stream().map(Object::toString).collect(Collectors.joining(", "));
            }
        };
    }
}
