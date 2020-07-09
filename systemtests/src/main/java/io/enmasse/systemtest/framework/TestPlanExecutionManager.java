/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.framework;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.operator.EnmasseOperatorManager;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;

/**
 * Execution listener useful for safety cleanups of the test environment after test suite execution
 */
public class TestPlanExecutionManager implements TestExecutionListener {
    private static final Logger LOGGER = LoggerUtils.getLogger();
    Environment env = Environment.getInstance();

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        TestPlanInfo.getInstance().setTestPlan(testPlan);
        TestPlanInfo.getInstance().printTestClasses();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        var tags = TestPlanInfo.getInstance().getTestRunTags();
        if (tags != null && tags.size() == 1 && tags.get(0).equals(TestTag.FRAMEWORK)) {
            LOGGER.info("Running framework tests, no cleanup performed");
            return;
        }

        try {
            if (!env.skipUninstall()) {
                EnmasseOperatorManager.getInstance().deleteEnmasseBundle();
            }
        } catch (Exception | AssertionError ex) {
            LOGGER.error(ex.getMessage());
            GlobalLogCollector.saveInfraState(TestUtils.getLogsPath("EndOfTestSuite"));
        }
    }
}