/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.listener;

import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.logs.CustomLogger;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;

/**
 * Execution listener useful for safety cleanups of the test environment after test suite execution
 */
public class JunitExecutionListener implements TestExecutionListener {
    private static final Logger LOGGER = CustomLogger.getLogger();

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        TestInfo.getInstance().setTestPlan(testPlan);
        TestInfo.getInstance().printTestClasses();
    }

}
