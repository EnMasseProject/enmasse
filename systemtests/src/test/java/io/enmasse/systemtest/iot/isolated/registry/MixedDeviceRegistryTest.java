/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.isolated.registry;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.newPostgresBasedRegistry;
import static io.enmasse.systemtest.utils.IoTUtils.assertCorrectDeviceConnectionType;
import static io.enmasse.systemtest.utils.IoTUtils.assertCorrectRegistryType;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.iot.DefaultDeviceRegistry;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;

/**
 * Test a mixed mode (JDBC + Infinispan) setup.
 */
class MixedDeviceRegistryTest extends DeviceRegistryTest {

    @Override
    protected IoTConfigBuilder provideIoTConfig() throws Exception {

        var jdbcEndpoint = SystemtestsKubernetesApps.deployPostgresqlServer();
        var infinispanEndpoint = SystemtestsKubernetesApps.deployInfinispanServer();

        return IoTTestSession
                .createDefaultConfig()
                .editOrNewSpec()
                .withNewServices()
                .withDeviceConnection(DefaultDeviceRegistry.newInfinispanExternalDeviceConnectionService(infinispanEndpoint))
                .withDeviceRegistry(newPostgresBasedRegistry(jdbcEndpoint, false))
                .endServices()
                .endSpec();

    }

    @Test
    void testCorrectTypeDeployed () {
        assertCorrectDeviceConnectionType("infinispan");
        assertCorrectRegistryType("jdbc");
    }

    @Test
    @Tag(ACCEPTANCE)
    void testRegisterDevice() throws Exception {
        super.doTestRegisterDevice();
    }

    @Test
    @Tag(ACCEPTANCE)
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
    @Disabled("Caches expire a bit unpredictably")
    void testCacheExpiryForCredentials() throws Exception {
        super.doTestCacheExpiryForCredentials();
    }

    @Test
    void testSetExpiryForCredentials() throws Exception {
        super.doTestSetExpiryForCredentials();
    }

    @Test
    void testCreateForNonExistingTenantFails() throws Exception {
        super.doTestCreateForNonExistingTenantFails();
    }

    @Test
    void testCreateDuplicateDeviceFails() throws Exception {
        super.doCreateDuplicateDeviceFails();
    }

    @Test
    void testRegisterMultipleDevices() throws Exception {
        super.doRegisterMultipleDevices();
    }

    @Test
    void testTenantDeletionTriggersDevicesDeletion() throws Exception {
        super.doTestTenantDeletionTriggersDevicesDeletion();
    }
}
