/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.framework;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.messaginginfra.ResourceManager;
import io.enmasse.systemtest.operator.EnmasseOperatorManager;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.cluster.KubeClusterManager;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.Logger;

/**
 * This class implements a variety of junit callbacks and orchestrates the full lifecycle of the operator installation
 * used for the test suite
 */
public class TestLifecycleManager implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler,
        AfterEachCallback, BeforeEachCallback, BeforeAllCallback, AfterAllCallback {
    private final Environment env = Environment.getInstance();
    private final Kubernetes kubernetes = Kubernetes.getInstance();
    private final Logger LOGGER = LoggerUtils.getLogger();
    private final TestPlanInfo testInfo = TestPlanInfo.getInstance();
    private final EnmasseOperatorManager operatorManager = EnmasseOperatorManager.getInstance();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        testInfo.setCurrentTestClass(context);
        handleCallBackError("Callback before all", context, () -> {
            if (!operatorManager.isEnmasseBundleDeployed()) {
                operatorManager.installEnmasseBundle();
            }
        });
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        handleCallBackError("Callback after all", extensionContext, () -> {
            if (!env.skipCleanup()) {
                ResourceManager.getInstance().deleteResources(extensionContext);
                KubeClusterManager.getInstance().restoreClassConfigurations();
            }
            if (env.skipCleanup() || env.skipUninstall()) {
                LOGGER.info("Skip cleanup/uninstall is set, enmasse and iot operators won't be deleted");
            }
        });
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        handleCallBackError("Callback before each", context, () -> {
            LoggerUtils.logDelimiter("*");
            LOGGER.info("Before each section");
            testInfo.setCurrentTest(context);
            KubeClusterManager.getInstance().setMethodConfigurations();

            logPodsInInfraNamespace();
            LoggerUtils.logDelimiter("*");
        });
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        handleCallBackError("Callback after each", extensionContext, () -> {
            LoggerUtils.logDelimiter("*");
            LOGGER.info("Teardown section: ");
            if (!env.skipCleanup()) {
                ResourceManager.getInstance().deleteResources(extensionContext);
                KubeClusterManager.getInstance().restoreMethodConfigurations();
            }
            LoggerUtils.logDelimiter("*");
        });
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState("Test execution", context, throwable);
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState("Test before all", context, throwable);
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState("Test before each", context, throwable);
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState("Test after each", context, throwable);
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState("Test after all", context, throwable);
    }

    private void handleCallBackError(String description, ExtensionContext context, ThrowableRunner runnable) throws Exception {
        try {
            runnable.run();
        } catch (Exception | AssertionError ex) {
            try {
                saveKubernetesState(description, context, ex);
            } catch (Throwable ignored) {
            }
            LOGGER.error("Exception captured in Junit callback", ex);
            throw ex;
        }
    }

    private void saveKubernetesState(String description, ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.error("Test failed at {}", description);
        logPodsInInfraNamespace();
        if (env.isSkipSaveState()) {
            throw throwable;
        }
        GlobalLogCollector.saveInfraState(TestUtils.getFailedTestLogsPath(extensionContext));
        throw throwable;
    }

    private void logPodsInInfraNamespace() {
        KubeCMDClient.runOnCluster("get", "pods", "-n", kubernetes.getInfraNamespace(), "-o", "wide");
    }

}