/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.bases;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

/**
 * Workaround for some JUnit issue. Remove it after upgrade to surefire plugin 3.0.0-M5.
 * <p>
 * To use this you need to:
 * <ul>
 * <li>Extend your test class from {@link JUnitWorkaround}.
 * <li>Wrap all code in {@code @BeforeAll} function using {@link #wrapBeforeAll(ThrowableRunner)}.
 * </ul>
 */
public interface JUnitWorkaround {

    /**
     * Hold the internal global state.
     */
    static final class State {
        private static final State INSTANCE = new State();

        private List<Throwable> exceptions;

        private State() {}
    }

    public static void wrapBeforeAll(final ThrowableRunner runner) {
        try {
            runner.run();
        } catch (Throwable e) {
            addBeforeAllException(e);
        }
    }

    /**
     * Record an exception from a {@code @BeforeAll} method.
     *
     * @param e The exception to store.
     */
    private static void addBeforeAllException(final Throwable e) {
        if (State.INSTANCE.exceptions == null) {
            State.INSTANCE.exceptions = new LinkedList<>();
        }
        State.INSTANCE.exceptions.add(e);
    }

    /**
     * Fetch all stored exception and reset the state.
     *
     * @return All exception that had been recorded. May be {@code null}.
     */
    static List<Throwable> fetchBeforeAllExceptions() {
        if (State.INSTANCE.exceptions == null) {
            return null;
        }
        var result = State.INSTANCE.exceptions;
        State.INSTANCE.exceptions = null;
        return result;
    }

    /**
     * Check if we have recorded "before all" exceptions.
     *
     * @throws Throwable if there was any recorded exception.
     */
    @BeforeEach
    default void triggerBeforeAllException() throws Throwable {

        // check if we have recorded exceptions ...

        final List<Throwable> exceptions = fetchBeforeAllExceptions();
        if (exceptions == null || exceptions.isEmpty()) {
            // ... all good
            return;
        }

        // build up exception

        Throwable first = null;
        for (Throwable e : exceptions) {
            if (first == null) {
                first = e;
            } else {
                first.addSuppressed(e);
            }
        }

        // throw it

        throw first;
    }

}
