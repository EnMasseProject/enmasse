/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.IndicativeSentences;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.ITestSeparator;
import io.enmasse.systemtest.listener.JunitCallbackListener;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messaginginfra.ResourceManager;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.ThrowingCallable;
import io.enmasse.systemtest.utils.ThrowingConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class for all tests
 */
@ExtendWith(JunitCallbackListener.class)
@DisplayNameGeneration(IndicativeSentences.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag(TestTag.SYSTEMTEST)
public abstract class TestBase implements ITestSeparator {
    protected static final Kubernetes kubernetes = Kubernetes.getInstance();
    protected static final UserCredentials clusterUser = new UserCredentials(KubeCMDClient.getOCUser());
    protected static final Environment environment = Environment.getInstance();
    protected UserCredentials defaultCredentials = environment.getDefaultCredentials();
    protected UserCredentials managementCredentials = environment.getManagementCredentials();
    protected ResourceManager resourceManager = ResourceManager.getInstance();


    protected List<ThrowingCallable> cleanup = new LinkedList<>();

    /**
     * Close any provided resource at the end of test, by calling the cleaner.
     *
     * @param <T>      The type of the closable.
     * @param resource The closable to close.
     * @param cleaner  The cleaner, which will be called.
     * @return The input value, for chained calls.
     */
    protected <T> T cleanup(final T resource, final ThrowingConsumer<T> cleaner) {
        this.cleanup.add(() -> cleaner.accept(resource));
        return resource;
    }

    /**
     * Close an {@link AutoCloseable} at the end of the test.
     *
     * @param <T>       The type of the closable.
     * @param closeable The closable to close.
     * @return The input value, for chained calls.
     */
    protected <T extends AutoCloseable> T cleanup(final T closeable) {
        return cleanup(closeable, AutoCloseable::close);
    }

    @AfterEach
    void cleanup() throws Exception {
        Exception exception = null;
        for (ThrowingCallable cleanup : this.cleanup) {
            try {
                cleanup.call();
            } catch (Exception e) {
                if (exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
            }
        }
        this.cleanup.clear();
        if (exception != null) {
            throw exception;
        }
    }
}
