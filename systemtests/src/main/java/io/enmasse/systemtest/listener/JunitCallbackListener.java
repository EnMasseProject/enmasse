/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.listener;

import io.enmasse.systemtest.EnmasseInstallType;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.bases.ThrowableRunner;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.manager.IsolatedIoTManager;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.manager.SharedIoTManager;
import io.enmasse.systemtest.manager.SharedResourceManager;
import io.enmasse.systemtest.operator.OperatorManager;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class JunitCallbackListener implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler,
        AfterEachCallback, BeforeEachCallback, BeforeAllCallback, AfterAllCallback {
    private static final Logger LOGGER = CustomLogger.getLogger();
    private static Environment env = Environment.getInstance();
    private Kubernetes kubernetes = Kubernetes.getInstance();
    private TestInfo testInfo = TestInfo.getInstance();
    private IsolatedResourcesManager isolatedResourcesManager = IsolatedResourcesManager.getInstance();
    private SharedResourceManager sharedResourcesManager = SharedResourceManager.getInstance();
    private SharedIoTManager sharedIoTManager = SharedIoTManager.getInstance();
    private IsolatedIoTManager isolatedIoTManager = IsolatedIoTManager.getInstance();
    private OperatorManager operatorManager = OperatorManager.getInstance();
    private static Exception beforeAllException; //TODO remove it after upgrade to surefire plugin 3.0.0-M5

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        testInfo.setCurrentTestClass(context);
        try { //TODO remove it after upgrade to surefire plugin 3.0.0-M5
            handleCallBackError(context, () -> {
                if (testInfo.isUpgradeTest()) {
                    LOGGER.info("Enmasse is not installed because next test is {}", context.getDisplayName());
                } else if (testInfo.isOLMTest()) {
                    LOGGER.info("Test is OLM");
                    if (operatorManager.isEnmasseOlmDeployed()) {
                        operatorManager.deleteEnmasseOlm();
                    }
                    if (operatorManager.isEnmasseBundleDeployed()) {
                        operatorManager.deleteEnmasseBundle();
                    }
                    operatorManager.installEnmasseOlm(testInfo.getOLMInstallationType());
                } else if (env.installType() == EnmasseInstallType.OLM) {
                    if (!operatorManager.isEnmasseOlmDeployed()) {
                        operatorManager.installEnmasseOlm();
                    }
                    if (!operatorManager.areExamplesApplied()) {
                        operatorManager.installExamplesBundleOlm();
                        operatorManager.waitUntilOperatorReadyOlm();
                    }
                } else {
                    if (!operatorManager.isEnmasseBundleDeployed()) {
                        operatorManager.installEnmasseBundle();
                    }
                    if (testInfo.isClassIoT() && !operatorManager.isIoTOperatorDeployed()) {
                        operatorManager.installIoTOperator();
                    }
                }
            });
        } catch (Exception ex) {
            beforeAllException = ex; //TODO remove it after upgrade to surefire plugin 3.0.0-M5
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        beforeAllException = null; //TODO remove it after upgrade to surefire plugin 3.0.0-M5
        handleCallBackError(extensionContext, () -> {
            if (env.skipCleanup() || env.skipUninstall()) {
                LOGGER.info("Skip cleanup/uninstall is set, enmasse and iot operators won't be deleted");
            } else if (env.installType() == EnmasseInstallType.BUNDLE) {
                if (testInfo.isEndOfIotTests() && operatorManager.isIoTOperatorDeployed()) {
                    operatorManager.removeIoTOperator();
                }
                if (operatorManager.isEnmasseBundleDeployed() && testInfo.isNextTestUpgrade()) {
                    operatorManager.deleteEnmasseBundle();
                }
                if (operatorManager.isEnmasseOlmDeployed()) {
                    operatorManager.deleteEnmasseOlm();
                }
            }
        });
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        testInfo.setCurrentTest(context);
        logPodsInInfraNamespace();
        if (beforeAllException != null) {
            throw beforeAllException;
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        handleCallBackError(extensionContext, () -> {
            LOGGER.info("Teardown section: ");
            if (testInfo.isTestShared()) {
                tearDownSharedResources();
            } else {
                if (testInfo.isTestIoT()) {
                    isolatedIoTManager.tearDown(testInfo.getActualTest());
                } else {
                    tearDownCommonResources();
                }
            }
        });
    }

    private void tearDownCommonResources() throws Exception {
        LOGGER.info("Admin resource manager teardown");
        isolatedResourcesManager.tearDown(testInfo.getActualTest());
        isolatedResourcesManager.unsetReuseAddressSpace();
        isolatedResourcesManager.deleteAddressspacesFromList();
    }

    private void tearDownSharedResources() throws Exception {
        if (testInfo.isAddressSpaceDeleteable() || testInfo.getActualTest().getExecutionException().isPresent()) {
            if (testInfo.isTestIoT()) {
                LOGGER.info("Teardown shared IoT!");
                sharedIoTManager.tearDown(testInfo.getActualTest());
            } else {
                LOGGER.info("Teardown shared!");
                sharedResourcesManager.tearDown(testInfo.getActualTest());
            }
        } else if (sharedResourcesManager.getSharedAddressSpace() != null) {
            sharedResourcesManager.tearDownShared();
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveKubernetesState(context, throwable);
    }

    private void handleCallBackError(ExtensionContext context, ThrowableRunner runnable) throws Exception {
        try {
            runnable.run();
        } catch (Exception ex) {
            try {
                saveKubernetesState(context, ex);
            } catch (Throwable ignored) {
            }
            LOGGER.error("Exception captured in Junit callback", ex);
            throw ex;
        }
    }

    private void saveKubernetesState(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.warn("Test failed: Saving pod logs and info...");
        logPodsInInfraNamespace();
        if (env.isSkipSaveState()) {
            throw throwable;
        }

        String testMethod = extensionContext.getDisplayName();
        Class<?> testClass = extensionContext.getRequiredTestClass();
        try {
            Kubernetes kube = Kubernetes.getInstance();
            Path path = getPath(testMethod, testClass);
            Files.createDirectories(path);
            List<Pod> pods = kube.listPods();
            for (Pod p : pods) {
                try {
                    List<Container> containers = kube.getContainersFromPod(p.getMetadata().getName());
                    for (Container c : containers) {
                        Path filePath = path.resolve(String.format("%s_%s.log", p.getMetadata().getName(), c.getName()));
                        try {
                            Files.writeString(filePath, kube.getLog(p.getMetadata().getName(), c.getName()));
                        } catch (IOException e) {
                            LOGGER.warn("Cannot write file {}", filePath, e);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Cannot access logs from container: ", ex);
                }
            }

            kube.getLogsOfTerminatedPods(kube.getInfraNamespace()).forEach((name, podLogTerminated) -> {
                Path filePath = path.resolve(String.format("%s.terminated.log", name));
                try {
                    Files.writeString(filePath, podLogTerminated);
                } catch (IOException e) {
                    LOGGER.warn("Cannot write file {}", filePath, e);
                }
            });

            Files.writeString(path.resolve("describe_pods.txt"), KubeCMDClient.describePods(kube.getInfraNamespace()).getStdOut());
            Files.writeString(path.resolve("describe_nodes.txt"), KubeCMDClient.describeNodes().getStdOut());
            Files.writeString(path.resolve("events.txt"), KubeCMDClient.getEvents(kube.getInfraNamespace()).getStdOut());
            Files.writeString(path.resolve("configmaps.yaml"), KubeCMDClient.getConfigmaps(kube.getInfraNamespace()).getStdOut());
            if (testInfo.isClassIoT()) {
                Files.writeString(path.resolve("iotconfig.yaml"), KubeCMDClient.getIoTConfig(kube.getInfraNamespace()).getStdOut());
                GlobalLogCollector collectors = new GlobalLogCollector(kube, path, kube.getInfraNamespace());
                collectors.collectAllAdapterQdrProxyState();
            }
            LOGGER.info("Pod logs and describe successfully stored into {}", path);
        } catch (Exception ex) {
            LOGGER.warn("Cannot save pod logs and info: ", ex);
        }
        throw throwable;
    }

    public static Path getPath(String testMethod, Class<?> testClass) {
        Path path = env.testLogDir().resolve(
                Paths.get(
                        "failed_test_logs",
                        testClass.getName()));
        if (testMethod != null) {
            path = path.resolve(testMethod);
        }
        return path;
    }

    private void logPodsInInfraNamespace() {
        LOGGER.info("Print all pods in infra namespace");
        KubeCMDClient.runOnCluster("get", "pods", "-n", kubernetes.getInfraNamespace());
    }

}


