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
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.List;

public class MessagingAddressResourceType implements ResourceType<MessagingAddress> {
    private static final MixedOperation<MessagingAddress, MessagingAddressList, DoneableMessagingAddress, Resource<MessagingAddress, DoneableMessagingAddress>> operation = Kubernetes.getClient().customResources(CoreCrd.messagingAddresses(), MessagingAddress.class, MessagingAddressList.class, DoneableMessagingAddress.class);

    @Override
    public String getKind() {
        return "MessagingAddress";
    }

    @Override
    public MessagingAddress get(String namespace, String name) {
        return operation.inNamespace(namespace).withName(name).get();
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
    public void delete(MessagingAddress resource) throws Exception {
        operation.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).cascading(true).delete();
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
}
