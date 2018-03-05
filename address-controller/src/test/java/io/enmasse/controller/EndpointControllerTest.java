/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RouteListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EndpointControllerTest {

    @Test
    public void testRoutesCreated() {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setType("type1")
                .setPlan("myplan")
                .build();


        OpenShiftClient client = mock(OpenShiftClient.class);
        when(client.isAdaptable(OpenShiftClient.class)).thenReturn(true);
        when(client.adapt(OpenShiftClient.class)).thenReturn(client);

        EndpointController controller = new EndpointController(client);

        Route route = new RouteBuilder()
                .editOrNewMetadata()
                .withName("myservice-external")
                .withNamespace(addressSpace.getNamespace())
                .addToLabels(LabelKeys.TYPE, "loadbalancer")
                .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpace.getName())
                .addToAnnotations(AnnotationKeys.SERVICE_NAME, "messaging")
                .addToAnnotations(AnnotationKeys.CERT_PROVIDER, "selfsigned")
                .addToAnnotations(AnnotationKeys.CERT_SECRET_NAME, "mysecret")
                .endMetadata()
                .editOrNewSpec()
                .withHost("messaging.example.com")
                .withNewTo()
                .withName("messaging")
                .endTo()
                .endSpec()
                .build();


        MixedOperation op = mock(MixedOperation.class);
        when(client.routes()).thenReturn(op);
        when(op.inNamespace(any())).thenReturn(op);
        when(op.list()).thenReturn(new RouteListBuilder().addToItems(route).build());

        AddressSpace newspace = controller.handle(addressSpace);

        assertThat(newspace.getEndpoints().size(), is(1));
        assertThat(newspace.getEndpoints().get(0).getName(), is("myservice-external"));
        assertTrue(newspace.getEndpoints().get(0).getHost().isPresent());
        assertThat(newspace.getEndpoints().get(0).getHost().get(), is("messaging.example.com"));
        assertTrue(newspace.getEndpoints().get(0).getCertSpec().isPresent());
    }
}
