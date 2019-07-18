/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.ability;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.AdminResourcesManager;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.GlobalLogCollector;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SharedAddressSpaceManager {
    private static final Logger log = CustomLogger.getLogger();
    private static SharedAddressSpaceManager instance;
    private List<TestIdentifier> testClasses;
    private List<TestIdentifier> tests;
    private ExtensionContext actualTest;
    private AddressSpace actualAddressSpace;

    private SharedAddressSpaceManager() {
    }

    public static synchronized SharedAddressSpaceManager getInstance() {
        if (instance == null) {
            instance = new SharedAddressSpaceManager();
        }
        return instance;
    }

    public void setActualTest(ExtensionContext test) {
        actualTest = test;
    }

    public void setActualSharedAddressSpace(AddressSpace addressSpace) {
        actualAddressSpace = addressSpace;
    }

    public void setTestPlan(TestPlan testPlan) {
        testClasses = Arrays.asList(testPlan.getChildren(testPlan.getRoots()
                .toArray(new TestIdentifier[0])[0]).toArray(new TestIdentifier[0]));
        tests = new ArrayList<>();
        testClasses.forEach(testIdentifier -> tests.addAll(testPlan.getChildren(testIdentifier)));
    }

    public void printTestClasses() {
        if (testClasses != null) {
            log.info("****************************************************************************");
            log.info("                 Following test classes will run                            ");
            log.info("****************************************************************************");
            testClasses.forEach(testIdentifier -> log.info(testIdentifier.getLegacyReportingName()));
            log.info("****************************************************************************");
            log.info("****************************************************************************");
        }
    }

    public void deleteSharedAddressSpace() throws Exception {
        if (actualAddressSpace != null && isNextTestShared()) {
            if (!isNextTestSameSharedTag()) {
                log.info("Shared address {} space will be removed", actualAddressSpace.getMetadata().getName());
                Environment env = Environment.getInstance();
                if (!env.skipCleanup()) {
                    Kubernetes kube = Kubernetes.getInstance();
                    GlobalLogCollector logCollector = new GlobalLogCollector(kube, new File(env.testLogDir()));
                    try {
                        AddressSpaceUtils.deleteAddressSpaceAndWait(actualAddressSpace, logCollector);
                        actualAddressSpace = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    log.warn("Remove address spaces when test run finished - SKIPPED!");
                }
            }
            log.info("Shared address space environment going to be removed!");
            AdminResourcesManager.getInstance().tearDownSharedEnv();
        } else {
            if (actualAddressSpace != null) {
                log.info("Shared address {} space will be reused", actualAddressSpace.getMetadata().getName());
            }
        }
    }

    public boolean isNextTestShared() {
        if (!tests.isEmpty()){
            TestIdentifier test = tests.stream().filter(testIdentifier -> isSameTestMethod(testIdentifier, actualTest)
                    && isSameClass(testIdentifier, actualTest)).findFirst().get();
            int currentTestIndex = tests.indexOf(test);
            if (!(currentTestIndex == tests.size() - 1)) {
                return !isSameClass(tests.get(currentTestIndex + 1), actualTest);
            }
        }
        return false;
    }

    private boolean isSameClass(TestIdentifier test1, ExtensionContext test2) {
        return test1.getUniqueId().contains(test2.getRequiredTestClass().getName());
    }

    private boolean isSameTestMethod(TestIdentifier test1, ExtensionContext test2) {
        return test1.getLegacyReportingName().replaceAll("\\(.*\\)", "").equals(test2.getRequiredTestMethod().getName());
    }

    private List<String> getTags(TestIdentifier test) {
        return test.getTags().stream().map(org.junit.platform.engine.TestTag::getName).collect(Collectors.toList());
    }

    private boolean isNextTestSameSharedTag() {
        TestIdentifier test = tests.stream().filter(testIdentifier -> isSameTestMethod(testIdentifier, actualTest)
                && isSameClass(testIdentifier, actualTest)).findFirst().get();
        int currentTestIndex = tests.indexOf(test);
        return isSameSharedTag(tests.get(currentTestIndex + 1), actualTest);
    }

    private boolean isSameSharedTag(TestIdentifier test1, ExtensionContext test2) {
        List<String> nextTestTags = getTags(test1);
        List<String> currentTestTags = new ArrayList<>(test2.getTags());

        return (nextTestTags.contains(TestTag.sharedBrokered) && currentTestTags.contains(TestTag.sharedBrokered))
                || (nextTestTags.contains(TestTag.sharedStandard) && currentTestTags.contains(TestTag.sharedStandard))
                || (nextTestTags.contains(TestTag.sharedMqtt) && currentTestTags.contains(TestTag.sharedMqtt));
    }
}
