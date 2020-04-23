/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.isolated.registry;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.iot.DefaultDeviceRegistry.newH2Based;
import static io.enmasse.systemtest.utils.IoTUtils.assertCorrectRegistryMode;
import static io.enmasse.systemtest.utils.IoTUtils.assertCorrectRegistryType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.iot.model.v1.Mode;
import io.enmasse.systemtest.iot.IoTTestSession;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;

class H2DeviceRegistryTest extends DeviceRegistryTest {

    private final Logger log = CustomLogger.getLogger();

    @Override
    protected IoTConfigBuilder provideIoTConfig() throws Exception {
        return IoTTestSession
                .createDefaultConfig()
                .editOrNewSpec()
                .withServices(newH2Based())
                .endSpec();
    }

    @Test
    void testCorrectTypeDeployed () {
        assertCorrectRegistryType("jdbc");
        assertCorrectRegistryMode(Mode.TABLE);
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

        // dump content, just in case

        dumpH2Database();

        // ensure that the database is still present, but has zero entries

        final List<String> command = new ArrayList<>(Arrays.asList(SystemtestsKubernetesApps.H2_SHELL_COMMAND));
        command.addAll(Arrays.asList("-sql", "select count(*) as NUM from device_registrations"));

        var pod = SystemtestsKubernetesApps
                .getH2ServerPod()
                .orElseThrow(() -> new IllegalStateException("Unable to find H2 server pod"));
        final String[] result = Kubernetes.executeWithInput(pod, null, null, Duration.ofSeconds(10),
                command.toArray(String[]::new)
                )
                .split("[\r\n]+");

        // must have three lines
        assertEquals(3, result.length);
        // first: the column name
        assertEquals("NUM", result[0]);
        // second: the number of entries, which must be zero
        assertEquals("0", result[1]);
        // third: the execution statistics, which we ignore

    }

    private void dumpH2Database() throws IOException, InterruptedException, TimeoutException {
        final List<String> command = new ArrayList<>(Arrays.asList(SystemtestsKubernetesApps.H2_SHELL_COMMAND));
        command.addAll(Arrays.asList("-sql", "select * from device_registrations"));

        var pod = SystemtestsKubernetesApps
                .getH2ServerPod()
                .orElseThrow(() -> new IllegalStateException("Unable to find H2 server pod"));
        final String result = Kubernetes.executeWithInput(pod, null, null, Duration.ofSeconds(10),
                command.toArray(String[]::new));

        log.info("Current H2 database:\n{}", result);
    }
}
