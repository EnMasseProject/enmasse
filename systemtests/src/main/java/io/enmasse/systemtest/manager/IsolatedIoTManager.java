/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.manager;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.iot.ITestIoTIsolated;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.IoTUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static io.enmasse.systemtest.bases.iot.ITestIoTBase.iotProjectNamespace;

public class IsolatedIoTManager extends ResourceManager {

    private Logger LOGGER = CustomLogger.getLogger();
    protected AmqpClientFactory amqpClientFactory;
    protected MqttClientFactory mqttClientFactory;
    protected List<IoTProject> ioTProjects;
    protected List<IoTConfig> ioTConfigs;
    private static IsolatedIoTManager instance = null;
    private Kubernetes kubernetes = Kubernetes.getInstance();

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

    @Override
    public void initFactories(AddressSpace addressSpace) {
        amqpClientFactory = new AmqpClientFactory(addressSpace, defaultCredentials);
        mqttClientFactory = new MqttClientFactory(addressSpace, defaultCredentials);
    }

    public void initFactories(IoTProject project) {
        String addSpaceName = project.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
        this.initFactories(kubernetes.getAddressSpaceClient(project.getMetadata()
                .getNamespace()).withName(addSpaceName).get());
    }

    @Override
    public void setup() throws Exception {
        LOGGER.warn("NAMESPACE PICO");
        if (!kubernetes.namespaceExists(iotProjectNamespace)) {
            LOGGER.warn("NAMESPACE NEEXISTUJE BUDE VYTVORENY");
            kubernetes.createNamespace(iotProjectNamespace);
        }
    }

    @Override
    public void tearDown(ExtensionContext context) throws Exception {
        try {
            tearDownProjects();
            tearDownConfigs();
            kubernetes.deleteNamespace(iotProjectNamespace);
        } catch (Exception e) {
            LOGGER.error("Error tearing down iot test: {}", e.getMessage());
            throw e;
        }
    }

    private void tearDownProjects() throws Exception {
        LOGGER.info("All IoTProjects will be removed");
        ioTProjects.forEach(project -> LOGGER.warn("PROJECT FOR DELETION {}", project));
        for (IoTProject project : ioTProjects) {
            var iotProjectApiClient = kubernetes.getIoTProjectClient(project.getMetadata().getNamespace());
            if (iotProjectApiClient.withName(project.getMetadata().getName()).get() != null) {
                IoTUtils.deleteIoTProjectAndWait(kubernetes, project);
            } else {
                LOGGER.info("IoTProject '{}' doesn't exists!", project.getMetadata().getName());
            }
        }
        ioTProjects.clear();
    }

    private void tearDownConfigs() throws Exception {
        // delete configs
        LOGGER.warn("MANAGER: {}", this);
        LOGGER.info("All IoTConfigs will be removed");
        ioTConfigs.forEach(confi -> LOGGER.warn("PROJECT FOR DELETION {}", confi));
        var iotConfigApiClient = kubernetes.getIoTConfigClient();
        for (IoTConfig config : ioTConfigs) {
            if (iotConfigApiClient.withName(config.getMetadata().getName()).get() != null) {
                IoTUtils.deleteIoTConfigAndWait(kubernetes, config);
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
        LOGGER.warn("MANAGER: {}", this);
        ioTProjects.add(project);
        IoTUtils.createIoTProject(project);
        initFactories(project);
        ioTProjects.forEach(project1 -> LOGGER.warn("PROJECTS {}", project1));
    }

    public void deleteIoTProject(IoTProject project) throws Exception {
        IoTUtils.deleteIoTProjectAndWait(kubernetes, project);
        ioTProjects.remove(project);
    }

    public void createIoTConfig(IoTConfig ioTConfig) throws Exception {
        ioTConfigs.add(ioTConfig);
        IoTUtils.createIoTConfig(ioTConfig);
        ioTConfigs.forEach(ioTConfig1 -> LOGGER.warn("CONFIGS {}", ioTConfig1));
    }

    public void deleteIoTConfig(IoTConfig ioTConfig) throws Exception {
        IoTUtils.deleteIoTConfigAndWait(kubernetes, ioTConfig);
        ioTConfigs.add(ioTConfig);
    }

    public List<IoTProject> getIoTProjects() {
        return ioTProjects;
    }
    public String getTenantID() {
        return IoTUtils.getTenantID(ioTProjects.get(0));
    }
    public List<IoTConfig> getIoTConfigs() {
        return ioTConfigs;
    }
}
