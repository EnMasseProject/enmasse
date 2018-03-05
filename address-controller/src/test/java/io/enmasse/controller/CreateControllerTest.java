/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.SchemaApi;
import io.enmasse.k8s.api.TestSchemaApi;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CreateControllerTest {

    @Test
    public void testAddressSpaceCreate() throws Exception {
        Kubernetes kubernetes = mock(Kubernetes.class);
        when(kubernetes.withNamespace(any())).thenReturn(kubernetes);
        when(kubernetes.hasService(any())).thenReturn(false);
        when(kubernetes.getNamespace()).thenReturn("otherspace");

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setType("type1")
                .setPlan("myplan")
                .build();


        EventLogger eventLogger = mock(EventLogger.class);
        InfraResourceFactory mockResourceFactory = mock(InfraResourceFactory.class);
        when(mockResourceFactory.createResourceList(eq(addressSpace))).thenReturn(Collections.emptyList());

        SchemaProvider testSchema = new TestSchemaProvider();
        CreateController createController = new CreateController(kubernetes, testSchema, mockResourceFactory, "test", eventLogger);

        createController.handle(addressSpace);

        ArgumentCaptor<AddressSpace> addressSpaceArgumentCaptor = ArgumentCaptor.forClass(AddressSpace.class);
        verify(kubernetes).createNamespace(addressSpaceArgumentCaptor.capture());
        AddressSpace value = addressSpaceArgumentCaptor.getValue();
        assertThat(value.getName(), is("myspace"));
        assertThat(value.getNamespace(), is("mynamespace"));
    }
}
