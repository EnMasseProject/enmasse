/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.info;

import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.logs.CustomLogger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestInfo {
    private static final Logger LOGGER = CustomLogger.getLogger();
    private static TestInfo testInfo = null;
    private List<TestIdentifier> testClasses;
    private List<TestIdentifier> tests;
    private ExtensionContext actualTest;

    public static synchronized TestInfo getInstance() {
        if (testInfo == null) {
            testInfo = new TestInfo();
        }
        return testInfo;
    }

    public void setTestPlan(TestPlan testPlan) {
        LOGGER.warn("Setting testplan");
        testClasses = Arrays.asList(testPlan.getChildren(testPlan.getRoots()
                .toArray(new TestIdentifier[0])[0]).toArray(new TestIdentifier[0]));
        tests = new ArrayList<>();
        testClasses.forEach(testIdentifier -> tests.addAll(testPlan.getChildren(testIdentifier)));
    }

    public void printTestClasses() {
        if (testClasses != null) {
            LOGGER.info("****************************************************************************");
            LOGGER.info("                 Following test classes will run                            ");
            LOGGER.info("****************************************************************************");
            testClasses.forEach(testIdentifier -> LOGGER.info(testIdentifier.getLegacyReportingName()));
            LOGGER.info("****************************************************************************");
            LOGGER.info("****************************************************************************");
        }
    }

    public boolean isAddressSpaceDeletable() {
        int currentTestIndex = getCurrentTestIndex();
        if (!(currentTestIndex == tests.size() - 1)) {
            return !isSameClass(tests.get(currentTestIndex + 1), actualTest)
                    && !isSameSharedTag(tests.get(currentTestIndex + 1), actualTest);
        }
        return true;
    }

    public boolean isSameClass(TestIdentifier test1, ExtensionContext test2) {
        return test1.getUniqueId().contains(test2.getRequiredTestClass().getName());
    }

    public boolean isSameTestMethod(TestIdentifier test1, ExtensionContext test2) {
        return test1.getLegacyReportingName().replaceAll("\\(.*\\)", "").equals(test2.getRequiredTestMethod().getName());
    }

    public List<String> getTags(TestIdentifier test) {
        return test.getTags().stream().map(org.junit.platform.engine.TestTag::getName).collect(Collectors.toList());
    }

    public boolean isSameSharedTag(TestIdentifier test1, ExtensionContext test2) {
        List<String> nextTestTags = getTags(test1);
        List<String> currentTestTags = new ArrayList<>(test2.getTags());

        return (nextTestTags.contains(TestTag.SHARED_BROKERED) && currentTestTags.contains(TestTag.SHARED_BROKERED))
                || ((nextTestTags.contains(TestTag.SHARED_STANDARD) && !nextTestTags.contains(TestTag.SHARED_MQTT))
                    && (currentTestTags.contains(TestTag.SHARED_STANDARD) && !currentTestTags.contains(TestTag.SHARED_MQTT)))
                || (nextTestTags.contains(TestTag.SHARED_MQTT) && currentTestTags.contains(TestTag.SHARED_MQTT));
    }

    public boolean isTestShared() {
        for (String tag : getTags(tests.get(getCurrentTestIndex()))) {
            LOGGER.info("Testtag: " + tag);
            if (TestTag.SHARED_TAGS.stream().anyMatch(sharedTag -> tag.equals(sharedTag))) {
                LOGGER.info("Test is shared");
                return true;
            }
        }
        LOGGER.info("Test is not shared!");
        return false;
    }

    public int getCurrentTestIndex() {
        TestIdentifier test = tests.stream().filter(testIdentifier -> isSameTestMethod(testIdentifier, actualTest)
                && isSameClass(testIdentifier, actualTest)).findFirst().get();
        return tests.indexOf(test);
    }

    public List<TestIdentifier> getTests() {
        return tests;
    }

    public ExtensionContext getActualTest() {
        return actualTest;
    }

    public void setActualTest(ExtensionContext test) {
        actualTest = test;
    }
}
