/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceStatus;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.time.Duration;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ControllerChainTest {
    private TestAddressSpaceApi testApi;
    private Kubernetes kubernetes;

    @Before
    public void setup() {
        kubernetes = mock(Kubernetes.class);
        testApi = new TestAddressSpaceApi();

        when(kubernetes.withNamespace(anyString())).thenReturn(kubernetes);
        when(kubernetes.getNamespace()).thenReturn("myspace");
        when(kubernetes.existsNamespace(anyString())).thenReturn(true);
    }

    @Test
    public void testController() throws Exception {
        EventLogger testLogger = mock(EventLogger.class);
        ControllerChain controllerChain = new ControllerChain(kubernetes, testApi, new TestSchemaProvider(), testLogger, Duration.ofSeconds(5), Duration.ofSeconds(5));
        Controller mockController = mock(Controller.class);
        controllerChain.addController(mockController);

        AddressSpace a1 = new AddressSpace.Builder()
                .setName("myspace")
                .setType("type1")
                .setPlan("myplan")
                .setStatus(new AddressSpaceStatus(false))
                .build();

        AddressSpace a2 = new AddressSpace.Builder()
                .setName("myspace2")
                .setType("type1")
                .setPlan("myplan")
                .setStatus(new AddressSpaceStatus(false))
                .build();

        when(mockController.handle(eq(a1))).thenReturn(a1);
        when(mockController.handle(eq(a2))).thenReturn(a2);

        controllerChain.onUpdate(Sets.newSet(a1, a2));

        verify(mockController, times(2)).handle(any());
        verify(mockController).handle(eq(a1));
        verify(mockController).handle(eq(a2));
    }

}

