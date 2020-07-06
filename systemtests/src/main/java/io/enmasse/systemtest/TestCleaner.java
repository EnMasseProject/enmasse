/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import io.enmasse.systemtest.utils.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;

import java.util.LinkedList;
import java.util.List;

/**
 * Allow cleaning up after a test.
 *
 * This interface can be added to your test case to help clean up. During the test you can call any of the 'cleanup()'
 * methods, and the code will be run after the test finished.
 */
public interface TestCleaner {

    class State {
        private final static List<ThrowingCallable> cleaners = new LinkedList<>();
    }

    default void cleanup(final ThrowingCallable cleaner) {
        State.cleaners.add(cleaner);
    }

    default <T extends AutoCloseable> T cleanup(final T closeable) {
        cleanup((ThrowingCallable) closeable::close);
        return closeable;
    }

    @AfterEach
    default void runCleaners() throws Exception {

        Exception ex = null;

        for (ThrowingCallable callable : State.cleaners) {
            try {
                callable.call();
            } catch (Exception e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex.addSuppressed(e);
                }
            }
        }

        State.cleaners.clear();

        if (ex != null) {
            throw ex;
        }
    }

}
