/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import io.enmasse.address.model.*;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.util.JULInitializingTest;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;

public class EndpointControllerTest extends JULInitializingTest {

    private OpenShiftClient client;
    private OpenShiftServer openShiftServer = new OpenShiftServer(false, true);

    @BeforeEach
    void setup() {
        openShiftServer.before();
        client = openShiftServer.getOpenshiftClient();
    }

    @AfterEach
    void tearDown() {
        openShiftServer.after();
    }

    @Test
    public void testRoutesNotCreated() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()

                .withNewMetadata()
                .withName("myspace")
                .withNamespace("mynamespace")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()

                .withNewSpec()
                .addToEndpoints(new EndpointSpecBuilder()
                        .withName("myendpoint")
                        .withService("messaging")
                        .build())
                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .build();

        Service service = new ServiceBuilder()
                .editOrNewMetadata()
                .withName("messaging-1234")
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

        client.services().create(service);

        EndpointController controller = new EndpointController(client, false, false);

        AddressSpace newspace = controller.reconcileAnyState(addressSpace);

        assertThat(newspace.getStatus().getEndpointStatuses().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getName(), is("myendpoint"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServiceHost(), is("messaging-1234.test.svc"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
        assertNull(newspace.getStatus().getEndpointStatuses().get(0).getExternalHost());
        assertTrue(newspace.getStatus().getEndpointStatuses().get(0).getExternalPorts().isEmpty());
    }

    @Test
    public void testExternalLoadBalancerCreated() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()

                .withNewMetadata()
                .withName("myspace")
                .withNamespace("mynamespace")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()

                .withNewSpec()
                .addNewEndpoint()
                        .withName("myendpoint")
                        .withService("messaging")
                        .withExpose(new ExposeSpecBuilder()
                                .withType(ExposeType.loadbalancer)
                                .withLoadBalancerPorts(Arrays.asList("amqps"))
                                .build())
                .endEndpoint()
                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .build();


        Service service = new ServiceBuilder()
                .editOrNewMetadata()
                .withName("messaging-1234")
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

        client.services().create(service);

        EndpointController controller = new EndpointController(client, true, false);

        AddressSpace newspace = controller.reconcileAnyState(addressSpace);

        assertThat(newspace.getStatus().getEndpointStatuses().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getName(), is("myendpoint"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServiceHost(), is("messaging-1234.test.svc"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getExternalPorts().size(), is(1));
    }

    public void testExternalRouteCreated() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()

                .withNewMetadata()
                .withName("myspace")
                .withNamespace("mynamespace")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()

                .withNewSpec()
                .addNewEndpoint()
                        .withName("myendpoint")
                        .withService("messaging")
                        .withExpose(new ExposeSpecBuilder()
                                .withType(ExposeType.route)
                                .withRouteHost("host1.example.com")
                                .withRouteServicePort("amqps")
                                .withRouteTlsTermination(TlsTermination.passthrough)
                                .build())
                .endEndpoint()
                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .build();


        Service service = new ServiceBuilder()
                .editOrNewMetadata()
                .withName("messaging-1234")
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

        client.services().create(service);

        EndpointController controller = new EndpointController(client, true, true);

        AddressSpace newspace = controller.reconcileAnyState(addressSpace);

        assertThat(newspace.getStatus().getEndpointStatuses().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getName(), is("myendpoint"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServiceHost(), is("messaging-1234.test.svc"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getExternalPorts().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getExternalPorts().get("amqps"), is(443));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getExternalHost(), is("host1.example.com"));
    }

    @Test
    public void testConsoleEndpointResourcesRemoved_Route() throws Exception {

        String serviceName = "console-1234";
        String secretName = "consolesecret";

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .withNamespace("mynamespace")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()

                .withNewSpec()
                .addToEndpoints(new EndpointSpecBuilder()
                        .withName("console")
                        .withService("console")
                        .withNewCert("selfsigned", secretName, null, null)
                        .withExpose(new ExposeSpecBuilder()
                                .withType(ExposeType.route)
                                .build())
                        .build())

                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .build();

        Service service = new ServiceBuilder()
                .editOrNewMetadata()
                .withName(serviceName)
                .addToLabels(LabelKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("https")
                .withPort(1234)
                .withNewTargetPort("https")
                .endPort()
                .addToSelector("component", "router")
                .endSpec()
                .build();
        client.services().create(service);

        Secret secret = new SecretBuilder()
                .editOrNewMetadata()
                .withName(secretName)
                .addToLabels(LabelKeys.INFRA_UUID, "1234")
                .endMetadata()
                .build();
        client.secrets().create(secret);


        // Problem with adapting the client prevents testing this path
//        Route route = new RouteBuilder()
//                .editOrNewMetadata()
//                .withName(serviceName)
//                .addToLabels(LabelKeys.INFRA_UUID, "1234")
//                .endMetadata()
//                .build();
//        client.adapt(OpenShiftClient.class).routes().create(route);

        EndpointController controller = new EndpointController(client, false, false);

        AddressSpace newspace = controller.reconcileAnyState(addressSpace);

        assertThat(newspace.getSpec().getEndpoints().size(), is(0));

        assertThat(client.services().withName(serviceName).get(), nullValue());
        assertThat(client.secrets().withName(secretName).get(), nullValue());
    }

    @Test
    public void testConsoleEndpointResourcesRemoved_Loadbalance() throws Exception {

        String serviceName = "console-1234";
        String externalServiceName = serviceName + "-external";
        String secretName = "consolesecret";

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .withNamespace("mynamespace")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()

                .withNewSpec()
                .addToEndpoints(new EndpointSpecBuilder()
                        .withName("console")
                        .withService("console")
                        .withNewCert("selfsigned", secretName, null, null)
                        .withExpose(new ExposeSpecBuilder()
                                .withType(ExposeType.loadbalancer)
                                .build())
                        .build())

                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .build();

        Service service = new ServiceBuilder()
                .editOrNewMetadata()
                .withName(serviceName)
                .addToLabels(LabelKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("https")
                .withPort(1234)
                .withNewTargetPort("https")
                .endPort()
                .addToSelector("component", "router")
                .endSpec()
                .build();
        Service extService = new ServiceBuilder()
                .editOrNewMetadata()
                .withName(externalServiceName)
                .addToLabels(LabelKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("https")
                .withPort(1234)
                .withNewTargetPort("https")
                .endPort()
                .addToSelector("component", "router")
                .endSpec()
                .build();
        client.services().create(service);
        client.services().create(extService);

        Secret secret = new SecretBuilder()
                .editOrNewMetadata()
                .withName(secretName)
                .addToLabels(LabelKeys.INFRA_UUID, "1234")
                .endMetadata()
                .build();
        client.secrets().create(secret);

        EndpointController controller = new EndpointController(client, false, false);

        AddressSpace newspace = controller.reconcileAnyState(addressSpace);

        assertThat(newspace.getSpec().getEndpoints().size(), is(0));

        assertThat(client.secrets().withName(secretName).get(), nullValue());
        assertThat(client.services().withName(serviceName).get(), nullValue());
        assertThat(client.services().withName(externalServiceName).get(), nullValue());
    }

    @Test
    public void testConsoleEndpointRemoved_UserSecretSurvives() throws Exception {
        String myCustomisedSecret = "consolesecret";

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .withNamespace("mynamespace")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()

                .withNewSpec()
                .addToEndpoints(new EndpointSpecBuilder()
                        .withName("console")
                        .withService("console")
                        .withNewCert("selfsigned", myCustomisedSecret, null, null)
                        .build())

                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .build();

        // Secret does not belong to infra (missing annotation)
        Secret secret = new SecretBuilder()
                .editOrNewMetadata()
                .withName(myCustomisedSecret)
                .endMetadata()
                .build();
        client.secrets().create(secret);

        EndpointController controller = new EndpointController(client, false, false);

        AddressSpace newspace = controller.reconcileAnyState(addressSpace);

        assertThat(newspace.getSpec().getEndpoints().size(), is(0));

        assertThat(client.secrets().withName(myCustomisedSecret).get(), notNullValue());
    }
}
