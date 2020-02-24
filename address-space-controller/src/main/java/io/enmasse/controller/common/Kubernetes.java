/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.common;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.controller.AppliedConfig;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;

/**
 * Interface for Kubernetes operations done by the address space controller
 */
public interface Kubernetes {

    String getNamespace();

    void create(KubernetesList resources);
    void apply(KubernetesList resourceList, boolean patchPersistentVolumeClaims);
    KubernetesList processTemplate(String templateName, Map<String, String> parameters);

    void deleteResources(String infraUuid);

    Set<Deployment> getReadyDeployments(AddressSpace addressSpace);
    Set<StatefulSet> getReadyStatefulSets(AddressSpace addressSpace);

    Optional<Secret> getSecret(String secretName);

    boolean existsAddressSpace(AddressSpace addressSpace);

    InfraConfig getAppliedInfraConfig(AddressSpace addressSpace) throws IOException;
    AppliedConfig getAppliedConfig(AddressSpace addressSpace) throws IOException;

    String getInfraUuid(AddressSpace addressSpace);
}
