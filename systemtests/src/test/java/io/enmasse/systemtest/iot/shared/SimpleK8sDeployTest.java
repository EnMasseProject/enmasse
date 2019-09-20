/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.iot.model.v1.CommonAdapterContainersBuilder;
import io.enmasse.iot.model.v1.ContainerConfigBuilder;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.iot.ITestIoTShared;
import io.enmasse.systemtest.bases.iot.IoTTestBaseWithShared;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Quantity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static io.enmasse.systemtest.TestTag.SHARED_IOT;
import static io.enmasse.systemtest.TestTag.SMOKE;
import static io.enmasse.systemtest.time.TimeoutBudget.ofDuration;
import static java.time.Duration.ofMinutes;
import static java.util.Collections.singletonMap;
//TODO: CELE DOJEBANE!
@Disabled
@Tag(SHARED_IOT)
@Tag(SMOKE)
@EnabledIfEnvironmentVariable(named = Environment.USE_MINUKUBE_ENV, matches = "true")
class SimpleK8sDeployTest extends TestBase implements ITestIoTShared {

    private static final Logger log = LoggerFactory.getLogger(SimpleK8sDeployTest.class);
    private static final String NAMESPACE = Environment.getInstance().namespace();
    private static IoTConfig config;
    private Kubernetes client = Kubernetes.getInstance();

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

                .withNewSigfox()
                .withNewEndpoint().withNewSecretNameStrategy("systemtests-iot-sigfox-adapter-tls").endEndpoint()
                .withNewContainersLike(commonContainers).endContainers()
                .endSigfox()

                .withNewLoraWan()
                .withNewEndpoint().withNewSecretNameStrategy("systemtests-iot-lorawan-adapter-tls").endEndpoint()
                .withNewContainersLike(commonContainers).endContainers()
                .endLoraWan()

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
        log.info("Waiting for IoT components to be removed");
        TestUtils.waitForNReplicas(0, singletonMap("component", "iot"), ofDuration(ofMinutes(5)));
    }

    @Test
    void testDeploy() throws Exception {
        IoTUtils.waitForIoTConfigReady(client, config);
    }

}
