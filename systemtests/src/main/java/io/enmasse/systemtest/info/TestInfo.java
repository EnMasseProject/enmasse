/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.info;

import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.condition.AssumeKubernetesCondition;
import io.enmasse.systemtest.condition.AssumeOpenshiftCondition;
import io.enmasse.systemtest.logs.CustomLogger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
/*
Class for store and query information about test plan and tests
 */
public class TestInfo {
    private static final Logger LOGGER = CustomLogger.getLogger();
    private static TestInfo testInfo = null;
    private List<TestIdentifier> testClasses;
    private List<TestIdentifier> tests;
    private ExtensionContext currentTest;
    private ExtensionContext currentTestClass;

    public static synchronized TestInfo getInstance() {
        if (testInfo == null) {
            testInfo = new TestInfo();
        }
        return testInfo;
    }

    public void setTestPlan(TestPlan testPlan) {
        LOGGER.info("Setting testplan");
        tests = new ArrayList<>();
        testClasses = Arrays.asList(testPlan.getChildren(testPlan.getRoots()
                .toArray(new TestIdentifier[0])[0]).toArray(new TestIdentifier[0]));
        testClasses.forEach(testIdentifier -> {
            testPlan.getChildren(testIdentifier)
            .forEach(test -> {
                if (test.getSource().isPresent() && test.getSource().get() instanceof MethodSource) {
                    MethodSource testSource = (MethodSource) test.getSource().get();
                    try {
                        Optional<Method> testMethod = ReflectionUtils.findMethod(Class.forName(testSource.getClassName()), testSource.getMethodName(), testSource.getMethodParameterTypes());
                        MethodBasedExtensionContext extensionContext = new MethodBasedExtensionContext(testMethod);
                        if (testMethod.isPresent()) {
                            Optional<Disabled> disabled = AnnotationSupport.findAnnotation(testMethod.get(), Disabled.class);
                            ConditionEvaluationResult kubernetesDisabled = new AssumeKubernetesCondition().evaluateExecutionCondition(extensionContext);
                            ConditionEvaluationResult openshiftDisabled = new AssumeOpenshiftCondition().evaluateExecutionCondition(extensionContext);
                            if (disabled.isPresent() || kubernetesDisabled.isDisabled() || openshiftDisabled.isDisabled()) {
                                LOGGER.debug("Test {}.{} is disabled", testSource.getClassName(), testSource.getMethodName());
                            } else {
                                tests.add(test);
                            }
                        }
                    } catch ( ClassNotFoundException e ) {
                        throw new IllegalArgumentException(e);
                    }
                } else {
                    LOGGER.warn("Test {} doesn't have MethodSource", test.getUniqueId());
                }
            });
        });
        LOGGER.debug("Final tests are {}", tests);
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

    public boolean isAddressSpaceDeleteable() {
        boolean isDeleteable = true;
        int currentTestIndex = getCurrentTestIndex();
        if (currentTestIndex + 1 < tests.size()) {
            isDeleteable = !isSameSharedTag(tests.get(currentTestIndex + 1), currentTest);
        }
        LOGGER.info("AddressSpace isDeleteable: {}", isDeleteable);
        return isDeleteable;
    }

    public ExtensionContext getActualTest() {
        return currentTest;
    }

    public void setCurrentTest(ExtensionContext test) {
        currentTest = test;
    }

    public void setCurrentTestClass(ExtensionContext testClass) {
        currentTestClass = testClass;
    }

    public boolean isTestShared() {
        for (String tag : currentTest.getTags()) {
            if (TestTag.SHARED_TAGS.contains(tag)) {
                LOGGER.info("Test is shared");
                return true;
            }
        }
        LOGGER.info("Test is not shared!");
        return false;
    }

    public boolean isTestIoT() {
        for (String tag : currentTest.getTags()) {
            if (TestTag.IOT_TAGS.contains(tag)) {
                LOGGER.info("Test is IoT");
                return true;
            }
        }
        LOGGER.info("Test is not IoT!");
        return false;
    }

    public boolean isClassIoT() {
        return currentTestClass.getTags().stream().anyMatch(tag -> TestTag.IOT_TAGS.contains(tag));
    }

    public boolean isEndOfIotTests() {
        int currentTestIndex = getCurrentTestIndex();
        if (currentTestIndex + 1 < tests.size()) {
            return getTags(tests.get(currentTestIndex + 1)).stream().noneMatch(tag -> TestTag.IOT_TAGS.contains(tag));
        }
        return true;
    }

    public boolean isUpgradeTest() {
        return currentTestClass.getTags().stream().anyMatch(TestTag.UPGRADE::equals);
    }

    public boolean isOLMTest() {
        return currentTestClass.getTags().stream().anyMatch(TestTag.OLM::equals);
    }

    public boolean isNextTestUpgrade() {
        int currentClassIndex = getCurrentClassIndex();
        if (currentClassIndex + 1 < testClasses.size()) {
            return getTags(testClasses.get(currentClassIndex + 1)).stream().anyMatch(TestTag.UPGRADE::equals);
        }
        return false;
    }

    public boolean isNextTestOLM() {
        int currentClassIndex = getCurrentClassIndex();
        if (currentClassIndex + 1 < testClasses.size()) {
            return getTags(testClasses.get(currentClassIndex + 1)).stream().anyMatch(TestTag.OLM::equals);
        }
        return false;
    }

    private List<String> getTags(TestIdentifier test) {
        return test.getTags().stream().map(org.junit.platform.engine.TestTag::getName).collect(Collectors.toList());
    }

    private boolean isSameSharedTag(TestIdentifier test1, ExtensionContext test2) {
        List<String> nextTestTags = getTags(test1);
        Set<String> currentTestTags = test2.getTags();

        return (nextTestTags.contains(TestTag.SHARED_BROKERED) && currentTestTags.contains(TestTag.SHARED_BROKERED))
                || (nextTestTags.contains(TestTag.SHARED_STANDARD) && currentTestTags.contains(TestTag.SHARED_STANDARD))
                || (nextTestTags.contains(TestTag.SHARED_MQTT) && currentTestTags.contains(TestTag.SHARED_MQTT))
                || (nextTestTags.contains(TestTag.SHARED_IOT) && currentTestTags.contains(TestTag.SHARED_IOT));
    }

    private int getCurrentTestIndex() {
        if (currentTest != null && tests.size() > 0) {
            TestIdentifier test = tests.stream()
                    .filter(testIdentifier -> isSameTestMethod(testIdentifier, currentTest) && isSameClass(testIdentifier, currentTest))
                    .findFirst()
                    .get();
            return tests.indexOf(test);
        }
        return 0;
    }

    private int getCurrentClassIndex() {
        if (currentTestClass != null && testClasses.size() > 0) {
            TestIdentifier test = testClasses.stream()
                    .filter(testClass -> isSameClass(testClass, currentTestClass))
                    .findFirst()
                    .get();
            return testClasses.indexOf(test);
        }
        return 0;
    }

    private boolean isSameClass(TestIdentifier test1, ExtensionContext test2) {
        return test1 != null && test2 != null && test1.getUniqueId().contains(test2.getRequiredTestClass().getName());
    }

    private boolean isSameTestMethod(TestIdentifier test1, ExtensionContext test2) {
        return test1 != null && test2 != null && test1.getDisplayName().equals(test2.getDisplayName());
    }

}
