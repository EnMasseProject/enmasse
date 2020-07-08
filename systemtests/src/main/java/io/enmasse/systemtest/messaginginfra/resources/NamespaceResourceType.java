/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra.resources;

import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;

import java.util.UUID;

public class NamespaceResourceType implements ResourceType<Namespace> {
    @Override
    public String getKind() {
        return "Namespace";
    }

    @Override
    public Namespace get(String namespace, String name) {
        return Kubernetes.getClient().namespaces().withName(name).get();
    }

    public static Namespace getDefault() {
        return new NamespaceBuilder().withNewMetadata().withName(String.format("enmasse-project-%s", UUID.randomUUID().toString().split("-")[0])).addToLabels("app", "enmasse").endMetadata().build();
    }

    @Override
    public void create(Namespace resource) {
        Kubernetes.getClient().namespaces().create(resource);
    }

    @Override
    public void delete(Namespace resource) throws Exception {
        Kubernetes.getInstance().deleteNamespace(resource.getMetadata().getName());
    }

    @Override
    public boolean isReady(Namespace resource) {
        return resource != null;
    }

    @Override
    public void refreshResource(Namespace existing, Namespace newResource) {
        existing.setMetadata(newResource.getMetadata());
        existing.setSpec(newResource.getSpec());
        existing.setStatus(newResource.getStatus());
    }
}
