/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra.resources;

import io.enmasse.address.model.CoreCrd;
import io.enmasse.api.model.DoneableMessagingInfra;
import io.enmasse.api.model.MessagingInfra;
import io.enmasse.api.model.MessagingInfraBuilder;
import io.enmasse.api.model.MessagingInfraCondition;
import io.enmasse.api.model.MessagingInfraList;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.List;

public class MessagingInfraResourceType implements ResourceType<MessagingInfra> {
    private static final MixedOperation<MessagingInfra, MessagingInfraList, DoneableMessagingInfra, Resource<MessagingInfra, DoneableMessagingInfra>> operation = Kubernetes.getInstance().getClient().customResources(CoreCrd.messagingInfras(), MessagingInfra.class, MessagingInfraList.class, DoneableMessagingInfra.class);

    @Override
    public String getKind() {
        return "MessagingInfra";
    }

    @Override
    public MessagingInfra get(String namespace, String name) {
        return operation.inNamespace(namespace).withName(name).get();
    }

    public static MessagingInfra getDefault() {
        return new MessagingInfraBuilder()
                .editOrNewMetadata()
                .withName("default-infra")
                .withNamespace(Environment.getInstance().namespace())
                .endMetadata()
                .editOrNewSpec()
                .endSpec()
                .build();
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
    public void delete(MessagingInfra resource) {
        operation.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).cascading(true).delete();
    }

    @Override
    public boolean isReady(MessagingInfra infra) {
        return infra != null &&
                infra.getStatus() != null &&
                "Active".equals(infra.getStatus().getPhase());
    }

    @Override
    public void refreshResource(MessagingInfra existing, MessagingInfra newResource) {
        existing.setMetadata(newResource.getMetadata());
        existing.setSpec(newResource.getSpec());
        existing.setStatus(newResource.getStatus());
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
