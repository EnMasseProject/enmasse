/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import io.enmasse.systemtest.framework.ITestSeparator;
import io.enmasse.systemtest.framework.TestLifecycleManager;
import io.enmasse.systemtest.framework.TestTag;
import io.enmasse.systemtest.messagingclients.MessagingClientRunner;
import io.enmasse.systemtest.messaginginfra.ResourceManager;
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

/**
 * Base class for all tests
 */
@ExtendWith(TestLifecycleManager.class)
@DisplayNameGeneration(IndicativeSentences.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag(TestTag.SYSTEMTEST)
public abstract class TestBase implements ITestSeparator {
    protected final Environment environment = Environment.getInstance();
    protected final Kubernetes kubernetes = Kubernetes.getInstance();
    protected final ResourceManager resourceManager = ResourceManager.getInstance();
    protected final MessagingClientRunner clientRunner = new MessagingClientRunner();

    @AfterEach
    void cleanup() throws Exception {
        clientRunner.cleanClients();
    }
}
