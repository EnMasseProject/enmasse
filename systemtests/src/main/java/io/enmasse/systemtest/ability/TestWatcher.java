/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.ability;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TestWatcher implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
    private static final Logger log = CustomLogger.getLogger();

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
        Method testMethod = extensionContext.getRequiredTestMethod();
        Class testClass = extensionContext.getRequiredTestClass();
        try {
            log.warn("Test failed: Saving pod logs and info...");
            Kubernetes kube = Kubernetes.getInstance();
            Path path = Paths.get(
                    Environment.getInstance().testLogDir(),
                    "failed_test_logs",
                    testClass.getName(),
                    testMethod.getName());
            Files.createDirectories(path);
            List<Pod> pods = kube.listPods();
            for (Pod p : pods) {
                try {
                    List<Container> containers = kube.getContainersFromPod(p.getMetadata().getName());
                    for (Container c : containers) {
                        File filePath = new File(path.toString(), String.format("%s_%s.log", p.getMetadata().getName(), c.getName()));
                        try {
                            Files.write(filePath.toPath(), kube.getLog(p.getMetadata().getName(), c.getName()).getBytes());
                        } catch (IOException e) {
                            log.warn("Cannot write file {}", filePath.getName());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Cannot access logs from container: ", ex);
                }
            }
            Files.write(path.resolve("describe.txt"), KubeCMDClient.describePods(kube.getInfraNamespace()).getStdOut().getBytes());
            Files.write(path.resolve("events.txt"), KubeCMDClient.getEvents(kube.getInfraNamespace()).getStdOut().getBytes());
            log.info("Pod logs and describe successfully stored into {}", path.toString());
        } catch (Exception ex) {
            log.warn("Cannot save pod logs and info: {}", ex.getMessage());
        }
        throw throwable;
    }
}
