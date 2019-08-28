/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.service.base;

import static io.enmasse.iot.model.v1.IoTCrd.project;
import static java.util.Optional.empty;

import java.util.Optional;

import io.enmasse.iot.model.v1.DoneableIoTProject;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectList;
import io.enmasse.model.CustomResourceDefinitions;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public final class IoTProjects {

    static {
        CustomResourceDefinitions.registerAll();
    }

    private IoTProjects() {}

    public static MixedOperation<IoTProject, IoTProjectList, DoneableIoTProject, Resource<IoTProject, DoneableIoTProject>> clientForProject(final KubernetesClient client) {
        return client
                .customResources(project(),
                        IoTProject.class, IoTProjectList.class, DoneableIoTProject.class);
    }

    public static Optional<IoTProject> getProject(final KubernetesClient client, final String tenantName) {
        return getProject(clientForProject(client), tenantName);
   }

    public static Optional<IoTProject> getProject(final MixedOperation<IoTProject, IoTProjectList, DoneableIoTProject, Resource<IoTProject, DoneableIoTProject>> projects,
            final String tenantName) {

        final String[] toks = tenantName.split("\\.", 2);

        if (toks.length < 2) {
            return empty();
        }

        final String namespace = toks[0];
        final String name = toks[1];

        final IoTProject project = projects
                .inNamespace(namespace)
                .withName(name)
                .get();

        return Optional.ofNullable(project);

    }

}
