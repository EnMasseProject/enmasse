/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.manager;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.iot.DefaultDeviceRegistry;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.enmasse.systemtest.bases.iot.ITestIoTBase.IOT_PROJECT_NAMESPACE;
import static io.enmasse.systemtest.utils.IoTUtils.createIoTConfig;
import static io.enmasse.systemtest.utils.IoTUtils.createIoTProject;


public class SharedIoTManager extends ResourceManager {

    private static final Logger LOGGER = CustomLogger.getLogger();
    private static SharedIoTManager instance;
    protected AmqpClientFactory amqpClientFactory = null;
    protected MqttClientFactory mqttClientFactory = null;
    private IoTProject sharedIoTProject = null;
    private IoTConfig sharedIoTConfig = null;
    private AmqpClient amqpClient;

    private SharedIoTManager() {
    }

    public static synchronized SharedIoTManager getInstance() {
        if (instance == null) {
            instance = new SharedIoTManager();
        }
        return instance;
    }

    @Override
    public AddressSpace getSharedAddressSpace() {
        if (sharedIoTProject == null) return null;
        String addSpaceName = sharedIoTProject.getSpec().getDownstreamStrategy().getManagedStrategy().getAddressSpace().getName();
        return kubernetes.getAddressSpaceClient(sharedIoTProject.getMetadata().getNamespace()).withName(addSpaceName).get();
    }

    @Override
    public void tearDown(ExtensionContext context) throws Exception {
        closeAmqpFactory();
        closeMqttFactory();
        if (environment.skipCleanup()) {
            LOGGER.info("Skip cleanup is set, no cleanup process");
        } else {
            List<Throwable> exceptions = new ArrayList<>();
            try {
                tearDownSharedIoTProject();
            } catch(Exception | AssertionFailedError e) {
                LOGGER.error("Error tearing down shared iotproject", e);
                exceptions.add(e);
            }
            try {
                tearDownSharedIoTConfig();
            } catch(Exception | AssertionFailedError e) {
                LOGGER.error("Error tearing down shared iotconfig", e);
                exceptions.add(e);
            }
            try {
                tearDownInfinispan(context);
            } catch(Exception | AssertionFailedError e) {
                LOGGER.error("Error tearing down infinispan", e);
                exceptions.add(e);
            }
            if (!exceptions.isEmpty()) {
                throw new IllegalStateException("Errors have been produced during tear down");
            }
        }
    }

    private void tearDownInfinispan(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) {
            Path path = TestUtils.getFailedTestLogsPath(context);
            SystemtestsKubernetesApps.collectInfinispanServerLogs(path);
        }
        SystemtestsKubernetesApps.deleteInfinispanServer();
    }

    private void tearDownSharedIoTProject() throws Exception {
        if (sharedIoTProject != null) {
            LOGGER.info("Shared IoTProject will be removed");
            var iotProjectApiClient = kubernetes.getIoTProjectClient(sharedIoTProject.getMetadata().getNamespace());
            if (iotProjectApiClient.withName(sharedIoTProject.getMetadata().getName()).get() != null) {
                try {
                    IoTUtils.deleteIoTProjectAndWait(kubernetes, sharedIoTProject);
                } finally {
                    sharedIoTProject = null;
                }
            } else {
                LOGGER.info("IoTProject '{}' doesn't exists!", sharedIoTProject.getMetadata().getName());
            }
        }
    }

    public void tearDownSharedIoTConfig() throws Exception {
        if (sharedIoTConfig != null) {
            LOGGER.info("Shared IoTConfig will be removed");
            var iotConfigApiClient = kubernetes.getIoTConfigClient();
            if (iotConfigApiClient.withName(sharedIoTConfig.getMetadata().getName()).get() != null) {
                try {
                    IoTUtils.deleteIoTConfigAndWait(kubernetes, sharedIoTConfig);
                } finally {
                    sharedIoTConfig = null;
                }
            } else {
                LOGGER.info("IoTConfig '{}' doesn't exists!", sharedIoTConfig.getMetadata().getName());
            }
        }
    }

    void initFactories(UserCredentials credentials) {
        amqpClientFactory = new AmqpClientFactory(getSharedAddressSpace(), credentials);
        mqttClientFactory = new MqttClientFactory(getSharedAddressSpace(), credentials);
    }

    @Override
    public void setup() throws Exception {
        kubernetes.createNamespace(IOT_PROJECT_NAMESPACE);

        UserCredentials credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        if (sharedIoTConfig == null) {
            createNewIoTConfig();
        }

        if (sharedIoTProject == null) {
            sharedIoTProject = IoTUtils.getBasicIoTProjectObject("shared-iot-project", defaultAddSpaceIdentifier, IOT_PROJECT_NAMESPACE, addressSpacePlan);
            createIoTProject(sharedIoTProject);
        }
        initFactories(credentials);
        createOrUpdateUser(getSharedAddressSpace(), credentials);
        this.amqpClient = amqpClientFactory.createQueueClient();
    }

    private void createNewIoTConfig() throws Exception {
        CertBundle certBundle = CertificateUtils.createCertBundle();
        sharedIoTConfig = new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withNewServices()
                .withDeviceRegistry(DefaultDeviceRegistry.newInfinispanBased())
                .endServices()
                .withNewAdapters()
                .withNewMqtt()
                .withNewEndpoint()
                .withNewKeyCertificateStrategy()
                .withCertificate(ByteBuffer.wrap(certBundle.getCert().getBytes()))
                .withKey(ByteBuffer.wrap(certBundle.getKey().getBytes()))
                .endKeyCertificateStrategy()
                .endEndpoint()
                .endMqtt()
                .endAdapters()
                .endSpec()
                .build();
        createIoTConfig(sharedIoTConfig);
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

    public IoTProject getSharedIoTProject() {
        return sharedIoTProject;
    }

    public IoTConfig getSharedIoTConfig() {
        return sharedIoTConfig;
    }

    public String getTenantId() {
        return IoTUtils.getTenantId(sharedIoTProject);
    }

    public AmqpClient getAmqpClient() {
        return amqpClient;
    }

    public void closeAmqpFactory() throws Exception {
        if (amqpClientFactory != null) {
            amqpClientFactory.close();
            amqpClientFactory = null;
        }
    }

    public void closeMqttFactory() {
        if (mqttClientFactory != null) {
            mqttClientFactory.close();
            mqttClientFactory = null;
        }
    }
}
