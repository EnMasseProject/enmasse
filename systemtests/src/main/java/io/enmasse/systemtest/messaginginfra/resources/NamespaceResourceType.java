/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra.resources;

import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.api.model.Namespace;

public class NamespaceResourceType implements ResourceType<Namespace> {
    @Override
    public String getKind() {
        return "Namespace";
    }

    @Override
    public void create(Namespace resource) {
        Kubernetes.getInstance().getClient().namespaces().create(resource);
    }

    @Override
    public void delete(Namespace resource) throws Exception {
        Kubernetes.getInstance().deleteNamespace(resource.getMetadata().getName());
    }

    @Override
    public void waitReady(Namespace resource) {
    }
}
