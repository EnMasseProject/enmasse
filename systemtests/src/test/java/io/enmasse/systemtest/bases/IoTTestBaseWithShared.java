/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.CertBundle;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.UUID;

public abstract class IoTTestBaseWithShared extends IoTTestBase {

    protected static Logger log = CustomLogger.getLogger();

    private final String addressSpace = "shared-address-space";

    protected IoTConfig sharedConfig;
    protected IoTProject sharedProject;

    private UserCredentials credentials;
    protected AmqpClientFactory iotAmqpClientFactory;

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
            sharedProject = IoTUtils.getBasicIoTProjectObject("shared-iot-project", this.addressSpace);
            createIoTProject(sharedProject);
        }

        this.iotAmqpClientFactory = createAmqpClientFactory();
    }

    public String getAddressSpace() {
        return this.addressSpace;
    }

    /**
     * Create a new {@link AmqpClientFactory} for the IoTProject's address space.
     */
    private AmqpClientFactory createAmqpClientFactory() throws Exception {

        getUserApiClient().createUser(this.addressSpace, this.credentials);
        return new AmqpClientFactory(kubernetes, Environment.getInstance(), getAddressSpace(this.addressSpace), this.credentials);

    }

    @AfterEach
    public void tearDownSharedIoTProject(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            if (!environment.skipCleanup()) {
                log.info("Shared IoTProject will be removed");
                if (iotProjectApiClient.existsIoTProject(sharedProject.getMetadata().getName())) {
                    IoTUtils.deleteIoTProjectAndWait(kubernetes, iotProjectApiClient, sharedProject, addressApiClient);
                } else {
                    log.info("IoTProject '" + sharedProject.getMetadata().getName() + "' doesn't exists!");
                }
                sharedProject = null;
                log.info("Shared IoTConfig will be removed");
                if (iotConfigApiClient.existsIoTConfig(sharedConfig.getMetadata().getName())) {
                    iotConfigApiClient.deleteIoTConfig(sharedConfig.getMetadata().getName());
                } else {
                    log.info("IoTConfig '" + sharedConfig.getMetadata().getName() + "' doesn't exists!");
                }
                sharedConfig = null;
            } else {
                log.warn("Remove shared iotproject when test failed - SKIPPED!");
            }
        }

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
