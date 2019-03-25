/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import java.nio.ByteBuffer;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectBuilder;
import io.enmasse.systemtest.CertBundle;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.utils.CertificateUtils;
import io.enmasse.systemtest.utils.IoTUtils;

public abstract class IoTTestBaseWithShared extends IoTTestBase{

    protected static Logger log = CustomLogger.getLogger();

    protected IoTConfig sharedConfig;
    protected IoTProject sharedProject;

    @BeforeEach
    public void setUpSharedIoTProject() throws Exception {
        if( sharedConfig == null ) {
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
                    .withCertificate(ByteBuffer.wrap(Base64.getDecoder().decode(certBundle.getCert())))
                    .withKey(ByteBuffer.wrap(Base64.getDecoder().decode(certBundle.getKey())))
                    .endKeyCertificateStrategy()
                    .endEndpoint()
                    .endMqtt()
                    .endAdapters()
                    .endSpec()
                    .build();

            createIoTConfig(sharedConfig);
        }

        if( sharedProject == null ) {
            sharedProject = new IoTProjectBuilder()
                    .withNewMetadata()
                    .withName("shared-iot-project")
                    .endMetadata()
                    .withNewSpec()
                    .withNewDownstreamStrategy()
                    .withNewManagedStrategy()
                    .withNewAddressSpace()
                    .withName("shard-address-space")
                    .withPlan("standard-unlimited")
                    .withType("standard")
                    .endAddressSpace()
                    .withNewAddresses()
                    .withNewTelemetry()
                    .withPlan("standard-small-anycast")
                    .withType("anycast")
                    .endTelemetry()
                    .withNewEvent()
                    .withPlan("standard-small-queue")
                    .withType("queue")
                    .endEvent()
                    .withNewCommand()
                    .withPlan("standard-small-anycast")
                    .withType("anycast")
                    .endCommand()
                    .endAddresses()
                    .endManagedStrategy()
                    .endDownstreamStrategy()
                    .endSpec()
                    .build();

            createIoTProject(sharedProject);
        }

    }

    @AfterEach
    public void tearDownSharedIoTProject(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            if (!environment.skipCleanup()) {
                log.info("Shared IoTProject will be removed");
                if(iotProjectApiClient.existsIoTProject(sharedProject.getMetadata().getName())) {
                    IoTUtils.deleteIoTProjectAndWait(kubernetes, iotProjectApiClient, sharedProject, addressApiClient);
                } else {
                    log.info("IoTProject '" + sharedProject.getMetadata().getName() + "' doesn't exists!");
                }
                sharedProject = null;
                log.info("Shared IoTConfig will be removed");
                if(iotConfigApiClient.existsIoTConfig(sharedConfig.getMetadata().getName())) {
                    iotConfigApiClient.deleteIoTConfig(sharedConfig.getMetadata().getName());
                } else {
                    log.info("IoTConfig '" + sharedConfig.getMetadata().getName() + "' doesn't exists!");
                }
                sharedConfig = null;
            } else {
                log.warn("Remove shared iotproject when test failed - SKIPPED!");
            }
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
