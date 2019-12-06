/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;

import io.enmasse.address.model.AuthenticationService;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import org.junit.jupiter.api.Test;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.Kubernetes;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

public class StatusControllerTest {

    @Test
    public void testStatusControllerSetsNotReady() throws Exception {
        InfraResourceFactory infraResourceFactory = mock(InfraResourceFactory.class);
        Kubernetes kubernetes = mock(Kubernetes.class);

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName("mydepl1")
                .endMetadata()
                .withNewStatus()
                .withUnavailableReplicas(1)
                .withAvailableReplicas(0)
                .endStatus()
                .build();

        when(kubernetes.getReadyDeployments(new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("a")
                .withNamespace("b")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()

                .withNewSpec()
                .withPlan("c")
                .withType("d")
                .endSpec()

                .build()))
        .thenReturn(Collections.emptySet());

        AuthenticationServiceRegistry authenticationServiceRegistry = mock(AuthenticationServiceRegistry.class);
        when(authenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.empty());
        StatusController controller = new StatusController(kubernetes, new TestSchemaProvider(), infraResourceFactory, authenticationServiceRegistry, null);

        AuthenticationService authenticationService = new AuthenticationService();
        authenticationService.setType(AuthenticationServiceType.NONE);
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .withAuthenticationService(authenticationService)
                .endSpec()
                .build();

        when(infraResourceFactory.createInfraResources(eq(addressSpace), any(), any())).thenReturn(Collections.singletonList(deployment));

        assertFalse(addressSpace.getStatus().isReady());
        controller.reconcileAnyState(addressSpace);
        assertFalse(addressSpace.getStatus().isReady());
    }
}
