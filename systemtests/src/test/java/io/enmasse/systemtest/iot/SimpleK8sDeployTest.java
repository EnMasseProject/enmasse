/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static io.enmasse.systemtest.TestTag.sharedIot;
import static io.enmasse.systemtest.TestTag.smoke;

import java.io.IOException;
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

    private static void execute(final String [] command ) throws InterruptedException, IOException {
        Process p =  new ProcessBuilder(command)
                .inheritIO()
                .start();

        final int rc = p.waitFor();
        if ( rc != 0) {
            throw new RuntimeException(String.format("Failed to run: %s -> %s", command, rc));
        }
    }

    @BeforeAll
    protected static void setup () throws Exception {
        execute(new String[] {
                "kubectl", "-n", NAMESPACE, "create", "--validate=false", "-f", "templates/iot/k8s-systemtests-config.json"
        });
    }

    @AfterAll
    protected static void cleanup() throws Exception {
        execute(new String [] {"kubectl", "-n", NAMESPACE, "delete", "iotconfig", "default"});
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
