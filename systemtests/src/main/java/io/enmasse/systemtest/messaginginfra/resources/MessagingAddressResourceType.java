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
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointCondition;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagingAddressResourceType implements ResourceType<MessagingAddress> {

    @Override
    public String getKind() {
        return ResourceKind.MESSAGING_ADDRESS;
    }

    @Override
    public MessagingAddress get(String namespace, String name) {
        return getOperation().inNamespace(namespace).withName(name).get();
    }

    public static MixedOperation<MessagingAddress, MessagingAddressList, DoneableMessagingAddress, Resource<MessagingAddress, DoneableMessagingAddress>> getOperation() {
        return Kubernetes.getClient().customResources(CustomResourceDefinitionContext.fromCrd(CoreCrd.messagingAddresses()), MessagingAddress.class, MessagingAddressList.class, DoneableMessagingAddress.class);
    }

    @Override
    public void create(MessagingAddress resource) {
        getOperation().inNamespace(resource.getMetadata().getNamespace()).createOrReplace(new MessagingAddressBuilder(resource)
                .editOrNewMetadata()
                .withNewResourceVersion("")
                .endMetadata()
                .withNewStatus()
                .endStatus()
                .build());
    }

    @Override
    public void delete(MessagingAddress resource) throws Exception {
        getOperation().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).cascading(true).delete();
    }

    @Override
    public boolean isReady(MessagingAddress address) {
        return address != null && address.getStatus() != null &&
                "Active".equals(address.getStatus().getPhase());
    }

    @Override
    public void refreshResource(MessagingAddress existing, MessagingAddress newResource) {
        existing.setMetadata(newResource.getMetadata());
        existing.setSpec(newResource.getSpec());
        existing.setStatus(newResource.getStatus());
    }

    public static MessagingAddressCondition getCondition(List<MessagingAddressCondition> conditions, String type) {
        for (MessagingAddressCondition condition : conditions) {
            if (type.equals(condition.getType())) {
                return condition;
            }
        }
        return null;
    }

    public static Map<String, String> getConditions(MessagingAddress infrastructure) {
        Map<String, String> conditions = new HashMap<>();
        for (String conditionName : List.of("FoundProject", "Validated", "Scheduled", "Created", "Ready")) {
            MessagingAddressCondition condition = MessagingAddressResourceType.getCondition(infrastructure.getStatus().getConditions(), conditionName);
            if (condition == null) {
                conditions.put(conditionName, "Unknown");
            } else {
                conditions.put(conditionName, condition.getStatus());
            }
        }
        return conditions;
    }
}
