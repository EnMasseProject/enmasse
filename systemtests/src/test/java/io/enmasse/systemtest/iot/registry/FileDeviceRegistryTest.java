/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.registry;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.CertBundle;
import io.enmasse.systemtest.utils.CertificateUtils;

import java.nio.ByteBuffer;

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
                .withNewDeviceRegistry()
                .withNewFile()
                .endFile()
                .endDeviceRegistry()
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

}
