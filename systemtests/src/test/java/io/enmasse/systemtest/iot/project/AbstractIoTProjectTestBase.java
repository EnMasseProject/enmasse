/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.project;



import java.nio.ByteBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.bases.DefaultDeviceRegistry;
import io.enmasse.systemtest.bases.IoTTestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.iot.CredentialsRegistryClient;
import io.enmasse.systemtest.iot.DeviceRegistryClient;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.utils.CertificateUtils;

public abstract class AbstractIoTProjectTestBase extends IoTTestBase implements ITestIsolatedStandard {

    protected DeviceRegistryClient registryClient;
    protected CredentialsRegistryClient credentialsClient;

    @BeforeEach
    void initEnv() throws Exception {
        CertBundle certBundle = CertificateUtils.createCertBundle();
        IoTConfig iotConfig = new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
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

        createIoTConfig(iotConfig);

        final Endpoint deviceRegistryEndpoint = kubernetes.getExternalEndpoint("device-registry");
        registryClient = new DeviceRegistryClient(kubernetes, deviceRegistryEndpoint);
        credentialsClient = new CredentialsRegistryClient(kubernetes, deviceRegistryEndpoint);
    }

    @AfterEach
    void cleanEnv(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            logCollector.collectHttpAdapterQdrProxyState();
        }

        SystemtestsKubernetesApps.deleteInfinispanServer(kubernetes.getInfraNamespace());
    }

}
