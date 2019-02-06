/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium;


import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.SystemtestsKubernetesApps;
import io.enmasse.systemtest.timemeasuring.Operation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import org.slf4j.Logger;

public class SeleniumManagement {
    private static Logger log = CustomLogger.getLogger();

    public static void deployFirefoxApp() throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(Operation.CREATE_SELENIUM_CONTAINER);
        log.info("Deploy firefox deployment");
        try {
            SystemtestsKubernetesApps.deployFirefoxSeleniumApp(SystemtestsKubernetesApps.SELENIUM_PROJECT, Kubernetes.create(Environment.getInstance()));
        } catch (Exception e) {
            log.error("Deployment of firefox app failed", e);
            throw e;
        } finally {
            TimeMeasuringSystem.stopOperation(operationID);
        }
    }

    public static void deployChromeApp() throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(Operation.CREATE_SELENIUM_CONTAINER);
        log.info("Deploy chrome deployment");
        try {
            SystemtestsKubernetesApps.deployChromeSeleniumApp(SystemtestsKubernetesApps.SELENIUM_PROJECT, Kubernetes.create(Environment.getInstance()));
        } catch (Exception e) {
            log.error("Deployment of chrome app failed", e);
            throw e;
        } finally {
            TimeMeasuringSystem.stopOperation(operationID);
        }
    }

    public static void removeFirefoxApp() throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(Operation.DELETE_SELENIUM_CONTAINER);
        SystemtestsKubernetesApps.deleteFirefoxSeleniumApp(SystemtestsKubernetesApps.SELENIUM_PROJECT, Kubernetes.create(Environment.getInstance()));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void removeChromeApp() throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(Operation.DELETE_SELENIUM_CONTAINER);
        SystemtestsKubernetesApps.deleteChromeSeleniumApp(SystemtestsKubernetesApps.SELENIUM_PROJECT, Kubernetes.create(Environment.getInstance()));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    private static void copyRheaWebPage(String containerName) {

    }
}
