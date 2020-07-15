/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingAddress;
import io.enmasse.api.model.MessagingAddressBuilder;
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingInfrastructure;
import io.enmasse.api.model.MessagingInfrastructureBuilder;
import io.enmasse.api.model.MessagingProject;
import io.enmasse.api.model.MessagingProjectBuilder;
import io.enmasse.systemtest.TestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DeleteTest extends TestBase  {

    private static final String INFRA_NAMESPACE = "enmasse-infra";
    private static final String PROJECT_NAMESPACE = "project-namespace";

    private final MessagingInfrastructure infra = new MessagingInfrastructureBuilder()

            .withNewMetadata()
            .withNamespace(INFRA_NAMESPACE)
            .withName("default")
            .endMetadata()

            .withNewSpec().endSpec()

            .build();

    private final MessagingProject project = new MessagingProjectBuilder()

            .withNewMetadata()
            .withNamespace(PROJECT_NAMESPACE)
            .withName("default")
            .endMetadata()

            .withNewSpec().endSpec()

            .build();

    private final MessagingEndpoint endpoint = new MessagingEndpointBuilder()

            .withNewMetadata()
            .withNamespace(PROJECT_NAMESPACE)
            .withName("cluster")
            .endMetadata()

            .withNewSpec()
            .withNewCluster().endCluster()

            .addNewProtocol("AMQP")

            .endSpec()

            .build();

    private final MessagingAddress address = new MessagingAddressBuilder()

            .withNewMetadata()
            .withNamespace(PROJECT_NAMESPACE)
            .withName("cluster")
            .endMetadata()

            .withNewSpec()
            .withNewQueue().endQueue()

            .endSpec()

            .build();

    @Test
    public void testDeleteEndpointFirst(final ExtensionContext context) throws Exception {

        // create and wait

        resourceManager.createResource(context, true, infra, project, endpoint, address);

        // delete endpoint first

        resourceManager.deleteResource(endpoint);

        // delete all the rest

        resourceManager.deleteResourcesParallel(infra, project, address);

    }

    @Test
    @Disabled("Deleting the project first gets stuck")
    public void testDeleteProjectFirst(final ExtensionContext context) throws Exception {

        // create and wait

        resourceManager.createResource(context, true, infra, project, endpoint, address);

        // delete endpoint first

        resourceManager.deleteResource(project);

        // delete all the rest

        resourceManager.deleteResourcesParallel(infra, endpoint, address);

    }

    @Test
    @Disabled("Deleting the infrastructure first gets stuck")
    public void testDeleteInfraFirst(final ExtensionContext context) throws Exception {

        // create and wait

        resourceManager.createResource(context, true, infra, project, endpoint, address);

        // delete endpoint first

        resourceManager.deleteResource(infra);

        // delete all the rest

        resourceManager.deleteResourcesParallel(project, endpoint, address);

    }

}
