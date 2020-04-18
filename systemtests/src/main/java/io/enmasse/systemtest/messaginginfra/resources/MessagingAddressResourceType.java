/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra.resources;

import io.enmasse.address.model.CoreCrd;
import io.enmasse.api.model.DoneableMessagingAddress;
import io.enmasse.api.model.MessagingAddress;
import io.enmasse.api.model.MessagingAddressBuilder;
import io.enmasse.api.model.MessagingAddressCondition;
import io.enmasse.api.model.MessagingAddressList;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MessagingAddressResourceType implements ResourceType<MessagingAddress> {
    private static final MixedOperation<MessagingAddress, MessagingAddressList, DoneableMessagingAddress, Resource<MessagingAddress, DoneableMessagingAddress>> operation = Kubernetes.getInstance().getClient().customResources(CoreCrd.messagingAddresses(), MessagingAddress.class, MessagingAddressList.class, DoneableMessagingAddress.class);

    @Override
    public String getKind() {
        return "MessagingAddress";
    }

    public static MixedOperation<MessagingAddress, MessagingAddressList, DoneableMessagingAddress, Resource<MessagingAddress, DoneableMessagingAddress>> getOperation() {
        return operation;
    }

    @Override
    public void create(MessagingAddress resource) {
        operation.inNamespace(resource.getMetadata().getNamespace()).createOrReplace(new MessagingAddressBuilder(resource)
                .editOrNewMetadata()
                .withNewResourceVersion("")
                .endMetadata()
                .withNewStatus()
                .endStatus()
                .build());
    }

    @Override
    public void delete(MessagingAddress resource) {
        operation.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).cascading(true).delete();
    }

    void waitDeleted() {
        TimeoutBudget budget = TimeoutBudget.ofDuration(Duration.ofMinutes(5));
        while (!budget.timeoutExpired()) {


        }
    }

    @Override
    public void waitReady(MessagingAddress infra) {
        MessagingAddress found = null;
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

    public static MessagingAddressCondition getCondition(List<MessagingAddressCondition> conditions, String type) {
        for (MessagingAddressCondition condition : conditions) {
            if (type.equals(condition.getType())) {
                return condition;
            }
        }
        return null;
    }
}
