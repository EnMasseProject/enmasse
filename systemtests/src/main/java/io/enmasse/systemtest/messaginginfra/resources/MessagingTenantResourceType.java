/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra.resources;

import io.enmasse.address.model.CoreCrd;
import io.enmasse.api.model.DoneableMessagingTenant;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.api.model.MessagingTenantBuilder;
import io.enmasse.api.model.MessagingTenantCondition;
import io.enmasse.api.model.MessagingTenantList;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MessagingTenantResourceType implements ResourceType<MessagingTenant> {
    private static final MixedOperation<MessagingTenant, MessagingTenantList, DoneableMessagingTenant, Resource<MessagingTenant, DoneableMessagingTenant>> operation = Kubernetes.getInstance().getClient().customResources(CoreCrd.messagingTenants(), MessagingTenant.class, MessagingTenantList.class, DoneableMessagingTenant.class);

    @Override
    public String getKind() {
        return "MessagingTenant";
    }

    public static MessagingTenant getDefault() {
        return new MessagingTenantBuilder()
                .editOrNewMetadata()
                .withName("default")
                .withNamespace(NamespaceResourceType.getDefault().getMetadata().getName())
                .endMetadata()
                .build();
    }

    public static MixedOperation<MessagingTenant, MessagingTenantList, DoneableMessagingTenant, Resource<MessagingTenant, DoneableMessagingTenant>> getOperation() {
        return operation;
    }

    @Override
    public void create(MessagingTenant resource) {
        operation.inNamespace(resource.getMetadata().getNamespace()).createOrReplace(new MessagingTenantBuilder(resource)
                .editOrNewMetadata()
                .withNewResourceVersion("")
                .endMetadata()
                .withNewStatus()
                .endStatus()
                .build());
    }

    @Override
    public void delete(MessagingTenant resource) throws InterruptedException {
        operation.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).cascading(true).delete();
        waitDeleted(operation, resource);
    }

    @Override
    public void waitReady(MessagingTenant infra) {
        MessagingTenant found = null;
        TimeoutBudget budget = TimeoutBudget.ofDuration(Duration.ofMinutes(5));
        while (!budget.timeoutExpired()) {
            found = operation.inNamespace(infra.getMetadata().getNamespace()).withName(infra.getMetadata().getName()).get();
            if (found != null &&
                    found.getStatus() != null &&
                    "Active".equals(found.getStatus().getPhase())) {
                break;
            }
        }
        assertNotNull(found);
        assertNotNull(found.getStatus());
        assertEquals("Active", found.getStatus().getPhase());
        infra.setMetadata(found.getMetadata());
        infra.setSpec(found.getSpec());
        infra.setStatus(found.getStatus());
    }

    public static MessagingTenantCondition getCondition(List<MessagingTenantCondition> conditions, String type) {
        for (MessagingTenantCondition condition : conditions) {
            if (type.equals(condition.getType())) {
                return condition;
            }
        }
        return null;
    }
}
