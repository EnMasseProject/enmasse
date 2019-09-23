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
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.IoTUtils;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.List;

public class IsolatedIoTManager extends ResourceManager implements ITestIoTIsolated {

    protected AmqpClientFactory amqpClientFactory;
    protected MqttClientFactory mqttClientFactory;
    protected List<IoTProject> ioTProjects = new ArrayList<>();
    protected List<IoTConfig> ioTConfigs = new ArrayList<>();
    private static IsolatedIoTManager instance = null;
    private Kubernetes kubernetes = Kubernetes.getInstance();

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

    @Override
    public void setup() throws Exception {
        if (!kubernetes.namespaceExists(iotProjectNamespace)) {
            kubernetes.createNamespace(iotProjectNamespace);
        }
        initFactories(null);
    }

    @Override
    public void tearDown(ExtensionContext context) throws Exception {
        try {
            tearDownProjects();
            tearDownConfigs();
        } catch (Exception e) {
            LOGGER.error("Error tearing down iot test: {}", e.getMessage());
            throw e;
        }
    }

    private void tearDownProjects() throws Exception {
        LOGGER.info("All IoTProjects will be removed");
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
        LOGGER.info("All IoTConfigs will be removed");
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
        return null;
    }

    @Override
    public void setAmqpClientFactory(AmqpClientFactory amqpClientFactory) {

    }

    @Override
    public MqttClientFactory getMqttClientFactory() {
        return null;
    }

    @Override
    public void setMqttClientFactory(MqttClientFactory mqttClientFactory) {

    }
}
