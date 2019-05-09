/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.ability;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.GlobalLogCollector;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;

import java.io.File;
import java.net.MalformedURLException;


public class ExecutionListener implements TestExecutionListener {
    private static final Logger log = CustomLogger.getLogger();

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        Environment env = Environment.getInstance();
        if (!env.skipCleanup()) {
            Kubernetes kube = Kubernetes.getInstance();
            try {
                AddressApiClient apiClient = new AddressApiClient(kube);
                GlobalLogCollector logCollector = new GlobalLogCollector(kube, new File(env.testLogDir()));
                try {
                    AddressSpaceUtils.getAddressSpacesObjects(apiClient).forEach((addrSpace) -> {
                        log.info("address space '{}' will be removed", addrSpace);
                        try {
                            AddressSpaceUtils.deleteAddressSpaceAndWait(apiClient, kube, addrSpace, logCollector);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                apiClient.close();
            } catch (MalformedURLException e) {
                log.error("AddressApiClient wasn't initialized properly!");
                e.printStackTrace();
            }
            if(IoTUtils.isIoTInstalled(kube)) {
                try {
                    kube.getNonNamespacedIoTProjectClient().list().getItems().forEach(project -> {
                        log.info("iot project '{}' will be removed", project.getMetadata().getName());
                        String projectNamespace = project.getMetadata().getNamespace();
                        try (AddressApiClient addressApiClient = new AddressApiClient(kube, projectNamespace)) {
                            IoTUtils.deleteIoTProjectAndWait(kube, project, addressApiClient);
                        } catch ( Exception e ) {
                            e.printStackTrace();
                        }
                    });
                    var iotConfigClient = kube.getIoTConfigClient();
                    iotConfigClient.list().getItems().forEach(config -> {
                        log.info("iot config '{}' will be removed", config.getMetadata().getName());
                        try {
                            IoTUtils.deleteIoTConfigAndWait(kube, config);
                        } catch ( Exception e ) {
                            e.printStackTrace();
                        }
                    });
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            log.warn("Remove address spaces when test run finished - SKIPPED!");
        }
        TimeMeasuringSystem.printAndSaveResults();
    }
}
