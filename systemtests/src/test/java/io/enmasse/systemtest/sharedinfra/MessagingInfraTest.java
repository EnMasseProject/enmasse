/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingInfra;
import io.enmasse.api.model.MessagingInfraBuilder;
import io.enmasse.api.model.MessagingInfraCondition;
import io.enmasse.systemtest.TestTag;
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
