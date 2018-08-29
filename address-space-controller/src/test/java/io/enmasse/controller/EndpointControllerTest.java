/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.internal.NamespaceOperationsImpl;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EndpointControllerTest {

    private KubernetesClient client;

    @Before
    public void setup() {
        client = mock(KubernetesClient.class);
    }

    @Test
    public void testRoutesNotCreated() {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .putAnnotation(AnnotationKeys.INFRA_UUID, "1234")
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .setServicePort("amqps")
                        .build())
                .setType("type1")
                .setPlan("myplan")
                .build();


        MixedOperation<Service, ServiceList, DoneableService, Resource<Service, DoneableService>> op = mock(MixedOperation.class);
        when(client.services()).thenReturn(op);
        when(op.inNamespace(any())).thenReturn(op);
        FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> r = mock(FilterWatchListDeletable.class);
        when(op.withLabel(eq(LabelKeys.INFRA_UUID), eq("1234"))).thenReturn(r);
        when(client.getNamespace()).thenReturn("myns");

        Service service = new ServiceBuilder()
                .editOrNewMetadata()
                .withName("messaging")
                .addToAnnotations(AnnotationKeys.SERVICE_PORT_PREFIX + "amqps", "5671")
                .addToLabels(LabelKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("amqps")
                .withPort(1234)
                .withNewTargetPort("amqps")
                .endPort()
                .addToSelector("component", "router")
                .endSpec()
                .build();

        when(r.list()).thenReturn(new ServiceListBuilder().addNewItemLike(service).endItem().build());

        EndpointController controller = new EndpointController(client, false);

        AddressSpace newspace = controller.handle(addressSpace);

        assertThat(newspace.getStatus().getEndpointStatuses().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getName(), is("myendpoint"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServiceHost(), is("messaging.myns.svc"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getPort(), is(0));
    }

    @Test
    public void testExternalCreated() {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .putAnnotation(AnnotationKeys.INFRA_UUID, "1234")
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .setServicePort("amqps")
                        .build())
                .setType("type1")
                .setPlan("myplan")
                .build();


        Service service = new ServiceBuilder()
                .editOrNewMetadata()
                .withName("messaging")
                .addToAnnotations(AnnotationKeys.SERVICE_PORT_PREFIX + "amqps", "5671")
                .addToLabels(LabelKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("amqps")
                .withPort(1234)
                .withNewTargetPort("amqps")
                .endPort()
                .addToSelector("component", "router")
                .endSpec()
                .build();

        MixedOperation<Service, ServiceList, DoneableService, Resource<Service, DoneableService>> op = mock(MixedOperation.class);
        when(client.services()).thenReturn(op);
        when(op.inNamespace(any())).thenReturn(op);
        FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> r = mock(FilterWatchListDeletable.class);
        when(op.withLabel(eq(LabelKeys.INFRA_UUID), eq("1234"))).thenReturn(r);
        when(client.getNamespace()).thenReturn("myns");
        when(r.list()).thenReturn(new ServiceListBuilder().addNewItemLike(service).endItem().build());

        Resource<Service, DoneableService> rexternal = mock(Resource.class);
        when(op.withName(eq("myendpoint-1234-external"))).thenReturn(rexternal);
        when(rexternal.get()).thenReturn(null);

        Resource<Service, DoneableService> rinternal = mock(Resource.class);
        when(op.withName(eq("messaging"))).thenReturn(rinternal);
        when(rinternal.get()).thenReturn(service);

        EndpointController controller = new EndpointController(client, true);

        AddressSpace newspace = controller.handle(addressSpace);

        assertThat(newspace.getStatus().getEndpointStatuses().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getName(), is("myendpoint"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServiceHost(), is("messaging.myns.svc"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
    }
}
