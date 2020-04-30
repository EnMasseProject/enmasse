/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.vertx.core.Promise;

public class MoreFuturesTest {

    @Test
    public void testTwoHandlersBefore() {
        final AtomicInteger counter = new AtomicInteger();

        final Promise<String> p = Promise.promise();
        p.complete("Foo");

        MoreFutures.whenComplete(p.future(), () -> counter.incrementAndGet())
                .onComplete(ar -> counter.incrementAndGet());

        assertEquals(2, counter.get());
    }

    @Test
    public void testTwoHandlersAfter() {
        final AtomicInteger counter = new AtomicInteger();

        final Promise<String> p = Promise.promise();
        MoreFutures.whenComplete(p.future(), () -> counter.incrementAndGet())
                .onComplete(ar -> counter.incrementAndGet());

        p.complete("Foo");

        assertEquals(2, counter.get());
    }

    @Test
    public void testNoException() {
        final AtomicInteger counter = new AtomicInteger();

        final Promise<String> p = Promise.promise();
        MoreFutures.whenComplete(p.future(),
                () -> {
                    throw new RuntimeException("This exception is expected. It shouldn't fail the test though.");
                })
                .onComplete(ar -> counter.incrementAndGet());

        p.complete("Foo");

        assertEquals(1, counter.get());
    }

}
