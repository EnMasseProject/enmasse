/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingAddress;
import io.enmasse.api.model.MessagingAddressBuilder;
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingInfra;
import io.enmasse.api.model.MessagingInfraBuilder;
import io.enmasse.api.model.MessagingInfraCondition;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.annotations.DefaultMessagingInfra;
import io.enmasse.systemtest.annotations.DefaultMessagingTenant;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedSharedInfra;
import io.enmasse.systemtest.messaginginfra.resources.MessagingInfraResourceType;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag(TestTag.ISOLATED_SHARED_INFRA)
public class MessagingInfraTest extends TestBase implements ITestIsolatedSharedInfra {

    /**
     * Test that infrastructure static scaling strategy can be changed and that change is reflected
     * in the underlying pods.
     */
    @Test
    public void testInfraStaticScalingStrategy() throws Exception {
        MessagingInfra infra = new MessagingInfraBuilder()
                .withNewMetadata()
                .withName("default-infra")
                .withNamespace(environment.namespace())
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();

        infraResourceManager.createResource(infra);

        MessagingInfraCondition condition = MessagingInfraResourceType.getCondition(infra.getStatus().getConditions(), "Ready");
        assertNotNull(condition);
        assertEquals("True", condition.getStatus());

        waitForConditionTrue(infra, "Ready");
        waitForConditionTrue(infra, "BrokersCreated");
        waitForConditionTrue(infra, "RoutersCreated");
        waitForConditionTrue(infra, "BrokersConnected");

        assertEquals(1, kubernetes.listPods(infra.getMetadata().getNamespace(), Map.of("component", "router")).size());
        assertEquals(1, kubernetes.listPods(infra.getMetadata().getNamespace(), Map.of("component", "broker")).size());

        // Scale up
        infra = new MessagingInfraBuilder(infra)
                .editOrNewSpec()
                .editOrNewRouter()
                .editOrNewScalingStrategy()
                .editOrNewStatic()
                .withReplicas(3)
                .endStatic()
                .endScalingStrategy()
                .endRouter()
                .editOrNewBroker()
                .editOrNewScalingStrategy()
                .editOrNewStatic()
                .withPoolSize(2)
                .endStatic()
                .endScalingStrategy()
                .endBroker()
                .endSpec()
                .build();
        infraResourceManager.createResource(infra);

        waitForConditionTrue(infra, "BrokersConnected");
        TestUtils.waitForNReplicas(3, infra.getMetadata().getNamespace(), Map.of("component", "router"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(2)));
        TestUtils.waitForNReplicas(2, infra.getMetadata().getNamespace(), Map.of("component", "broker"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(2)));

        // Scale down
        infra = new MessagingInfraBuilder(infra)
                .editOrNewSpec()
                .editOrNewRouter()
                .editOrNewScalingStrategy()
                .editOrNewStatic()
                .withReplicas(2)
                .endStatic()
                .endScalingStrategy()
                .endRouter()
                .editOrNewBroker()
                .editOrNewScalingStrategy()
                .editOrNewStatic()
                .withPoolSize(1)
                .endStatic()
                .endScalingStrategy()
                .endBroker()
                .endSpec()
                .build();
        infraResourceManager.createResource(infra);

        waitForConditionTrue(infra, "BrokersConnected");
        TestUtils.waitForNReplicas(2, infra.getMetadata().getNamespace(), Map.of("component", "router"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(5)));
        TestUtils.waitForNReplicas(1, infra.getMetadata().getNamespace(), Map.of("component", "broker"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(5)));
    }

    /**
     * Test that the pods in the messaging infrastructure can be restarted, and that they are reconfigured to support
     * existing addresses and endpoints.
     */
    @Test
    @ExternalClients
    @DefaultMessagingInfra
    @DefaultMessagingTenant
    public void testInfraRestart() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();

        MessagingAddress queue = new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("queue1")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewQueue()
                .endQueue()
                .endSpec()
                .build();

        MessagingAddress anycast = new MessagingAddressBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("anycast1")
                .endMetadata()
                .editOrNewSpec()
                .editOrNewAnycast()
                .endAnycast()
                .endSpec()
                .build();

        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
                .endMetadata()
                .editOrNewSpec()
                .addToProtocols("AMQP")
                .editOrNewCluster()
                .endCluster()
                .endSpec()
                .build();

        infraResourceManager.createResource(anycast, queue, endpoint);

        // Make sure endpoints work first
        LOGGER.info("Running initial client check");
        MessagingEndpointTest.doTestSendReceiveAddress(endpoint.getStatus().getHost(), endpoint.getStatus().getPorts().get(0).getPort(), anycast.getMetadata().getName());
        MessagingEndpointTest.doTestSendReceiveAddress(endpoint.getStatus().getHost(), endpoint.getStatus().getPorts().get(0).getPort(), queue.getMetadata().getName());

        // Restart router and broker pods
        MessagingInfra infra = infraResourceManager.getDefaultInfra();
        kubernetes.deletePod(infra.getMetadata().getNamespace(), Collections.singletonMap("infra", infra.getMetadata().getName()));

        LOGGER.info("Waiting for pods to come back up");

        // Give operator some time to detect restart and re-sync its state
        Thread.sleep(60_000);

        waitForConditionTrue(infra, "Synchronized");
        waitForConditionTrue(infra, "Ready");

        LOGGER.info("Re-running client check");

        MessagingEndpointTest.doTestSendReceiveAddress(endpoint.getStatus().getHost(), endpoint.getStatus().getPorts().get(0).getPort(), anycast.getMetadata().getName());
        MessagingEndpointTest.doTestSendReceiveAddress(endpoint.getStatus().getHost(), endpoint.getStatus().getPorts().get(0).getPort(), queue.getMetadata().getName());
    }

    private void waitForConditionTrue(MessagingInfra infra, String conditionName) throws InterruptedException {
        TimeoutBudget budget = TimeoutBudget.ofDuration(Duration.ofMinutes(5));
        while (!budget.timeoutExpired()) {
            if (infra.getStatus() != null) {
                MessagingInfraCondition condition = MessagingInfraResourceType.getCondition(infra.getStatus().getConditions(), conditionName);
                if (condition != null && "True".equals(condition.getStatus())) {
                    break;
                }
            }
            Thread.sleep(1000);
            infra = MessagingInfraResourceType.getOperation().inNamespace(infra.getMetadata().getNamespace()).withName(infra.getMetadata().getName()).get();
            assertNotNull(infra);
        }
        infra = MessagingInfraResourceType.getOperation().inNamespace(infra.getMetadata().getNamespace()).withName(infra.getMetadata().getName()).get();
        assertNotNull(infra);
        assertNotNull(infra.getStatus());
        MessagingInfraCondition condition = MessagingInfraResourceType.getCondition(infra.getStatus().getConditions(), conditionName);
        assertNotNull(condition);
        assertEquals("True", condition.getStatus());
    }
}
