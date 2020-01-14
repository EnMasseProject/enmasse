/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.listener;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.operator.OperatorManager;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;

public class JunitExecutionListener implements TestExecutionListener {
    private static final Logger LOGGER = CustomLogger.getLogger();

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        TestInfo.getInstance().setTestPlan(testPlan);
        TestInfo.getInstance().printTestClasses();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {

        final Environment env = Environment.getInstance();

        if (!env.skipCleanup()) {
            // clean up resources
            performCleanup();
        } else {
            LOGGER.warn("Remove address spaces when test run finished - SKIPPED!");
        }

        TimeMeasuringSystem.printAndSaveResults();

        if (!(env.skipCleanup() || env.skipUninstall())) {
            // clean up infrastructure after resources
            performInfraCleanup();
        }

    }

    private void performCleanup() {
        final Environment env = Environment.getInstance();
        final Kubernetes kube = Kubernetes.getInstance();
        final GlobalLogCollector logCollector = new GlobalLogCollector(kube, env.testLogDir());

        if (IoTUtils.isIoTInstalled(kube)) {
            try {
                kube.getNonNamespacedIoTProjectClient().list().getItems().forEach(project -> {
                    LOGGER.info("iot project '{}' will be removed", project.getMetadata().getName());
                    try {
                        IoTUtils.deleteIoTProjectAndWait(kube, project);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to delete IoT projects: {}", project, e);
                    }
                });
                kube.getIoTConfigClient().list().getItems().forEach(config -> {
                    LOGGER.info("iot config '{}' will be removed", config.getMetadata().getName());
                    try {
                        IoTUtils.deleteIoTConfigAndWait(kube, config);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to delete IoT config: {}", config, e);
                    }
                });
                LOGGER.info("Infinispan server will be removed");
                SystemtestsKubernetesApps.deleteInfinispanServer();
                if (!SystemtestsKubernetesApps.INFINISPAN_PROJECT.equals(kube.getInfraNamespace())) {
                    kube.deleteNamespace(SystemtestsKubernetesApps.INFINISPAN_PROJECT);
                }
            } catch (Exception e) {
                LOGGER.warn("Cleanup failed or no clean is needed");
            }
        }

        /*
         * Clean up address spaces after IoT projects, as the iot operator immediately re-created missing
         * address spaces.
         */
        try {
            kube.getAddressSpaceClient().inAnyNamespace().list().getItems().forEach((addrSpace) -> {
                LOGGER.info("address space '{}' will be removed", addrSpace);
                try {
                    AddressSpaceUtils.deleteAddressSpaceAndWait(addrSpace, logCollector);
                    IsolatedResourcesManager.getInstance().tearDown(TestInfo.getInstance().getActualTest());
                } catch (Exception e) {
                    LOGGER.warn("Cleanup failed or no clean is needed");
                }
            });
        } catch (Exception e) {
            LOGGER.warn("Cleanup failed or no clean is needed");
        }

    }

    private void performInfraCleanup() {
        try {
            OperatorManager.getInstance().removeIoTOperator();
            OperatorManager.getInstance().deleteEnmasseOlm();
            OperatorManager.getInstance().deleteEnmasseBundle();
        } catch (Exception e) {
            LOGGER.error("Failed", e);
        }
    }

}
