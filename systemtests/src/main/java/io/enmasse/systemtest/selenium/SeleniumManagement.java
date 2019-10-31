/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;

import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.TestUtils;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps.*;

public class SeleniumManagement {
    private static Logger log = CustomLogger.getLogger();

    public static void deployFirefoxApp() throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_SELENIUM_CONTAINER);
        log.info("Deploy firefox deployment");
        try {
            deployFirefoxSeleniumApp(
                    SELENIUM_PROJECT, Kubernetes.getInstance());
        } catch (Exception e) {
            log.error("Deployment of firefox app failed", e);
            throw e;
        } finally {
            TimeMeasuringSystem.stopOperation(operationID);
        }
    }

    static void deployChromeApp() throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_SELENIUM_CONTAINER);
        log.info("Deploy chrome deployment");
        try {
            deployChromeSeleniumApp(
                    SELENIUM_PROJECT, Kubernetes.getInstance());
        } catch (Exception e) {
            log.error("Deployment of chrome app failed", e);
            throw e;
        } finally {
            TimeMeasuringSystem.stopOperation(operationID);
        }
    }

    public static void removeFirefoxApp() throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_SELENIUM_CONTAINER);
        deleteFirefoxSeleniumApp(
                SELENIUM_PROJECT, Kubernetes.getInstance());
        Kubernetes.getInstance().deleteNamespace(SELENIUM_PROJECT);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    static void collectAppLogs(Path path) {
        try {
            Files.createDirectories(path);
            GlobalLogCollector collector = new GlobalLogCollector(
                    Kubernetes.getInstance(), path.toFile(), SELENIUM_PROJECT);
            collector.collectLogsOfPodsInNamespace(SELENIUM_PROJECT);
        } catch (Exception e) {
            log.error("Failed to collect pod logs from namespace : {}", SELENIUM_PROJECT);
        }
    }

    static void removeChromeApp() {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_SELENIUM_CONTAINER);
        deleteChromeSeleniumApp(SELENIUM_PROJECT, Kubernetes.getInstance());
        TimeMeasuringSystem.stopOperation(operationID);
    }

    static void restartSeleniumApp() {
        List<String> beforeRestart = Kubernetes.getInstance().listPods(SELENIUM_PROJECT).stream()
                .map(pod -> pod.getMetadata().getName()).collect(Collectors.toList());
        int attempts = 5;
        for (int i = 1; i <= attempts; i++) {
            deleteSeleniumPod(SELENIUM_PROJECT, Kubernetes.getInstance());
            try {
                TestUtils.waitUntilCondition("Selenium pods ready", (phase) -> {
                            List<String> current = TestUtils.listReadyPods(Kubernetes.getInstance(),
                                    SELENIUM_PROJECT).stream().map(pod -> pod.getMetadata().getName()).collect(Collectors.toList());
                            current.removeAll(beforeRestart);
                            log.info("Following pods are in ready state {}", current);
                            return current.size() == beforeRestart.size();
                        },
                        new TimeoutBudget(1, TimeUnit.MINUTES));
                break;
            } catch (Exception ex) {
                log.warn("Selenium application was not redeployed correctly, try it again {}/{}", i, attempts);
            }
        }
    }
}
