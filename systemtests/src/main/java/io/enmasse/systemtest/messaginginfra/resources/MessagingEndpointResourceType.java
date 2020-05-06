/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra.resources;

import io.enmasse.address.model.CoreCrd;
import io.enmasse.api.model.DoneableMessagingEndpoint;
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingEndpointCondition;
import io.enmasse.api.model.MessagingEndpointList;
import io.enmasse.api.model.MessagingEndpointStatus;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MessagingEndpointResourceType implements ResourceType<MessagingEndpoint> {
    private static final MixedOperation<MessagingEndpoint, MessagingEndpointList, DoneableMessagingEndpoint, Resource<MessagingEndpoint, DoneableMessagingEndpoint>> operation = Kubernetes.getInstance().getClient().customResources(CoreCrd.messagingEndpoints(), MessagingEndpoint.class, MessagingEndpointList.class, DoneableMessagingEndpoint.class);

    @Override
    public String getKind() {
        return "MessagingEndpoint";
    }

    public static MixedOperation<MessagingEndpoint, MessagingEndpointList, DoneableMessagingEndpoint, Resource<MessagingEndpoint, DoneableMessagingEndpoint>> getOperation() {
        return operation;
    }

    @Override
    public void create(MessagingEndpoint resource) {
        operation.inNamespace(resource.getMetadata().getNamespace()).createOrReplace(new MessagingEndpointBuilder(resource)
                .editOrNewMetadata()
                .withNewResourceVersion("")
                .endMetadata()
                .withNewStatus()
                .endStatus()
                .build());
    }

    @Override
    public void delete(MessagingEndpoint resource) throws InterruptedException {
        operation.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).cascading(true).delete();
        waitDeleted(operation, resource);
    }

    @Override
    public void waitReady(MessagingEndpoint infra) {
        MessagingEndpoint found = null;
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
        assertEquals("Active", found.getStatus().getPhase(), printStatus(found.getStatus()));
        infra.setMetadata(found.getMetadata());
        infra.setSpec(found.getSpec());
        infra.setStatus(found.getStatus());
    }

    private static String printStatus(MessagingEndpointStatus status) {
        StringBuilder sb = new StringBuilder();
        sb.append("{phase=").append(status.getPhase())
                .append(",message=").append(status.getMessage())
                .append(",conditions=").append(status.getConditions().stream()
                    .map(condition -> String.format("{type=%s,status=%s,message=%s}", condition.getType(), condition.getStatus(), condition.getMessage()))
                    .collect(Collectors.joining()))
                .append("}");
        return sb.toString();
    }

    public static MessagingEndpointCondition getCondition(List<MessagingEndpointCondition> conditions, String type) {
        for (MessagingEndpointCondition condition : conditions) {
            if (type.equals(condition.getType())) {
                return condition;
            }
        }
        return null;
    }
}
