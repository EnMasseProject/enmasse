/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static io.enmasse.systemtest.TestTag.smoke;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.TestUtils;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;

@Tag(sharedIot)
@Tag(smoke)
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
        KubeCMDClient.createFromFile(NAMESPACE, Paths.get("templates/iot/k8s-systemtests-config.json"));
    }

    @AfterAll
    protected static void cleanup() throws Exception {
        KubeCMDClient.deleteIoTConfig(NAMESPACE, "default");
    }

    @Test
    public void testDeploy() throws Exception {

        final TimeoutBudget budget = TimeoutBudget.ofDuration(Duration.ofMinutes(5));

        TestUtils.waitUntilCondition("IoT Config to deploy", () -> allDeploymentsPresent(), budget);
        TestUtils.waitForNReplicas(this.client , EXPECTED_DEPLOYMENTS.length, IOT_LABELS, budget);
    }

    private boolean allDeploymentsPresent () {
        final String[] deployments = this.client.listDeployments(IOT_LABELS).stream()
                .map(deployment -> deployment.getMetadata().getName())
                .toArray(String[]::new);
        Arrays.sort(deployments);
        return Arrays.equals(deployments, EXPECTED_DEPLOYMENTS);
    }


}
