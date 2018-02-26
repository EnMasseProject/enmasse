/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;


import io.enmasse.address.model.AddressSpace;
import io.enmasse.controller.common.Kubernetes;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatusControllerTest {

    @Test
    public void testStatusControllerSetsReady() throws Exception {
        InfraResourceFactory infraResourceFactory = mock(InfraResourceFactory.class);
        Kubernetes kubernetes = mock(Kubernetes.class);
        when(kubernetes.withNamespace(eq("mynamespace"))).thenReturn(kubernetes);

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName("mydepl1")
                .endMetadata()
                .withNewStatus()
                .withAvailableReplicas(1)
                .endStatus()
                .build();

        when(kubernetes.getReadyDeployments()).thenReturn(Collections.singleton(deployment));

        StatusController controller = new StatusController(kubernetes,  infraResourceFactory);

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setType("type1")
                .setPlan("myplan")
                .build();

        when(infraResourceFactory.createResourceList(eq(addressSpace))).thenReturn(Collections.singletonList(deployment));

        assertFalse(addressSpace.getStatus().isReady());
        controller.handle(addressSpace);
        assertTrue(addressSpace.getStatus().isReady());
    }

    @Test
    public void testStatusControllerSetsNotReady() throws Exception {
        InfraResourceFactory infraResourceFactory = mock(InfraResourceFactory.class);
        Kubernetes kubernetes = mock(Kubernetes.class);
        when(kubernetes.withNamespace(eq("mynamespace"))).thenReturn(kubernetes);

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName("mydepl1")
                .endMetadata()
                .withNewStatus()
                .withUnavailableReplicas(1)
                .withAvailableReplicas(0)
                .endStatus()
                .build();

        when(kubernetes.getReadyDeployments()).thenReturn(Collections.emptySet());

        StatusController controller = new StatusController(kubernetes,  infraResourceFactory);

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setType("type1")
                .setPlan("myplan")
                .build();

        when(infraResourceFactory.createResourceList(eq(addressSpace))).thenReturn(Collections.singletonList(deployment));

        assertFalse(addressSpace.getStatus().isReady());
        controller.handle(addressSpace);
        assertFalse(addressSpace.getStatus().isReady());
    }
}
