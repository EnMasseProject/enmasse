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
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag(TestTag.ISOLATED_SHARED_INFRA)
public class MessagingInfraTest extends TestBase implements ITestIsolatedSharedInfra {
    @Test
    public void testInfraStaticScalingStrategy() throws Exception {
        MessagingInfra infra = new MessagingInfraBuilder()
                .editOrNewMetadata()
                .withName("infra1")
                .endMetadata()
                .editOrNewSpec()
                .endSpec()
                .build();

        messagingInfraClient.inNamespace(environment.namespace()).create(infra);

        MessagingInfra found = waitForInfraActive(environment.namespace(), infra.getMetadata().getName());

        MessagingInfraCondition readyCondition = findCondition(found.getStatus().getConditions(), "Ready");
        assertNotNull(readyCondition);
        assertEquals("True", readyCondition.getStatus());

        assertEquals(1, kubernetes.listPods(found.getMetadata().getNamespace(), Map.of("component", "router")).size());
        assertEquals(1, kubernetes.listPods(found.getMetadata().getNamespace(), Map.of("component", "broker-infra1")).size());

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
        messagingInfraClient.inNamespace(environment.namespace()).createOrReplace(infra);

        TestUtils.waitForNReplicas(3, found.getMetadata().getNamespace(), Map.of("component", "router"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(2)));
        TestUtils.waitForNReplicas(2, found.getMetadata().getNamespace(), Map.of("component", "broker-infra1"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(2)));

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
        messagingInfraClient.inNamespace(environment.namespace()).createOrReplace(infra);

        TestUtils.waitForNReplicas(2, found.getMetadata().getNamespace(), Map.of("component", "router"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(2)));
        TestUtils.waitForNReplicas(1, found.getMetadata().getNamespace(), Map.of("component", "broker-infra1"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(2)));
    }

    private MessagingInfra waitForInfraActive(String namespace, String name) {
        MessagingInfra found = null;
        TimeoutBudget budget = TimeoutBudget.ofDuration(Duration.ofMinutes(5));
        while (!budget.timeoutExpired()) {
            found = messagingInfraClient.inNamespace(namespace).withName(name).get();
            assertNotNull(found);
            if (found.getStatus() != null &&
                    "Active".equals(found.getStatus().getPhase())) {
                break;
            }
        }
        assertNotNull(found);
        assertNotNull(found.getStatus());
        assertEquals("Active", found.getStatus().getPhase());
        return found;
    }

    private MessagingInfraCondition findCondition(List<MessagingInfraCondition> conditions, String type) {
        for (MessagingInfraCondition condition : conditions) {
            if (type.equals(condition.getType())) {
                return condition;
            }
        }
        return null;
    }
}
