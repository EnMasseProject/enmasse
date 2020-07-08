/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra.resources;

import io.enmasse.address.model.CoreCrd;
import io.enmasse.api.model.DoneableMessagingProject;
import io.enmasse.api.model.MessagingProject;
import io.enmasse.api.model.MessagingProjectBuilder;
import io.enmasse.api.model.MessagingProjectCondition;
import io.enmasse.api.model.MessagingProjectList;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.List;

public class MessagingProjectResourceType implements ResourceType<MessagingProject> {
    private static final MixedOperation<MessagingProject, MessagingProjectList, DoneableMessagingProject, Resource<MessagingProject, DoneableMessagingProject>> operation = Kubernetes.getClient().customResources(CoreCrd.messagingProjects(), MessagingProject.class, MessagingProjectList.class, DoneableMessagingProject.class);

    @Override
    public String getKind() {
        return "MessagingProject";
    }

    @Override
    public MessagingProject get(String namespace, String name) {
        return operation.inNamespace(namespace).withName(name).get();
    }

    public static MessagingProject getDefault() {
        return new MessagingProjectBuilder()
                .editOrNewMetadata()
                .withName("default")
                .withNamespace(NamespaceResourceType.getDefault().getMetadata().getName())
                .endMetadata()
                .build();
    }

    public static MixedOperation<MessagingProject, MessagingProjectList, DoneableMessagingProject, Resource<MessagingProject, DoneableMessagingProject>> getOperation() {
        return operation;
    }

    @Override
    public void create(MessagingProject resource) {
        operation.inNamespace(resource.getMetadata().getNamespace()).createOrReplace(new MessagingProjectBuilder(resource)
                .editOrNewMetadata()
                .withNewResourceVersion("")
                .endMetadata()
                .withNewStatus()
                .endStatus()
                .build());
    }

    @Override
    public void delete(MessagingProject resource) throws InterruptedException {
        operation.inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).cascading(true).delete();
    }

    @Override
    public boolean isReady(MessagingProject infra) {
        return infra != null &&
                infra.getStatus() != null &&
                "Active".equals(infra.getStatus().getPhase());
    }

    @Override
    public void refreshResource(MessagingProject existing, MessagingProject newResource) {
        existing.setMetadata(newResource.getMetadata());
        existing.setSpec(newResource.getSpec());
        existing.setStatus(newResource.getStatus());
    }

    public static MessagingProjectCondition getCondition(List<MessagingProjectCondition> conditions, String type) {
        for (MessagingProjectCondition condition : conditions) {
            if (type.equals(condition.getType())) {
                return condition;
            }
        }
        return null;
    }
}
