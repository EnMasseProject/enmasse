/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.manager;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.iot.ITestIoTBase;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.UUID;

import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.deviceRegistry;
import static io.enmasse.systemtest.utils.IoTUtils.createIoTConfig;
import static io.enmasse.systemtest.utils.IoTUtils.createIoTProject;


public class SharedIoTManager extends ResourceManager implements ITestIoTBase {

    private static final Logger LOGGER = CustomLogger.getLogger();
    private static SharedIoTManager instance;
    protected AmqpClientFactory amqpClientFactory = null;
    protected MqttClientFactory mqttClientFactory = null;
    private IoTProject sharedIoTProject = null;
    private IoTConfig sharedIoTConfig = null;
    private Endpoint deviceRegistryEndpoint;
    private Endpoint httpAdapterEndpoint;
    private DeviceRegistryClient client;

    private static final String DEFAULT_ADDRESS_TEMPLATE = "iot-shared-";

    public static synchronized SharedIoTManager getInstance() {
        if (instance == null) {
            instance = new SharedIoTManager();
        }
        return instance;
    }

    @Override
    public AddressSpace getSharedAddressSpace() {
        //TODO: DODELEJ PICO
    }

    //TODO: PREPSAT!
    @Override
    public void tearDown(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            if (!TestBase.environment.skipCleanup()) {
                if (sharedProject != null) {
                    log.info("Shared IoTProject will be removed");
                    var iotProjectApiClient = TestBase.kubernetes.getIoTProjectClient(sharedProject.getMetadata().getNamespace());
                    if (iotProjectApiClient.withName(sharedProject.getMetadata().getName()).get() != null) {
                        IoTUtils.deleteIoTProjectAndWait(TestBase.kubernetes, sharedProject);
                    } else {
                        log.info("IoTProject '{}' doesn't exists!", sharedProject.getMetadata().getName());
                    }
                    sharedProject = null;
                }
                if (sharedConfig != null) {
                    log.info("Shared IoTConfig will be removed");
                    var iotConfigApiClient = TestBase.kubernetes.getIoTConfigClient();
                    if (iotConfigApiClient.withName(sharedConfig.getMetadata().getName()).get() != null) {
                        IoTUtils.deleteIoTConfigAndWait(TestBase.kubernetes, sharedConfig);
                    } else {
                        log.info("IoTConfig '{}' doesn't exists!", sharedConfig.getMetadata().getName());
                    }
                }
                log.info("Infinispan server will be removed");
                SystemtestsKubernetesApps.deleteInfinispanServer(TestBase.kubernetes.getInfraNamespace());
                sharedConfig = null;
            } else {
                log.warn("Remove shared iotproject when test failed - SKIPPED!");
            }
        }
        sharedConfig = null;
        sharedProject = null;
    }

    @Override
    void initFactories(AddressSpace addressSpace) {
        amqpClientFactory = new AmqpClientFactory(getSharedAddressSpace(), defaultCredentials);
        mqttClientFactory = new MqttClientFactory(getSharedAddressSpace(), defaultCredentials);
    }

    @Override
    public void setup() {
        initFactories(getSharedAddressSpace());
    }
    
    public void createSharedIoTEnv() throws Exception {
        //TODO: ASI MOVE NEKAM DO WATCHERU
        this.credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        if (sharedIoTConfig == null) {
            CertBundle certBundle = CertificateUtils.createCertBundle();
            sharedIoTConfig = new IoTConfigBuilder()
                    .withNewMetadata()
                    .withName("default")
                    .endMetadata()
                    .withNewSpec()
                    .withNewServices()
                    .withDeviceRegistry(deviceRegistry())
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

        if (sharedIoTProject == null) {
            sharedIoTProject = IoTUtils.getBasicIoTProjectObject("shared-iot-project",
                    getSharedAddressSpace().getMetadata().getName(), iotProjectNamespace);
            createIoTProject(sharedIoTProject);
        }

        //TODO: CO TO KURVA JE
        this.iotAmqpClient = this.iotAmqpClientFactory.createQueueClient();
    }

    public void createDeviceRegistrySharedEnv() throws Exception {
        if (sharedIoTConfig == null) {
            sharedIoTConfig = provideIoTConfig();
            createIoTConfig(sharedIoTConfig);
        }
        if (sharedIoTProject == null) {
            sharedIoTProject = IoTUtils.getBasicIoTProjectObject(DEVICE_REGISTRY_TEST_PROJECT, DEVICE_REGISTRY_TEST_ADDRESSSPACE, this.iotProjectNamespace);
            createIoTProject(sharedIoTProject);
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

        this.credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        createOrUpdateUser(resourcesManager.getAddressSpace(this.iotProjectNamespace, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
        this.iotAmqpClientFactory = new AmqpClientFactory(resourcesManager.getAddressSpace(this.iotProjectNamespace, DEVICE_REGISTRY_TEST_ADDRESSSPACE), this.credentials);
    }
    
    public AmqpClientFactory getAmqpClientFactory() {
        return amqpClientFactory;
    }

    @Override
    public void setAmqpClientFactory(AmqpClientFactory amqpClientFactory) {
        this.amqpClientFactory = amqpClientFactory;
    }

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

    public DeviceRegistryClient getDevClient() {
        return client;
    }

    //TODO: CLOSE FACTORIES IMPLEMENT
}
