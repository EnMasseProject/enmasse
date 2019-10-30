/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.manager;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.IoTUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static io.enmasse.systemtest.bases.iot.ITestIoTBase.IOT_PROJECT_NAMESPACE;

public class IsolatedIoTManager extends ResourceManager {
    private static final Logger LOGGER = CustomLogger.getLogger();
    private AmqpClientFactory amqpClientFactory;
    private MqttClientFactory mqttClientFactory;
    private List<IoTProject> ioTProjects;
    private List<IoTConfig> ioTConfigs;
    private static IsolatedIoTManager instance = null;
    private UserCredentials defaultCredentials = ENVIRONMENT.getDefaultCredentials();

    private IsolatedIoTManager() {
        ioTProjects = new ArrayList<>();
        ioTConfigs = new ArrayList<>();
    }

    public static synchronized IsolatedIoTManager getInstance() {
        if (instance == null) {
            instance = new IsolatedIoTManager();
        }
        return instance;
    }

    private void initFactories(AddressSpace addressSpace) {
        amqpClientFactory = new AmqpClientFactory(addressSpace, defaultCredentials);
        mqttClientFactory = new MqttClientFactory(addressSpace, defaultCredentials);
    }

    private void initFactories(IoTProject project) {
        String addSpaceName = project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
        this.initFactories(KUBERNETES.getAddressSpaceClient(project.getMetadata()
                .getNamespace()).withName(addSpaceName).get());
    }

    @Override
    public void setup() {
        if (!KUBERNETES.namespaceExists(IOT_PROJECT_NAMESPACE)) {
            KUBERNETES.createNamespace(IOT_PROJECT_NAMESPACE);
        }
    }

    @Override
    public void tearDown(ExtensionContext context) throws Exception {
        if (ENVIRONMENT.skipCleanup()) {
            LOGGER.info("Skip cleanup is set, no cleanup process");
        } else {
            try {
                tearDownProjects();
                tearDownConfigs();
                SystemtestsKubernetesApps.deleteInfinispanServer(KUBERNETES.getInfraNamespace());
            } catch (Exception e) {
                LOGGER.error("Error tearing down iot test: {}", e.getMessage());
                throw e;
            }
        }
    }

    private void tearDownProjects() throws Exception {
        LOGGER.info("All IoTProjects will be removed");
        for (IoTProject project : ioTProjects) {
            var iotProjectApiClient = KUBERNETES.getIoTProjectClient(project.getMetadata().getNamespace());
            if (iotProjectApiClient.withName(project.getMetadata().getName()).get() != null) {
                IoTUtils.deleteIoTProjectAndWait(KUBERNETES, project);
            } else {
                LOGGER.info("IoTProject '{}' doesn't exists!", project.getMetadata().getName());
            }
        }
        ioTProjects.clear();
    }

    private void tearDownConfigs() throws Exception {
        // delete configs
        LOGGER.info("All IoTConfigs will be removed");
        var iotConfigApiClient = KUBERNETES.getIoTConfigClient();
        for (IoTConfig config : ioTConfigs) {
            if (iotConfigApiClient.withName(config.getMetadata().getName()).get() != null) {
                IoTUtils.deleteIoTConfigAndWait(KUBERNETES, config);
            } else {
                LOGGER.info("IoTConfig '{}' doesn't exists!", config.getMetadata().getName());
            }
        }
        ioTConfigs.clear();
    }

    @Override
    public AmqpClientFactory getAmqpClientFactory() {
        return amqpClientFactory;
    }

    @Override
    public void setAmqpClientFactory(AmqpClientFactory amqpClientFactory) {
        this.amqpClientFactory = amqpClientFactory;
    }

    @Override
    public MqttClientFactory getMqttClientFactory() {
        return mqttClientFactory;
    }

    @Override
    public void setMqttClientFactory(MqttClientFactory mqttClientFactory) {
        this.mqttClientFactory = mqttClientFactory;
    }

    public void createIoTProject(IoTProject project) throws Exception {
        ioTProjects.add(project);
        IoTUtils.createIoTProject(project);
        initFactories(project);
    }

    public void createIoTConfig(IoTConfig ioTConfig) throws Exception {
        ioTConfigs.add(ioTConfig);
        IoTUtils.createIoTConfig(ioTConfig);
    }

    public String getTenantId() {
        return IoTUtils.getTenantId(ioTProjects.get(0));
    }


}
