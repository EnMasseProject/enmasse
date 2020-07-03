/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingInfrastructure;
import io.enmasse.api.model.MessagingInfrastructureCondition;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.api.model.MessagingTenantCondition;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.annotations.DefaultMessagingTenant;
import io.enmasse.systemtest.messaginginfra.resources.MessagingInfrastructureResourceType;
import io.enmasse.systemtest.messaginginfra.resources.MessagingTenantResourceType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultsTest extends TestBase {

    @Test
    @DefaultMessagingInfrastructure
    public void testDefaultInfra() {
        MessagingInfrastructure infra = resourceManager.getDefaultInfra();

        resourceManager.waitResourceCondition(infra, i -> {
            MessagingInfrastructureCondition condition = MessagingInfrastructureResourceType.getCondition(i.getStatus().getConditions(), "Ready");
            return condition != null && "True".equals(condition.getStatus());
        });


        assertEquals(1, kubernetes.listPods(infra.getMetadata().getNamespace(), Map.of("component", "router")).size());
        assertEquals(1, kubernetes.listPods(infra.getMetadata().getNamespace(), Map.of("component", "broker")).size());
    }

    @Test
    @DefaultMessagingInfrastructure
    @DefaultMessagingTenant
    public void testDefaultTenant() {
        MessagingInfrastructure infra = resourceManager.getDefaultInfra();
        MessagingTenant tenant = resourceManager.getDefaultMessagingTenant();

        assertNotNull(tenant);
        resourceManager.waitResourceCondition(tenant, t -> {
            MessagingTenantCondition condition = MessagingTenantResourceType.getCondition(t.getStatus().getConditions(), "Ready");
            return condition != null && "True".equals(condition.getStatus());
        });
        assertEquals(infra.getMetadata().getName(), tenant.getStatus().getMessagingInfrastructureRef().getName());
        assertEquals(infra.getMetadata().getNamespace(), tenant.getStatus().getMessagingInfrastructureRef().getNamespace());
    }
}
