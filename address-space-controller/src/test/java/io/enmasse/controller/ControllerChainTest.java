/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ControllerChainTest {
    private TestAddressSpaceApi testApi;
    private Kubernetes kubernetes;

    @BeforeEach
    public void setup() {
        kubernetes = mock(Kubernetes.class);
        testApi = new TestAddressSpaceApi();

        when(kubernetes.getNamespace()).thenReturn("myspace");
    }

    @Test
    public void testController() throws Exception {
        EventLogger testLogger = mock(EventLogger.class);
        ControllerChain controllerChain = new ControllerChain(testApi, new TestSchemaProvider(), testLogger, Duration.ofSeconds(5), Duration.ofSeconds(5));
        Controller mockController = mock(Controller.class);
        controllerChain.addController(mockController);

        AddressSpace a1 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .withNewStatus(false)

                .build();

        AddressSpace a2 = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace2")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .endSpec()
                .withNewStatus(false)
                .build();

        when(mockController.reconcileAnyState(eq(a1))).thenReturn(a1);
        when(mockController.reconcileAnyState(eq(a2))).thenReturn(a2);

        controllerChain.onUpdate(Arrays.asList(a1, a2));

        verify(mockController, times(2)).reconcileAnyState(any());
        verify(mockController).reconcileAnyState(eq(a1));
        verify(mockController).reconcileAnyState(eq(a2));


    }
}

