/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.ability;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestWatcher implements AfterTestExecutionCallback {
    private static final Logger log = CustomLogger.getLogger();

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        Method testMethod = extensionContext.getRequiredTestMethod();
        Class testClass = extensionContext.getRequiredTestClass();
        if (extensionContext.getExecutionException().isPresent()) {
            try {
                log.warn("Test failed: Saving pod logs and info...");
                Kubernetes kube = Kubernetes.getInstance();
                Path path = Paths.get(
                        Environment.getInstance().testLogDir(),
                        "failed_test_logs",
                        testClass.getName(),
                        testMethod.getName());
                Files.createDirectories(path);
                kube.listPods().forEach(pod -> {
                    kube.getContainersFromPod(pod.getMetadata().getName()).forEach(
                            container -> {
                                File filePath = new File(path.toString(), String.format("%s_%s.log", pod.getMetadata().getName(), container.getName()));
                                try {
                                    Files.write(Paths.get(filePath.toString()), kube.getLog(pod.getMetadata().getName(), container.getName()).getBytes());
                                } catch (IOException e) {
                                    log.warn("Cannot write file {}", filePath.getName());
                                }
                            }
                    );
                });
                Files.write(Paths.get(path.toString(), "describe.txt"), KubeCMDClient.describePods(kube.getInfraNamespace()).getStdOut().getBytes());
                log.info("Pod logs and describe successfully stored into {}", path.toString());
            } catch (Exception ex) {
                log.warn("Cannot save pod logs and info: {}", ex.getMessage());
            }
        }
    }
}
