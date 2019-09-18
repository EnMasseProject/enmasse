/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.registry;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.certs.CertBundle;
import io.enmasse.systemtest.utils.CertificateUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static io.enmasse.systemtest.bases.DefaultDeviceRegistry.newFileBased;

public class FileDeviceRegistryTest extends DeviceRegistryTestBase {

    @Override
    protected IoTConfig provideIoTConfig() throws Exception {
        CertBundle certBundle = CertificateUtils.createCertBundle();
        return new IoTConfigBuilder()
                .withNewMetadata()
                .withName("default")
                .endMetadata()
                .withNewSpec()
                .withNewServices()
                .withDeviceRegistry(newFileBased())
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
    }

    @Test
    public void testCorrectTypeDeployed () {
        assertCorrectRegistryType("file");
    }

    @Disabled("Not supported by file based device registry")
    @Override
    void testCreateForNonExistingTenantFails() throws Exception {
        super.testCreateForNonExistingTenantFails();
    }
}
