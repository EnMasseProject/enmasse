/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import io.enmasse.address.model.AddressSpaceSpecBuilder;
import io.enmasse.address.model.AuthenticationServiceBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.model.CustomResourceDefinitions;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesList;

public class CreateControllerTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    public static void init () {
        CustomResourceDefinitions.registerAll();
    }

    @Test
    public void testAddressSpaceCreate() throws Exception {
        Kubernetes kubernetes = mock(Kubernetes.class);
        when(kubernetes.getNamespace()).thenReturn("otherspace");

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .withUid(UUID.randomUUID().toString())
                .withNamespace("mynamespace")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .withAuthenticationService(new AuthenticationServiceBuilder().withName("standard").build())
                .endSpec()

                .build();


        EventLogger eventLogger = mock(EventLogger.class);
        InfraResourceFactory mockResourceFactory = mock(InfraResourceFactory.class);
        when(mockResourceFactory.createInfraResources(eq(addressSpace), any(), any())).thenReturn(Arrays.asList(new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName("mymap")
                .endMetadata()
                .build()));

        SchemaProvider testSchema = new TestSchemaProvider();
        CreateController createController = new CreateController(kubernetes, testSchema, mockResourceFactory, eventLogger, null, "1.0", new TestAddressSpaceApi(), mock(AuthenticationServiceResolver.class));

        createController.reconcileAnyState(addressSpace);

        ArgumentCaptor<KubernetesList> resourceCaptor = ArgumentCaptor.forClass(KubernetesList.class);
        verify(kubernetes).create(resourceCaptor.capture());
        KubernetesList value = resourceCaptor.getValue();
        assertThat(value.getItems().size(), is(1));
    }

    @Test
    public void testPlanUpdateNotAccepted() throws Exception {
        Kubernetes kubernetes = mock(Kubernetes.class);
        when(kubernetes.getNamespace()).thenReturn("otherspace");
        SchemaProvider testSchema = new TestSchemaProvider();

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .withNamespace("mynamespace")
                .withUid(UUID.randomUUID().toString())
                .addToAnnotations(AnnotationKeys.APPLIED_INFRA_CONFIG,
                        mapper.writeValueAsString(testSchema.getSchema().findAddressSpaceType("type1").get().getInfraConfigs().get(0)))
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .withAuthenticationService(new AuthenticationServiceBuilder().withName("standard").build())
                .endSpec()

                .build();


        AppliedConfig.setCurrentAppliedConfig(addressSpace, AppliedConfig.create(new AddressSpaceSpecBuilder(addressSpace.getSpec())
                .withPlan("plan1")
                .build(), null));
        TestAddressSpaceApi addressSpaceApi = new TestAddressSpaceApi();
        addressSpaceApi.createAddressSpace(addressSpace);

        AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);

        Address address = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.q1")
                .withNamespace("mynamespace")
                .endMetadata()

                .withNewSpec()
                .withAddressSpace(addressSpace.getMetadata().getName())
                .withAddress("q1")
                .withType("queue")
                .withPlan("plan1")
                .endSpec()

                .build();

        Address address2 = new AddressBuilder()
                .withNewMetadata()
                .withName("myspace.q2")
                .withNamespace("mynamespace")
                .endMetadata()

                .withNewSpec()
                .withAddressSpace(addressSpace.getMetadata().getName())
                .withAddress("q2")
                .withType("queue")
                .withPlan("plan1")
                .endSpec()

                .build();

        addressApi.createAddress(address);
        addressApi.createAddress(address2);

        EventLogger eventLogger = mock(EventLogger.class);
        when(kubernetes.existsAddressSpace(eq(addressSpace))).thenReturn(true);

        CreateController createController = new CreateController(kubernetes, testSchema, null, eventLogger, null, "1.0", addressSpaceApi, mock(AuthenticationServiceResolver.class));

        addressSpace = createController.reconcileAnyState(addressSpace);

        assertThat(addressSpace.getStatus().getMessages().size(), is(1));
        assertTrue(addressSpace.getStatus().getMessages().iterator().next().contains("quota exceeded for resource broker"));
    }
}
