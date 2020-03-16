/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot.isolated.registry;

import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.newFileBased;
import static io.enmasse.systemtest.utils.IoTUtils.assertCorrectRegistryType;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.enmasse.iot.model.v1.IoTConfigBuilder;

class FileDeviceRegistryTest extends DeviceRegistryTest {

    @Override
    protected int tenantDoesNotExistCode() {
        return HttpURLConnection.HTTP_NOT_FOUND;
    }

    @Override
    protected IoTConfigBuilder provideIoTConfig() throws Exception {
        return new IoTConfigBuilder()
                .withNewSpec()
                .withNewServices()
                .withDeviceRegistry(newFileBased())
                .endServices()
                .withNewAdapters()
                .withNewMqtt()
                .endMqtt()
                .endAdapters()
                .endSpec();
    }

    @Test
    void testCorrectTypeDeployed () {
        assertCorrectRegistryType("file");
    }

    @Test
    void testRegisterDevice() throws Exception {
        super.doTestRegisterDevice();
    }

    @Test
    void testDisableDevice() throws Exception {
        super.doTestDisableDevice();
    }

    @Test
    void testDeviceCredentials() throws Exception {
        super.doTestDeviceCredentials();
    }

    @Test
    void testDeviceCredentialsPlainPassword() throws Exception {
        super.doTestDeviceCredentialsPlainPassword();
    }

    @Test
    @Disabled("Fixed in hono/pull/1565")
    void testDeviceCredentialsDoesNotContainsPasswordDetails() throws Exception {
        super.doTestDeviceCredentialsDoesNotContainsPasswordDetails();
    }
    @Test
    void testSetExpiryForCredentials() throws Exception {
        super.doTestSetExpiryForCredentials();
    }

}
