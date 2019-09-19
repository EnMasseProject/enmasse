/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.project;



import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.enmasse.address.model.KubeUtil;
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

    private static final byte [] GO_NAMESPACE = new byte []{(byte)0x15,(byte)0x16,(byte)0xb2,(byte)0x46,(byte)0x23,(byte)0xaa,(byte)0x11,(byte)0xe9,(byte)0xb6,(byte)0x15,(byte)0xc8,(byte)0x5b,(byte)0x76,(byte)0x2e,(byte)0x5a,(byte)0x2c};
    private static final Pattern GO_ADDRESS_PATTERN = Pattern.compile("[^a-z0-9]");

    /**
     * Create the address name, aligned with the Go logic of creating names.
     * <br>
     * The main difference between Go and the Java method {@link #generateName(String, String)} is, that in Go
     * you must use a namespace prefix for a type 3 UUID. This method uses the same prefix as the Go code.
     *
     * @param addressSpace The name of the address space.
     * @param address The name of the address.
     * @return The encoded name, compatible with the logic in Go.
     */
    public static String generateNameForGo(final String addressSpace, final String address) {

        byte [] addressBytes = address.getBytes(StandardCharsets.UTF_8);
        byte [] data = new byte[addressBytes.length + GO_NAMESPACE.length];

        System.arraycopy(GO_NAMESPACE, 0, data, 0, GO_NAMESPACE.length);
        System.arraycopy(addressBytes, 0, data, GO_NAMESPACE.length, addressBytes.length);

        String uuid = UUID.nameUUIDFromBytes(data).toString();

        return KubeUtil.sanitizeName(addressSpace) + "." + KubeUtil.sanitizeWithPattern(address, GO_ADDRESS_PATTERN) + "-" + uuid;
    }

    @Test
    public void test1 () {
        assertEquals("iot.commandenmasseinfraiot-40f42ffb-c35d-39da-a3b7-8d3b7cf30edb", generateNameForGo("iot", "command/enmasse-infra.iot"));
    }


}
