/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra;

import io.enmasse.address.model.CoreCrd;
import io.enmasse.api.model.DoneableMessagingInfra;
import io.enmasse.api.model.MessagingInfra;
import io.enmasse.api.model.MessagingInfraBuilder;
import io.enmasse.api.model.MessagingInfraCondition;
import io.enmasse.api.model.MessagingInfraList;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MessagingInfraResourceType implements ResourceType<MessagingInfra> {
    private static final MixedOperation<MessagingInfra, MessagingInfraList, DoneableMessagingInfra, Resource<MessagingInfra, DoneableMessagingInfra>> operation = Kubernetes.getInstance().getClient().customResources(CoreCrd.messagingInfras(), MessagingInfra.class, MessagingInfraList.class, DoneableMessagingInfra.class);

    @Override
    public String getKind() {
        return "MessagingInfra";
    }

    public static MixedOperation<MessagingInfra, MessagingInfraList, DoneableMessagingInfra, Resource<MessagingInfra, DoneableMessagingInfra>> getOperation() {
        return operation;
    }

    @Override
    public void create(MessagingInfra resource) {
        operation.inNamespace(resource.getMetadata().getNamespace()).createOrReplace(new MessagingInfraBuilder(resource)
                .editOrNewMetadata()
                .withNewResourceVersion("")
                .endMetadata()
                .withNewStatus()
                .endStatus()
                .build());
    }

    @Override
    public void delete(MessagingInfra resource) throws InterruptedException {
        operation.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).cascading(true).delete();
        TimeoutBudget budget = TimeoutBudget.ofDuration(Duration.ofMinutes(5));
        while (!budget.timeoutExpired() && operation.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).get() != null) {
            Thread.sleep(1_000);
        }
    }

    @Override
    public void waitReady(MessagingInfra infra) {
        MessagingInfra found = null;
        TimeoutBudget budget = TimeoutBudget.ofDuration(Duration.ofMinutes(5));
        while (!budget.timeoutExpired()) {
            found = operation.inNamespace(infra.getMetadata().getNamespace()).withName(infra.getMetadata().getName()).get();
            assertNotNull(found);
            if (found.getStatus() != null &&
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

    public static MessagingInfraCondition getCondition(List<MessagingInfraCondition> conditions, String type) {
        for (MessagingInfraCondition condition : conditions) {
            if (type.equals(condition.getType())) {
                return condition;
            }
        }
        return null;
    }
}
