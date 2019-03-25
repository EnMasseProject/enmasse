/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static io.enmasse.systemtest.TestTag.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.iot.model.v1.CommonAdapterContainersBuilder;
import io.enmasse.iot.model.v1.ContainerConfigBuilder;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Quantity;

@Tag(sharedIot)
@Tag(smoke)
@DisabledIfEnvironmentVariable(named = Environment.useMinikubeEnv, matches = "false")
public class SimpleK8sDeployTest {

    private static final String NAMESPACE = Environment.getInstance().namespace();

    private static final String[] EXPECTED_DEPLOYMENTS = new String[] {
                    "iot-auth-service",
                    "iot-device-registry",
                    "iot-gc",
                    "iot-http-adapter",
                    "iot-mqtt-adapter",
                    "iot-tenant-service",
    };

    private static final Map<String,String> IOT_LABELS;

    static {
        IOT_LABELS= new HashMap<> ();
        IOT_LABELS.put("component", "iot");
    }

    private Kubernetes client = Kubernetes.getInstance();

    @BeforeAll
    protected static void setup () throws Exception {
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

        IoTConfig config = new IoTConfigBuilder()

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
    protected static void cleanup() throws Exception {
        KubeCMDClient.deleteIoTConfig(NAMESPACE, "default");
    }

    @Test
    public void testDeploy() throws Exception {

        final TimeoutBudget budget = TimeoutBudget.ofDuration(Duration.ofMinutes(5));

        try {
            TestUtils.waitUntilCondition("IoT Config to deploy", () -> allDeploymentsPresent(), budget);
            TestUtils.waitForNReplicas(this.client , EXPECTED_DEPLOYMENTS.length, IOT_LABELS, budget);
        } catch ( Exception e ) {
            TestUtils.streamNonReadyPods(this.client, NAMESPACE).forEach(KubeCMDClient::dumpPodLogs);
            KubeCMDClient.describePods(NAMESPACE);
            throw e;
        }
    }

    private boolean allDeploymentsPresent () {
        final String[] deployments = this.client.listDeployments(IOT_LABELS).stream()
                .map(deployment -> deployment.getMetadata().getName())
                .toArray(String[]::new);
        Arrays.sort(deployments);
        return Arrays.equals(deployments, EXPECTED_DEPLOYMENTS);
    }


}
