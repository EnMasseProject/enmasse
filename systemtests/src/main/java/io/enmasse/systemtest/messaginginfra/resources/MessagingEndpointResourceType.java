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
import io.enmasse.api.model.MessagingInfrastructure;
import io.enmasse.api.model.MessagingInfrastructureCondition;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagingEndpointResourceType implements ResourceType<MessagingEndpoint> {

    @Override
    public String getKind() {
        return ResourceKind.MESSAGING_ENDPOINT;
    }

    @Override
    public MessagingEndpoint get(String namespace, String name) {
        return getOperation().inNamespace(namespace).withName(name).get();
    }

    public static MixedOperation<MessagingEndpoint, MessagingEndpointList, DoneableMessagingEndpoint, Resource<MessagingEndpoint, DoneableMessagingEndpoint>> getOperation() {
        return Kubernetes.getClient().customResources(CustomResourceDefinitionContext.fromCrd(CoreCrd.messagingEndpoints()), MessagingEndpoint.class, MessagingEndpointList.class, DoneableMessagingEndpoint.class);
    }

    @Override
    public void create(MessagingEndpoint resource) {
        getOperation().inNamespace(resource.getMetadata().getNamespace()).createOrReplace(new MessagingEndpointBuilder(resource)
                .editOrNewMetadata()
                .withNewResourceVersion("")
                .endMetadata()
                .withNewStatus()
                .endStatus()
                .build());
    }

    @Override
    public void delete(MessagingEndpoint resource) throws InterruptedException {
        getOperation().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).cascading(true).delete();
    }

    @Override
    public boolean isReady(MessagingEndpoint endpoint) {
        return endpoint != null &&
                endpoint.getStatus() != null &&
                "Active".equals(endpoint.getStatus().getPhase());
    }

    @Override
    public void refreshResource(MessagingEndpoint existing, MessagingEndpoint newResource) {
        existing.setMetadata(newResource.getMetadata());
        existing.setSpec(newResource.getSpec());
        existing.setStatus(newResource.getStatus());
    }

    public static MessagingEndpointCondition getCondition(List<MessagingEndpointCondition> conditions, String type) {
        for (MessagingEndpointCondition condition : conditions) {
            if (type.equals(condition.getType())) {
                return condition;
            }
        }
        return null;
    }

    public static Map<String, String> getConditions(MessagingEndpoint infrastructure) {
        Map<String, String> conditions = new HashMap<>();
        for (String conditionName : List.of("FoundProject", "ConfiguredTLS", "AllocatedPorts", "Created", "ServiceCreated", "Ready")) {
            MessagingEndpointCondition condition = MessagingEndpointResourceType.getCondition(infrastructure.getStatus().getConditions(), conditionName);
            if (condition == null) {
                conditions.put(conditionName, "Unknown");
            } else {
                conditions.put(conditionName, condition.getStatus());
            }
        }
        return conditions;
    }

    public static int getPort(String protocol, MessagingEndpoint endpoint) {
        return endpoint.getStatus().getPorts().stream()
                .filter(p -> p.getProtocol().equals(protocol))
                .findAny().orElseThrow().getPort();
    }
}
