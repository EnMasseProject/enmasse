/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.common;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointSpec;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.openshift.client.ParameterValue;

import java.util.*;

/**
 * Interface for Kubernetes operations done by the address space controller
 */
public interface Kubernetes {

    String getNamespace();

    void create(KubernetesList resources);
    KubernetesList processTemplate(String templateName, ParameterValue ... parameterValues);

    boolean hasService(String service);
    boolean hasService(String infraUuid, String service);

    void deleteResourcesNotIn(String[] addressSpaces);

    Set<Deployment> getReadyDeployments();

    Optional<Secret> getSecret(String secretName);

    void createServiceAccount(String saName, Map<String, String> labels);
}
