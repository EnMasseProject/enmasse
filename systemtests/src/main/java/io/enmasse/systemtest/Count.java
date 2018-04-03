/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import java.util.function.Predicate;

public class Count<T> implements Predicate<T> {
    private final int expected;
    private volatile int actual;

    public Count(int expected) {
        this.expected = expected;
    }

    @Override
    public boolean test(T message) {
        if (message != null) {
            ++actual;
        }
        return actual == expected;
    }

    public int actual() {
        return actual;
    }
}
