/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.common;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.enmasse.api.auth.SubjectAccessReview;
import io.enmasse.api.auth.TokenReview;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.openshift.client.ParameterValue;

import java.util.*;

/**
 * Interface for Kubernetes operations done by the address controller
 */
public interface Kubernetes {

    String getNamespace();
    Kubernetes withNamespace(String namespace);

    void create(KubernetesList resources, String namespace);
    void delete(KubernetesList resources);
    KubernetesList processTemplate(String templateName, ParameterValue ... parameterValues);

    Set<NamespaceInfo> listAddressSpaces();
    void deleteNamespace(NamespaceInfo namespaceInfo);
    void createNamespace(AddressSpace addressSpace);

    boolean existsNamespace(String namespace);

    boolean hasService(String service);

    HasMetadata createEndpoint(Endpoint endpoint, Service service, String addressSpaceName, String namespace);

    Set<Deployment> getReadyDeployments();

    Optional<Secret> getSecret(String secretName);

    Optional<Secret> getSecret(String secretName, String namespace);

    boolean isRBACEnabled();
    void addAddressSpaceRoleBindings(AddressSpace namespace);
    void addSystemImagePullerPolicy(String namespace, AddressSpace tenantNamespace);

    void addAddressSpaceAdminRoleBinding(AddressSpace addressSpace);

    String getAddressSpaceAdminSa();

    void createServiceAccount(String namespace, String addressSpaceAdminSa);
}
