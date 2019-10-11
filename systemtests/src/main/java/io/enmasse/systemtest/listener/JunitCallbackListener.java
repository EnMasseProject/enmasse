/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.listener;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.IsolatedIoTManager;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.manager.SharedIoTManager;
import io.enmasse.systemtest.manager.SharedResourceManager;
import io.enmasse.systemtest.operator.OperatorManager;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class JunitCallbackListener implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler,
        BeforeTestExecutionCallback, AfterEachCallback, BeforeEachCallback, BeforeAllCallback, AfterAllCallback {
    private static final Logger LOGGER = CustomLogger.getLogger();
    private TestInfo testInfo = TestInfo.getInstance();
    private IsolatedResourcesManager isolatedResourcesManager = IsolatedResourcesManager.getInstance();
    private SharedResourceManager sharedResourcesManager = SharedResourceManager.getInstance();
    private SharedIoTManager sharedIoTManager = SharedIoTManager.getInstance();
    private IsolatedIoTManager isolatedIoTManager = IsolatedIoTManager.getInstance();
    private OperatorManager operatorManager = OperatorManager.getInstance();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        testInfo.setActualTestClass(context);
        if (!operatorManager.isEnmasseBundleDeployed() && !testInfo.isUpgradeTest()) {
            operatorManager.installEnmasseBundle();
        }
        if (testInfo.isClassIoT() && !operatorManager.isIoTOperatorDeployed() && !testInfo.isUpgradeTest()) {
            operatorManager.installIoTOperator();
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (!Environment.getInstance().skipCleanup()) {
            if (testInfo.isEndOfIotTests() && operatorManager.isIoTOperatorDeployed()) {
                operatorManager.removeIoTOperator();
            }
            if (operatorManager.isEnmasseBundleDeployed() && testInfo.isNextTestUpgrade()) {
                operatorManager.deleteEnmasseBundle();
            }
        } else {
            LOGGER.info("Skip cleanup is set, enmasse and iot operators won't be deleted");
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        testInfo.setActualTest(context);
        if (testInfo.isTestShared()) {
            Environment.getInstance().getDefaultCredentials().setUsername("test").setPassword("test");
            Environment.getInstance().setManagementCredentials(new UserCredentials("artemis-admin", "artemis-admin"));
        }
        if (SharedIoTManager.getInstance().getAmqpClientFactory() == null) {
            sharedIoTManager.setup();
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        if (testInfo.isTestShared()) {
            if (sharedResourcesManager.getAmqpClientFactory() == null) {
                sharedResourcesManager.setup();
            }
        } else {
            if (testInfo.isTestIoT()) {
                isolatedIoTManager.setup();
            } else {
                isolatedResourcesManager.setup();
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
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
    }

    private void tearDownCommonResources() throws Exception {
        LOGGER.info("Admin resource manager teardown");
        isolatedResourcesManager.tearDown(testInfo.getActualTest());
        isolatedResourcesManager.unsetReuseAddressSpace();
        isolatedResourcesManager.deleteAddressspacesFromList();
    }

    private void tearDownSharedResources() throws Exception {
        if (testInfo.isAddressSpaceDeletable() || testInfo.getActualTest().getExecutionException().isPresent()) {
            if (testInfo.isTestIoT()) {
                LOGGER.info("Teardown shared IoT!");
                sharedIoTManager.tearDown(testInfo.getActualTest());
            } else {
                LOGGER.info("Teardown shared!");
                sharedResourcesManager.tearDown(testInfo.getActualTest());
            }
        } else if (sharedResourcesManager.getSharedAddressSpace() != null) {
            LOGGER.info("Deleting addresses");
            sharedResourcesManager.deleteAddresses(sharedResourcesManager.getSharedAddressSpace());
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

    private void saveKubernetesState(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        if (isSkipSaveState()) {
            throw throwable;
        }

        Method testMethod = extensionContext.getTestMethod().orElse(null);
        Class<?> testClass = extensionContext.getRequiredTestClass();
        try {
            LOGGER.warn("Test failed: Saving pod logs and info...");
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
            if (testInfo.isTestIoT()) {
                Files.writeString(path.resolve("iotconfig.yaml"), KubeCMDClient.getIoTConfig(kube.getInfraNamespace()).getStdOut());
            }
            LOGGER.info("Pod logs and describe successfully stored into {}", path);
        } catch (Exception ex) {
            LOGGER.warn("Cannot save pod logs and info: ", ex);
        }
        throw throwable;
    }

    public static Path getPath(Method testMethod, Class<?> testClass) {
        Path path = Paths.get(
                Environment.getInstance().testLogDir(),
                "failed_test_logs",
                testClass.getName());
        if (testMethod != null) {
            path = path.resolve(testMethod.getName());
        }
        return path;
    }

    private boolean isSkipSaveState() {
        return Environment.getInstance().isSkipSaveState();
    }
}


