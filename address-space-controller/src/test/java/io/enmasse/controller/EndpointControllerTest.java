/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class EndpointControllerTest {

    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    private KubernetesClient client;

    @Before
    public void setup() {
        client = server.getClient();
    }

    @Test
    public void testRoutesNotCreated() {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .putAnnotation(AnnotationKeys.NAMESPACE, "myns")
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .setServicePort("amqps")
                        .build())
                .setType("type1")
                .setPlan("myplan")
                .build();


        client.services().inNamespace("myns").createNew()
                .editOrNewMetadata()
                .withName("messaging")
                .addToAnnotations(AnnotationKeys.SERVICE_PORT_PREFIX + "amqps", "5671")
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("amqps")
                .withPort(1234)
                .withNewTargetPort("amqps")
                .endPort()
                .addToSelector("component", "router")
                .endSpec()
                .done();

        EndpointController controller = new EndpointController(client, false);

        AddressSpace newspace = controller.handle(addressSpace);

        assertThat(newspace.getStatus().getEndpointStatuses().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getName(), is("myendpoint"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServiceHost(), is("messaging.myns.svc"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getPort(), is(0));
        assertNull(client.services().inNamespace("myns").withName("myendpoint-external").get());
    }

    @Test
    public void testExternalCreated() {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .putAnnotation(AnnotationKeys.NAMESPACE, "myns")
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .setServicePort("amqps")
                        .build())
                .setType("type1")
                .setPlan("myplan")
                .build();


        client.services().inNamespace("myns").createNew()
                .editOrNewMetadata()
                .withName("messaging")
                .addToAnnotations(AnnotationKeys.SERVICE_PORT_PREFIX + "amqps", "5671")
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("amqps")
                .withPort(1234)
                .withNewTargetPort("amqps")
                .endPort()
                .addToSelector("component", "router")
                .endSpec()
                .done();

        EndpointController controller = new EndpointController(client, true);

        AddressSpace newspace = controller.handle(addressSpace);

        assertThat(newspace.getStatus().getEndpointStatuses().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getName(), is("myendpoint"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServiceHost(), is("messaging.myns.svc"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getPort(), is(1234));
        assertNotNull(client.services().inNamespace("myns").withName("myendpoint-external").get());
    }
}
