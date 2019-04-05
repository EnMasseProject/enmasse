/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.apiclients.IoTConfigApiClient;
import io.enmasse.systemtest.apiclients.IoTProjectApiClient;
import io.enmasse.systemtest.apiclients.UserApiClient;
import io.enmasse.systemtest.timemeasuring.SystemtestsOperation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import io.enmasse.systemtest.utils.IoTUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public abstract class IoTTestBase extends TestBase {

    protected static final String IOT_ADDRESS_EVENT = "event";
    protected static final String IOT_ADDRESS_TELEMETRY = "telemetry";
    protected static final String IOT_ADDRESS_CONTROL = "control";

    protected static Logger log = CustomLogger.getLogger();

    private List<IoTConfig> iotConfigs = new ArrayList<>();
    private List<IoTProject> iotProjects = new ArrayList<>();

    protected String iotProjectNamespace = "iot-project-ns";
    protected IoTProjectApiClient iotProjectApiClient;
    protected IoTConfigApiClient iotConfigApiClient;

    @BeforeEach
    public void setupIoT() throws Exception {
        if (iotProjectApiClient == null) {
            if (!kubernetes.namespaceExists(iotProjectNamespace)) {
                kubernetes.createNamespace(iotProjectNamespace);
            }
            iotProjectApiClient = new IoTProjectApiClient(kubernetes, iotProjectNamespace);
            //additional clients that need to query against the namespace where iotprojects are created
            addressApiClient = new AddressApiClient(kubernetes, iotProjectNamespace);
            setUserApiClient(new UserApiClient(kubernetes, iotProjectNamespace));
        }
        if (iotConfigApiClient == null) {
            iotConfigApiClient = new IoTConfigApiClient(kubernetes);
        }
    }

    @AfterEach
    public void teardownIoT() throws Exception {
        try {
            if (!environment.skipCleanup()) {
                //FIXME maybe collect logs of iot related pods?
                log.info("All IoTProjects will be removed");
                for (IoTProject project : iotProjects) {
                    if (iotProjectApiClient.existsIoTProject(project.getMetadata().getName())) {
                        IoTUtils.deleteIoTProjectAndWait(kubernetes, iotProjectApiClient, project, addressApiClient);
                    } else {
                        log.info("IoTProject '" + project.getMetadata().getName() + "' doesn't exists!");
                    }
                }
                iotProjects.clear();
                log.info("All IoTConfigs will be removed");
                for (IoTConfig config : iotConfigs) {
                    if (iotConfigApiClient.existsIoTConfig(config.getMetadata().getName())) {
                        iotConfigApiClient.deleteIoTConfig(config.getMetadata().getName());
                    } else {
                        log.info("IoTConfig '" + config.getMetadata().getName() + "' doesn't exists!");
                    }
                }
                iotConfigs.clear();
            } else {
                log.warn("Clean IoT environment in tear down - SKIPPED!");
            }
        } catch (Exception e) {
            log.error("Error tearing down iot test: {}", e.getMessage());
            throw e;
        }
    }

    public IoTConfig getSharedIoTConfig() {
        return null;
    }

    public IoTProject getSharedIoTProject() {
        return null;
    }

    protected void createIoTConfig(IoTConfig config) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_IOT_CONFIG);
        if (iotConfigApiClient.existsIoTConfig(config.getMetadata().getName())) {
            log.info("iot config {} already exists", config.getMetadata().getName());
        } else {
            log.info("iot config {} will be created", config.getMetadata().getName());
            iotConfigApiClient.createIoTConfig(config);
            if (!config.equals(getSharedIoTConfig())) {
                iotConfigs.add(config);
            }
        }
        IoTUtils.waitForIoTConfigReady(iotConfigApiClient, config);
        IoTUtils.syncIoTConfig(config, iotConfigApiClient);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    protected void createIoTProject(IoTProject project) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_IOT_PROJECT);
        if (iotProjectApiClient.existsIoTProject(project.getMetadata().getName())) {
            log.info("iot project {} already exists", project.getMetadata().getName());
        } else {
            log.info("iot project {} will be created", project.getMetadata().getName());
            iotProjectApiClient.createIoTProject(project);
            if (!project.equals(getSharedIoTProject())) {
                iotProjects.add(project);
            }
        }
        IoTUtils.waitForIoTProjectReady(iotProjectApiClient, project);
        IoTUtils.syncIoTProject(project, iotProjectApiClient);
        TimeMeasuringSystem.stopOperation(operationID);
    }

}
