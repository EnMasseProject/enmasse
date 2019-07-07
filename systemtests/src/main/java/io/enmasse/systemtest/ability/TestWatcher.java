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
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TestWatcher implements AfterTestExecutionCallback {
    private static final Logger log = CustomLogger.getLogger();

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        Method testMethod = extensionContext.getRequiredTestMethod();
        Class testClass = extensionContext.getRequiredTestClass();
        if (extensionContext.getExecutionException().isPresent()) {
            try {
                log.info("Saving pod logs and info...");
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
                                Files.write(Paths.get(filePath.toString()), kube.getLog(p.getMetadata().getName(), c.getName()).getBytes());
                            } catch (IOException e) {
                                log.warn("Cannot write file {}", filePath.getName());
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("Cannot access logs from container: ", ex);
                    }
                }

                kube.getLogsOfTerminatedPods(kube.getNamespace()).forEach((name, podLogTerminated) -> {
                    File filePath = new File(path.toString(), String.format("%s.terminated.log", name));
                    try {
                        Files.write(filePath.toPath(), podLogTerminated.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        log.warn("Cannot write file {}", filePath.getName());
                    }
                });

                Files.write(path.resolve("describe_pods.txt"), KubeCMDClient.describePods(kube.getNamespace()).getStdOut().getBytes());
                Files.write(path.resolve("describe_nodes.txt"), KubeCMDClient.describeNodes().getStdOut().getBytes());
                Files.write(path.resolve("events.txt"), KubeCMDClient.getEvents(kube.getNamespace()).getStdOut().getBytes());
                log.info("Pod logs and describe successfully stored into {}", path.toString());
            } catch (Exception ex) {
                log.warn("Cannot save pod logs and info: {}", ex.getMessage());
            }
        }
    }

    private Path getPath(Method testMethod, Class testClass) {
        Path path = Paths.get(
                Environment.getInstance().testLogDir(),
                "failed_test_logs",
                testClass.getName());
        if (testMethod != null) {
            path = Paths.get(path.toString(), testMethod.getName());
        }
        return path;
    }
}
