/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingInfra;
import io.enmasse.api.model.MessagingInfraBuilder;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.api.model.MessagingTenantBuilder;
import io.enmasse.api.model.MessagingTenantCondition;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedSharedInfra;
import io.enmasse.systemtest.messaginginfra.MessagingTenantResourceType;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag(TestTag.ISOLATED_SHARED_INFRA)
public class MessagingTenantTest extends TestBase implements ITestIsolatedSharedInfra {

    @Test
    public void testMultipleMessagingTenants() {
        MessagingInfra infra = new MessagingInfraBuilder()
                .withNewMetadata()
                .withName("default-infra")
                .withNamespace(environment.namespace())
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();

        MessagingTenant t1 = new MessagingTenantBuilder()
                .editOrNewMetadata()
                .withName("default")
                .withNamespace("app1")
                .endMetadata()
                .build();

        MessagingTenant t2 = new MessagingTenantBuilder()
                .editOrNewMetadata()
                .withName("default")
                .withNamespace("app2")
                .endMetadata()
                .build();

        infraResourceManager.createResource(infra, t1, t2);

        t1 = MessagingTenantResourceType.getOperation().inNamespace(t1.getMetadata().getNamespace()).withName(t1.getMetadata().getName()).get();
        assertNotNull(t1);
        MessagingTenantCondition condition = MessagingTenantResourceType.getCondition(t1.getStatus().getConditions(), "Ready");
        assertNotNull(condition);
        assertEquals("True", condition.getStatus());
        assertEquals("default-infra", t1.getStatus().getMessagingInfraRef().getName());
        assertEquals(environment.namespace(), t1.getStatus().getMessagingInfraRef().getNamespace());

        t2 = MessagingTenantResourceType.getOperation().inNamespace(t2.getMetadata().getNamespace()).withName(t2.getMetadata().getName()).get();
        assertNotNull(t2);
        condition = MessagingTenantResourceType.getCondition(t2.getStatus().getConditions(), "Ready");
        assertNotNull(condition);
        assertEquals("True", condition.getStatus());
        assertEquals("default-infra", t2.getStatus().getMessagingInfraRef().getName());
        assertEquals(environment.namespace(), t2.getStatus().getMessagingInfraRef().getNamespace());
    }

    @Test
    public void testSelectors() {
        MessagingInfra infra = new MessagingInfraBuilder()
                .withNewMetadata()
                .withName("default-infra")
                .withNamespace(environment.namespace())
                .endMetadata()
                .withNewSpec()
                .editOrNewSelector()
                .addNewNamespace("app1")
                .endSelector()
                .endSpec()
                .build();

        MessagingTenant t1 = new MessagingTenantBuilder()
                .editOrNewMetadata()
                .withName("default")
                .withNamespace("app1")
                .endMetadata()
                .build();

        MessagingTenant t2 = new MessagingTenantBuilder()
                .editOrNewMetadata()
                .withName("default")
                .withNamespace("app2")
                .endMetadata()
                .build();

        infraResourceManager.createResource(infra, t1);
        infraResourceManager.createResource(false, t2);

        MessagingTenantCondition condition = null;
        TimeoutBudget budget = TimeoutBudget.ofDuration(Duration.ofMinutes(2));
        while (!budget.timeoutExpired()) {
            t2 = MessagingTenantResourceType.getOperation().inNamespace(t2.getMetadata().getNamespace()).withName(t2.getMetadata().getName()).get();
            assertNotNull(t2);
            if (t2.getStatus() != null && t2.getStatus().getConditions() != null) {
                condition = MessagingTenantResourceType.getCondition(t2.getStatus().getConditions(), "Ready");
                break;
            }
        }

        assertNotNull(condition);
        assertEquals("False", condition.getStatus());
    }
}
