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
        Environment env = Environment.getInstance();
        if (!env.skipCleanup()) {
            Kubernetes kube = Kubernetes.getInstance();
            GlobalLogCollector logCollector = new GlobalLogCollector(kube, env.testLogDir());
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
            if (IoTUtils.isIoTInstalled(kube)) {
                try {
                    kube.getNonNamespacedIoTProjectClient().list().getItems().forEach(project -> {
                        LOGGER.info("iot project '{}' will be removed", project.getMetadata().getName());
                        try {
                            IoTUtils.deleteIoTProjectAndWait(kube, project);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    var iotConfigClient = kube.getIoTConfigClient();
                    iotConfigClient.list().getItems().forEach(config -> {
                        LOGGER.info("iot config '{}' will be removed", config.getMetadata().getName());
                        try {
                            IoTUtils.deleteIoTConfigAndWait(kube, config);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    LOGGER.info("Infinispan server will be removed");
                    SystemtestsKubernetesApps.deleteInfinispanServer(kube.getInfraNamespace());
                } catch (Exception e) {
                    LOGGER.warn("Cleanup failed or no clean is needed");
                }
            }
        } else {
            LOGGER.warn("Remove address spaces when test run finished - SKIPPED!");
        }
        TimeMeasuringSystem.printAndSaveResults();
        if (!(Environment.getInstance().skipCleanup() || Environment.getInstance().skipUninstall())) {
            try {
                OperatorManager.getInstance().removeIoTOperator();
                OperatorManager.getInstance().deleteEnmasseOlm();
                OperatorManager.getInstance().deleteEnmasseBundle();
                OperatorManager.getInstance().clean();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
