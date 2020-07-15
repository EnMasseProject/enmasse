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
import io.enmasse.systemtest.platform.Kubernetes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TwoProjectsTest extends TestBase {

    private static final String INFRA_NAMESPACE = "enmasse-infra";
    private static final String PROJECT_NAMESPACE = "project-namespace";
    private static final String PROJECT_NAMESPACE_2 = "project-namespace-2";

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
            .withHost(Kubernetes.getInstance().getHost())
            .withNewNodePort().endNodePort()

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

    /**
     * Test that creating and immediately destroying resources works.
     */
    @Test
    public void testCreateTwoProjectsWithSameHost(final ExtensionContext context) throws Exception {

        // create and wait

        resourceManager.createResource(context, true, infra, project, endpoint, address);

        var project2 = new MessagingProjectBuilder(project)
                .editMetadata().withNamespace(PROJECT_NAMESPACE_2).endMetadata()
                .build();
        var endpoint2 = new MessagingEndpointBuilder(endpoint)
                .editMetadata().withNamespace(PROJECT_NAMESPACE_2).endMetadata()
                .build();
        var address2 = new MessagingAddressBuilder(address)
                .editMetadata().withNamespace(PROJECT_NAMESPACE_2).endMetadata()
                .build();

        // create second project

        resourceManager.createResource(context, true, project2, endpoint2, address2);

        // delete everything right away, all at once, then wait

        resourceManager.deleteResourcesParallel(project2, endpoint2, address2, infra, project, endpoint, address);

    }


}
