/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.manager;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.UUID;

import static io.enmasse.systemtest.bases.iot.ITestIoTBase.iotProjectNamespace;
import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.deviceRegistry;
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
        AddressSpace addressSpace = kubernetes.getAddressSpaceClient(sharedIoTProject.getMetadata().getNamespace()).withName(addSpaceName).get();
        return addressSpace;
    }

    @Override
    public void tearDown(ExtensionContext context) throws Exception {
        closeFactories();
        if (sharedIoTProject != null) {
            LOGGER.info("Shared IoTProject will be removed");
            var iotProjectApiClient = kubernetes.getIoTProjectClient(sharedIoTProject.getMetadata().getNamespace());
            if (iotProjectApiClient.withName(sharedIoTProject.getMetadata().getName()).get() != null) {
                IoTUtils.deleteIoTProjectAndWait(kubernetes, sharedIoTProject);
                sharedIoTProject = null;
            } else {
                LOGGER.info("IoTProject '{}' doesn't exists!", sharedIoTProject.getMetadata().getName());
            }
        }
        tearDownSharedIoTConfig();
        SystemtestsKubernetesApps.deleteInfinispanServer(kubernetes.getInfraNamespace());
    }

    public void tearDownSharedIoTConfig() throws Exception {
        if (sharedIoTConfig != null) {
            LOGGER.info("Shared IoTConfig will be removed");
            var iotConfigApiClient = kubernetes.getIoTConfigClient();
            if (iotConfigApiClient.withName(sharedIoTConfig.getMetadata().getName()).get() != null) {
                IoTUtils.deleteIoTConfigAndWait(kubernetes, sharedIoTConfig);
                sharedIoTConfig = null;
            } else {
                LOGGER.info("IoTConfig '{}' doesn't exists!", sharedIoTConfig.getMetadata().getName());
            }
        }
    }

    @Override
    void initFactories(AddressSpace addressSpace) {
        amqpClientFactory = new AmqpClientFactory(getSharedAddressSpace(), defaultCredentials);
        mqttClientFactory = new MqttClientFactory(getSharedAddressSpace(), defaultCredentials);
    }

    @Override
    public void setup() {
        if (!kubernetes.namespaceExists(iotProjectNamespace)) {
            LOGGER.info("Namespace {} doesn't exists and will be created.");
            kubernetes.createNamespace(iotProjectNamespace);
        }
    }

    public void createSharedIoTEnv() throws Exception {
        Environment.getInstance().setDefaultCredentials(new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

        if (sharedIoTConfig == null) {
            createNewIoTConfig();
        }

        if (sharedIoTProject == null) {
            sharedIoTProject = IoTUtils.getBasicIoTProjectObject("shared-iot-project",
                    DEFAULT_ADD_SPACE_IDENTIFIER, iotProjectNamespace, ADDRESS_SPACE_PLAN);
            createIoTProject(sharedIoTProject);

        }

        initFactories(getSharedAddressSpace());
        this.amqpClient = amqpClientFactory.createQueueClient();
    }

/*    public void createDeviceRegistrySharedEnv() throws Exception {
        if (sharedIoTConfig == null) {
            createNewIoTConfig();
        }

        if (deviceRegistryEndpoint == null) {
            deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        }
        if (httpAdapterEndpoint == null) {
            httpAdapterEndpoint = kubernetes.getExternalEndpoint("iot-http-adapter");
        }
        if (client == null) {
            client = new DeviceRegistryClient(kubernetes, deviceRegistryEndpoint);
        }
        this.randomDeviceId = UUID.randomUUID().toString();

        Environment.getInstance().setDefaultCredentials(new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        createOrUpdateUser(getAddressSpace(iotProjectNamespace,
                getSharedAddressSpace().getMetadata().getName()), Environment.getInstance().getDefaultCredentials());
        this.amqpClientFactory = new AmqpClientFactory(getSharedAddressSpace(),
                Environment.getInstance().getDefaultCredentials());
        this.amqpClient = amqpClientFactory.createQueueClient();
    }*/

    private void createNewIoTConfig() throws Exception {
        CertBundle certBundle = CertificateUtils.createCertBundle();
        sharedIoTConfig = new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .endMetadata()
                .withNewSpec()
                .withNewServices()
                .withDeviceRegistry(deviceRegistry("file"))
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

    public String getTenantID() {
        return IoTUtils.getTenantID(sharedIoTProject);
    }

    public AmqpClient getAmqpClient() {
        return amqpClient;
    }

    public void closeFactories() throws Exception {
        if (amqpClientFactory != null) {
            amqpClientFactory.close();
            amqpClientFactory = null;
        }
        if (mqttClientFactory != null) {
            mqttClientFactory.close();
            mqttClientFactory = null;
        }
    }
}
