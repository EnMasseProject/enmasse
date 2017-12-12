/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.controller.common;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.openshift.client.ParameterValue;

import java.time.Clock;
import java.util.*;

/**
 * Interface for Kubernetes operations done by the address controller
 */
public interface Kubernetes {

    String getNamespace();
    Kubernetes withNamespace(String namespace);

    void create(HasMetadata ... resources);
    void create(KubernetesList resources);
    void create(KubernetesList resources, String namespace);
    void delete(KubernetesList resources);
    void delete(HasMetadata ... resources);
    KubernetesList processTemplate(String templateName, ParameterValue ... parameterValues);

    Set<NamespaceInfo> listAddressSpaces();
    void deleteNamespace(NamespaceInfo namespaceInfo);
    void createNamespace(AddressSpace addressSpace);

    boolean existsNamespace(String namespace);

    boolean hasService(String service);

    void createEndpoint(Endpoint endpoint, Service service, String addressSpaceName, String namespace);

    Set<Deployment> getReadyDeployments();

    Optional<Secret> getSecret(String secretName);

    TokenReview performTokenReview(String token);

    SubjectAccessReview performSubjectAccessReview(String user, String namespace, String verb, String impersonateUser);

    boolean isRBACSupported();
    void addAddressSpaceRoleBindings(AddressSpace namespace);
    void addSystemImagePullerPolicy(String namespace, AddressSpace tenantNamespace);

    EventLogger createEventLogger(Clock clock, String componentName);

    void addAddressSpaceAdminRoleBinding(AddressSpace addressSpace);

    String getAddressSpaceAdminSa();

    void createServiceAccount(String namespace, String addressSpaceAdminSa);
}
