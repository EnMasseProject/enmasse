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
import io.enmasse.systemtest.messaginginfra.crds.MessagingInfraCrd;
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
        MessagingInfra infra = MessagingInfraCrd.getDefaultInfra()
                .editMetadata()
                .withNamespace(environment.namespace())
                .endMetadata()
                .done();

        infraResourceManager.createResource(infra);

        MessagingInfraCondition condition = MessagingInfraCrd.getCondition(infra.getStatus().getConditions(), "Ready");
        assertNotNull(condition);
        assertEquals("True", condition.getStatus());


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

        TestUtils.waitForNReplicas(2, infra.getMetadata().getNamespace(), Map.of("component", "router"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(2)));
        TestUtils.waitForNReplicas(1, infra.getMetadata().getNamespace(), Map.of("component", "broker"), Collections.emptyMap(), TimeoutBudget.ofDuration(Duration.ofMinutes(2)));
    }
}
