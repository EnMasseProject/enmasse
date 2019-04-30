/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.iot.model.v1.CommonAdapterContainersBuilder;
import io.enmasse.iot.model.v1.ContainerConfigBuilder;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.utils.IoTUtils;
import io.fabric8.kubernetes.api.model.Quantity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static io.enmasse.systemtest.TestTag.smoke;

@Tag(sharedIot)
@Tag(smoke)
@EnabledIfEnvironmentVariable(named = Environment.useMinikubeEnv, matches = "true")
class SimpleK8sDeployTest {

    private static final String NAMESPACE = Environment.getInstance().namespace();

    private Kubernetes client = Kubernetes.getInstance();

    private static IoTConfig config;

    @BeforeAll
    static void setup() throws Exception {
        Map<String, String> secrets = new HashMap<>();
        secrets.put("iot-auth-service", "systemtests-iot-auth-service-tls");
        secrets.put("iot-tenant-service", "systemtests-iot-tenant-service-tls");
        secrets.put("iot-device-registry", "systemtests-iot-device-registry-tls");

        var r1 = new ContainerConfigBuilder()
                .withNewResources().addToLimits("memory", new Quantity("64Mi")).endResources()
                .build();
        var r2 = new ContainerConfigBuilder()
                .withNewResources().addToLimits("memory", new Quantity("256Mi")).endResources()
                .build();

        var commonContainers = new CommonAdapterContainersBuilder()
                .withNewAdapterLike(r2).endAdapter()
                .withNewProxyLike(r1).endProxy()
                .withNewProxyConfiguratorLike(r1).endProxyConfigurator()
                .build();

        config = new IoTConfigBuilder()

                .withNewMetadata()
                .withName("default")
                .endMetadata()

                .withNewSpec()

                .withNewInterServiceCertificates()
                .withNewSecretCertificatesStrategy()
                .withCaSecretName("systemtests-iot-service-ca")
                .withServiceSecretNames(secrets)
                .endSecretCertificatesStrategy()
                .endInterServiceCertificates()

                .withNewAdapters()

                .withNewHttp()
                .withNewEndpoint().withNewSecretNameStrategy("systemtests-iot-http-adapter-tls").endEndpoint()
                .withNewContainersLike(commonContainers).endContainers()
                .endHttp()

                .withNewMqtt()
                .withNewEndpoint().withNewSecretNameStrategy("systemtests-iot-mqtt-adapter-tls").endEndpoint()
                .withNewContainersLike(commonContainers).endContainers()
                .endMqtt()

                .endAdapters()

                .withNewServices()

                .withNewAuthentication()
                .withNewContainerLike(r2).endContainer()
                .endAuthentication()

                .withNewTenant()
                .withNewContainerLike(r2).endContainer()
                .endTenant()

                .withNewCollector()
                .withNewContainerLike(r1).endContainer()
                .endCollector()

                .withNewDeviceRegistry()
                .withNewFile()
                .withNewContainerLike(r2).endContainer()
                .endFile()
                .endDeviceRegistry()

                .endServices()

                .endSpec()
                .build();

        final Path configTempFile = Files.createTempFile("iot-config", "json");
        try {
            Files.write(configTempFile, new ObjectMapper().writeValueAsBytes(config));
            KubeCMDClient.createFromFile(NAMESPACE, configTempFile);
        } finally {
            Files.deleteIfExists(configTempFile);
        }
    }

    @AfterAll
    static void cleanup() throws Exception {
        KubeCMDClient.deleteIoTConfig(NAMESPACE, "default");
    }

    @Test
    void testDeploy() throws Exception {
        IoTUtils.waitForIoTConfigReady(client, config);
    }

}
