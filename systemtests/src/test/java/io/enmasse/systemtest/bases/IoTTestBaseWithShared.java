/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.bases.isolated.ITestBaseIsolated;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.CommonResourcesManager;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.bases.DefaultDeviceRegistry.deviceRegistry;

public abstract class IoTTestBaseWithShared extends IoTTestBase implements ITestBaseIsolated {

    private final static Logger log = CustomLogger.getLogger();

    private final String addressSpace = "shared-address-space";

    protected IoTConfig sharedConfig;
    protected IoTProject sharedProject;
    protected AmqpClientFactory iotAmqpClientFactory;
    protected AmqpClient iotAmqpClient;
    private UserCredentials credentials;

    @BeforeEach
    public void setUpSharedIoTProject() throws Exception {

        this.credentials = new UserCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        if (sharedConfig == null) {
            CertBundle certBundle = CertificateUtils.createCertBundle();
            sharedConfig = new IoTConfigBuilder()
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

            createIoTConfig(sharedConfig);
        }

        if (sharedProject == null) {
            sharedProject = IoTUtils.getBasicIoTProjectObject("shared-iot-project", this.addressSpace, this.iotProjectNamespace);
            createIoTProject(sharedProject);
        }

        this.iotAmqpClientFactory = createAmqpClientFactory();
        this.iotAmqpClient = this.iotAmqpClientFactory.createQueueClient();
    }

    public AddressSpace getAddressSpace() {
        return resourcesManager.getAddressSpace(this.iotProjectNamespace, this.addressSpace);
    }

    /**
     * Create a new {@link AmqpClientFactory} for the IoTProject's address space.
     */
    private AmqpClientFactory createAmqpClientFactory() throws Exception {

        resourcesManager.createOrUpdateUser(resourcesManager.getAddressSpace(this.iotProjectNamespace, this.addressSpace), this.credentials);
        return new AmqpClientFactory(resourcesManager.getAddressSpace(this.iotProjectNamespace, this.addressSpace), this.credentials);

    }

    @AfterEach
    public void tearDownSharedIoTProject(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            if (!environment.skipCleanup()) {
                if (sharedProject != null) {
                    log.info("Shared IoTProject will be removed");
                    var iotProjectApiClient = kubernetes.getIoTProjectClient(sharedProject.getMetadata().getNamespace());
                    if (iotProjectApiClient.withName(sharedProject.getMetadata().getName()).get() != null) {
                        IoTUtils.deleteIoTProjectAndWait(kubernetes, sharedProject);
                    } else {
                        log.info("IoTProject '{}' doesn't exists!", sharedProject.getMetadata().getName());
                    }
                    sharedProject = null;
                }
                if (sharedConfig != null) {
                    log.info("Shared IoTConfig will be removed");
                    var iotConfigApiClient = kubernetes.getIoTConfigClient();
                    if (iotConfigApiClient.withName(sharedConfig.getMetadata().getName()).get() != null) {
                        IoTUtils.deleteIoTConfigAndWait(kubernetes, sharedConfig);
                    } else {
                        log.info("IoTConfig '{}' doesn't exists!", sharedConfig.getMetadata().getName());
                    }
                }
                log.info("Infinispan server will be removed");
                SystemtestsKubernetesApps.deleteInfinispanServer(kubernetes.getInfraNamespace());
                sharedConfig = null;
            } else {
                log.warn("Remove shared iotproject when test failed - SKIPPED!");
            }
        }
        sharedConfig = null;
        sharedProject = null;
    }

    @AfterEach
    public void closeIoTAmqpClient() throws Exception {
        if (this.iotAmqpClient != null) {
            this.iotAmqpClient.close();
            this.iotAmqpClient = null;
        }
    }

    @AfterEach
    public void closeIoTAmqpClientFactory() throws Exception {
        if (this.iotAmqpClientFactory != null) {
            this.iotAmqpClientFactory.close();
            this.iotAmqpClientFactory = null;
        }
    }

    @Override
    public IoTConfig getSharedIoTConfig() {
        return sharedConfig;
    }

    @Override
    public IoTProject getSharedIoTProject() {
        return sharedProject;
    }

}
