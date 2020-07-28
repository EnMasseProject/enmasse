/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.framework;

import io.enmasse.systemtest.EnmasseInstallType;
import io.enmasse.systemtest.framework.condition.KubernetesCondition;
import io.enmasse.systemtest.framework.condition.OpenshiftCondition;
import io.enmasse.systemtest.framework.condition.SupportedInstallType;
import io.enmasse.systemtest.framework.condition.SupportedInstallTypeCondition;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
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

/**
 * Class for store and query information about test plan and tests
 */
public class TestPlanInfo {
    private static final Logger LOGGER = LoggerUtils.getLogger();
    private static TestPlanInfo testPlanInfo = null;
    private List<TestIdentifier> testClasses;
    private List<TestIdentifier> tests;
    private ExtensionContext currentTest;
    private ExtensionContext currentTestClass;
    private List<String> testRunTags;

    public static synchronized TestPlanInfo getInstance() {
        if (testPlanInfo == null) {
            testPlanInfo = new TestPlanInfo();
        }
        return testPlanInfo;
    }

    public void setTestPlan(TestPlan testPlan) {
        LOGGER.info("Setting testplan {}", testPlan.getRoots());
        tests = new ArrayList<>();
        List<TestIdentifier> testPlanClasses = Arrays.asList(testPlan.getChildren(testPlan.getRoots()
                .toArray(new TestIdentifier[0])[0])
                .toArray(new TestIdentifier[0]));
        testPlanClasses.forEach(testIdentifier -> {
            testPlan.getChildren(testIdentifier)
                    .forEach(test -> {
                        if (test.getSource().isPresent() && test.getSource().get() instanceof MethodSource) {
                            MethodSource testSource = (MethodSource) test.getSource().get();
                            try {
                                Optional<Method> testMethod = ReflectionUtils.findMethod(Class.forName(testSource.getClassName()), testSource.getMethodName(), testSource.getMethodParameterTypes());
                                if (testMethod.isPresent()) {
                                    TestMethodExtensionContext extensionContext = new TestMethodExtensionContext(Class.forName(testSource.getClassName()), testMethod);
                                    ExecutionCondition[] conditions = new ExecutionCondition[]{this::disabledCondition, new SupportedInstallTypeCondition(), new KubernetesCondition(), new OpenshiftCondition()};
                                    if (evaluateTestDisabled(extensionContext, conditions)) {
                                        LOGGER.debug("Test {}.{} is disabled", testSource.getClassName(), testSource.getMethodName());
                                    } else {
                                        tests.add(test);
                                    }
                                } else {
                                    LOGGER.error("Missing method: {}#{}", testSource.getClassName(), testSource.getMethodName());
                                }
                            } catch (ClassNotFoundException e) {
                                throw new IllegalArgumentException(e);
                            }
                        } else {
                            LOGGER.warn("Test {} doesn't have MethodSource", test.getUniqueId());
                        }
                    });
        });
        LOGGER.debug("Final tests are {}", tests);
        List<String> finalClasses = tests.stream()
                .map(test -> (MethodSource) test.getSource().get())
                .map(MethodSource::getClassName)
                .map(t -> {
                    try {
                        return Class.forName(t);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .map(Class::getSimpleName)
                .distinct()
                .collect(Collectors.toList());

        testClasses = testPlanClasses.stream()
                .filter(testClass -> {
                    return finalClasses.stream()
                            .anyMatch(testClassName -> {
                                return testClassName.equals(testClass.getDisplayName());
                            });
                })
                .collect(Collectors.toList());

        testRunTags = testClasses.stream()
                .map(t -> {
                    return t.getTags();
                })
                .flatMap(Set::stream)
                .distinct()
                .map(t -> t.getName())
                .collect(Collectors.toList());
    }

    private ConditionEvaluationResult disabledCondition(ExtensionContext ctx) {
        return AnnotationSupport.findAnnotation(ctx.getElement().get(), Disabled.class)
                .map(a -> ConditionEvaluationResult.disabled("Disabled annotation"))
                .orElseGet(() -> ConditionEvaluationResult.enabled("No disabled annotation"));
    }

    private boolean evaluateTestDisabled(ExtensionContext context, ExecutionCondition... conditions) {
        for (ExecutionCondition condition : conditions) {
            if (condition.evaluateExecutionCondition(context).isDisabled()) {
                return true;
            }
        }
        return false;
    }

    public void printTestClasses() {
        if (testClasses != null) {
            LoggerUtils.logDelimiter("*");
            LOGGER.info("                     Following test classes will run");
            LoggerUtils.logDelimiter("*");
            testClasses.forEach(testIdentifier -> LOGGER.info(testIdentifier.getLegacyReportingName()));
            LoggerUtils.logDelimiter("*");
            LoggerUtils.logDelimiter("*");
        }
    }

    public ExtensionContext getActualTest() {
        return currentTest;
    }

    public ExtensionContext getActualTestClass() {
        return currentTestClass;
    }

    public void setCurrentTest(ExtensionContext test) {
        currentTest = test;
    }

    public void setCurrentTestClass(ExtensionContext testClass) {
        currentTestClass = testClass;
    }

    public List<String> getTestRunTags() {
        return testRunTags;
    }

    public boolean isOLMTest() {
        return AnnotationSupport.findAnnotation(currentTestClass.getElement(), SupportedInstallType.class)
                .map(a -> a.value() == EnmasseInstallType.OLM)
                .orElse(false);
    }
}