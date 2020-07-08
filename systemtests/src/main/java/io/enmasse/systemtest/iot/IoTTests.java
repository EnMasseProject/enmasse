/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.systemtest.IndicativeSentences;
import io.enmasse.systemtest.TestCleaner;
import io.enmasse.systemtest.framework.ITestSeparator;
import io.enmasse.systemtest.framework.TestLifecycleManager;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Base interface for IoT tests.
 */
@ExtendWith(TestLifecycleManager.class)
@DisplayNameGeneration(IndicativeSentences.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public interface IoTTests extends ITestSeparator, TestCleaner {
}
