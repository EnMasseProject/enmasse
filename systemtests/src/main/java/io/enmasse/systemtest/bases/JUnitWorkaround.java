/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.bases;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workaround for some JUnit issue.
 * <p>
 * TODO: Remove it after upgrade to surefire plugin 3.0.0-M5.
 * <p>
 * To use this you need to:
 * <ul>
 * <li>Add {@link JUnitWorkaround} as a JUnit extension.
 * </ul>
 */
public class JUnitWorkaround implements BeforeEachCallback, AfterAllCallback, LifecycleMethodExecutionExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(JUnitWorkaround.class);

    private List<Throwable> exceptions;

    @Override
    public void handleBeforeAllMethodExecutionException(final ExtensionContext context, final Throwable throwable) throws Throwable {
        logger.info("Exception caught - recording");
        addBeforeAllException(throwable);
    }

    /**
     * Record an exception from a {@code @BeforeAll} method.
     *
     * @param e The exception to store.
     */
    private void addBeforeAllException(final Throwable e) {
        if (this.exceptions == null) {
            this.exceptions = new LinkedList<>();
        }
        this.exceptions.add(e);
    }

    /**
     * Fetch all stored exceptions.
     *
     * @return All exception that had been recorded. May be {@code null}.
     */
    private List<Throwable> getBeforeAllExceptions() {
        return this.exceptions;
    }

    /**
     * Clear all stored exceptions.
     */
    private void clearBeforeAllExceptions() {
        this.exceptions = null;
    }

    /**
     * Check if we have recorded "before all" exceptions.
     * <p>
     * As we use {@link #beforeEach(ExtensionContext)} this means that all tests covered by the wrapping
     * {@link BeforeAll} call will fail. Which is reasonable as they did not get set up properly.
     *
     * @throws Throwable if there was any recorded exception.
     */
    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {

        // check if we have recorded exceptions ...

        final List<Throwable> exceptions = getBeforeAllExceptions();
        if (exceptions == null || exceptions.isEmpty()) {
            // ... all good
            logger.info("No recorded @BeforeAll errors");
            return;
        }

        logger.warn("There are @BeforeAll errors - count = {}", exceptions.size());

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

        if (first instanceof Exception) {
            // normal exception
            throw (Exception) first;
        } else {
            // assert*() calls throw AssertionError's
            throw new Exception(first);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // we get called when we are closing up the BeforeAll calls
        // now we can clear the collected exceptions and start fresh
        clearBeforeAllExceptions();
    }

}
