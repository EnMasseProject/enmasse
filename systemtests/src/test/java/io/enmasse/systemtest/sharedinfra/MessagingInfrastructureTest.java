/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingAddress;
import io.enmasse.api.model.MessagingAddressBuilder;
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingInfrastructure;
import io.enmasse.api.model.MessagingInfrastructureBuilder;
import io.enmasse.api.model.MessagingInfrastructureCondition;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.annotations.DefaultMessagingTenant;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messaginginfra.resources.MessagingInfrastructureResourceType;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessagingInfrastructureTest extends TestBase {
    private static final Logger LOGGER = CustomLogger.getLogger();

    /**
     * Test that infrastructure static scaling strategy can be changed and that change is reflected
     * in the underlying pods.
     */
    @Test
    public void testInfraStaticScalingStrategy() throws Exception {
        MessagingInfrastructure infra = new MessagingInfrastructureBuilder()
                .withNewMetadata()
                .withName("default-infra")
                .withNamespace(environment.namespace())
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();

        resourceManager.createResource(infra);

        waitForConditionsTrue(infra);

        assertEquals(1, kubernetes.listPods(infra.getMetadata().getNamespace(), Map.of("component", "router")).size());
        assertEquals(1, kubernetes.listPods(infra.getMetadata().getNamespace(), Map.of("component", "broker")).size());

        // Scale up
        infra = new MessagingInfrastructureBuilder(infra)
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

        resourceManager.createResource(infra);

        assertTrue(resourceManager.waitResourceCondition(infra, i -> i.getStatus() != null && i.getStatus().getBrokers() != null && i.getStatus().getBrokers().size() == 2));
        assertTrue(resourceManager.waitResourceCondition(infra, i -> i.getStatus() != null && i.getStatus().getRouters() != null && i.getStatus().getRouters().size() == 3));

        waitForConditionsTrue(infra);

        // Ensure that we can find the pods as the above conditions are true
        assertEquals(3, kubernetes.listPods(infra.getMetadata().getNamespace(), Map.of("component", "router")).size());
        assertEquals(2, kubernetes.listPods(infra.getMetadata().getNamespace(), Map.of("component", "broker")).size());

        // Scale down
        infra = new MessagingInfrastructureBuilder(infra)
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
        resourceManager.createResource(infra);

        assertTrue(resourceManager.waitResourceCondition(infra, i -> i.getStatus() != null && i.getStatus().getBrokers() != null && i.getStatus().getBrokers().size() == 1));
        assertTrue(resourceManager.waitResourceCondition(infra, i -> i.getStatus() != null && i.getStatus().getRouters() != null && i.getStatus().getRouters().size() == 2));

        waitForConditionsTrue(infra);

        // Conditions are not set to false when scaling down, so we must wait for replicas
        TestUtils.waitForNReplicas(2, infra.getMetadata().getNamespace(), Map.of("component", "router"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(5)));
        TestUtils.waitForNReplicas(1, infra.getMetadata().getNamespace(), Map.of("component", "broker"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(5)));
    }

    /**
     * Test that the pods in the messaging infrastructure can be restarted, and that they are reconfigured to support
     * existing addresses and endpoints.
     */
    @Test
    @ExternalClients
    @DefaultMessagingInfrastructure
    @DefaultMessagingTenant
    public void testInfraRestart() throws Exception {
        MessagingTenant tenant = resourceManager.getDefaultMessagingTenant();

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

        resourceManager.createResource(endpoint, anycast, queue);

        // Make sure endpoints work first
        LOGGER.info("Running initial client check");
        MessagingEndpointTest.doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), endpoint.getStatus().getPorts().get(0).getPort(), anycast.getMetadata().getName(), false, false);
        MessagingEndpointTest.doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), endpoint.getStatus().getPorts().get(0).getPort(), queue.getMetadata().getName(), false, false);

        // Restart router and broker pods
        MessagingInfrastructure defaultInfra = resourceManager.getDefaultInfra();
        kubernetes.deletePod(defaultInfra.getMetadata().getNamespace(), Collections.singletonMap("infra", defaultInfra.getMetadata().getName()));

        Thread.sleep(60_000);

        // Wait for the operator state to be green again.
        assertTrue(resourceManager.waitResourceCondition(defaultInfra, i -> i.getStatus() != null && i.getStatus().getBrokers() != null && i.getStatus().getBrokers().size() == 1));
        assertTrue(resourceManager.waitResourceCondition(defaultInfra, i -> i.getStatus() != null && i.getStatus().getRouters() != null && i.getStatus().getRouters().size() == 1));
        waitForConditionsTrue(defaultInfra);

        MessagingEndpointTest.doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), endpoint.getStatus().getPorts().get(0).getPort(), anycast.getMetadata().getName(), false, false);
        MessagingEndpointTest.doTestSendReceiveOnCluster(endpoint.getStatus().getHost(), endpoint.getStatus().getPorts().get(0).getPort(), queue.getMetadata().getName(), false, false);
    }

    private void waitForConditionsTrue(MessagingInfrastructure infra) {
        for (String condition : List.of("CaCreated", "RoutersCreated", "BrokersCreated", "BrokersConnected", "Ready")) {
            waitForCondition(infra, condition, "True");
        }
    }

    private void waitForCondition(MessagingInfrastructure infra, String conditionName, String expectedValue) {
        assertTrue(resourceManager.waitResourceCondition(infra, messagingInfra -> {
            MessagingInfrastructureCondition condition = MessagingInfrastructureResourceType.getCondition(messagingInfra.getStatus().getConditions(), conditionName);
            return condition != null && expectedValue.equals(condition.getStatus());
        }));
    }
}
