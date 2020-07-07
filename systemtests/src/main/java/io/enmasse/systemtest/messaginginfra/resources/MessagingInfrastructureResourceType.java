/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra.resources;

import io.enmasse.address.model.CoreCrd;
import io.enmasse.api.model.DoneableMessagingInfrastructure;
import io.enmasse.api.model.MessagingInfrastructure;
import io.enmasse.api.model.MessagingInfrastructureBuilder;
import io.enmasse.api.model.MessagingInfrastructureCondition;
import io.enmasse.api.model.MessagingInfrastructureList;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.List;

public class MessagingInfrastructureResourceType implements ResourceType<MessagingInfrastructure> {
    private static final MixedOperation<MessagingInfrastructure, MessagingInfrastructureList, DoneableMessagingInfrastructure, Resource<MessagingInfrastructure, DoneableMessagingInfrastructure>> operation = Kubernetes.getClient().customResources(CoreCrd.messagingInfras(), MessagingInfrastructure.class, MessagingInfrastructureList.class, DoneableMessagingInfrastructure.class);

    @Override
    public String getKind() {
        return "MessagingInfrastructure";
    }

    @Override
    public MessagingInfrastructure get(String namespace, String name) {
        return operation.inNamespace(namespace).withName(name).get();
    }

    public static MessagingInfrastructure getDefault() {
        return new MessagingInfrastructureBuilder()
                .editOrNewMetadata()
                .withName("default-infra")
                .withNamespace(Environment.getInstance().namespace())
                .endMetadata()
                .editOrNewSpec()
                .endSpec()
                .build();
    }

    public static MixedOperation<MessagingInfrastructure, MessagingInfrastructureList, DoneableMessagingInfrastructure, Resource<MessagingInfrastructure, DoneableMessagingInfrastructure>> getOperation() {
        return operation;
    }

    @Override
    public void create(MessagingInfrastructure resource) {
        operation.inNamespace(resource.getMetadata().getNamespace()).createOrReplace(new MessagingInfrastructureBuilder(resource)
                .editOrNewMetadata()
                .withNewResourceVersion("")
                .endMetadata()
                .withNewStatus()
                .endStatus()
                .build());
    }

    @Override
    public void delete(MessagingInfrastructure resource) {
        operation.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).cascading(true).delete();
    }

    @Override
    public boolean isReady(MessagingInfrastructure infra) {
        return infra != null &&
                infra.getStatus() != null &&
                "Active".equals(infra.getStatus().getPhase());
    }

    @Override
    public void refreshResource(MessagingInfrastructure existing, MessagingInfrastructure newResource) {
        existing.setMetadata(newResource.getMetadata());
        existing.setSpec(newResource.getSpec());
        existing.setStatus(newResource.getStatus());
    }

    public static MessagingInfrastructureCondition getCondition(List<MessagingInfrastructureCondition> conditions, String type) {
        for (MessagingInfrastructureCondition condition : conditions) {
            if (type.equals(condition.getType())) {
                return condition;
            }
        }
        return null;
    }
}
