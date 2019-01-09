/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesList;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CreateControllerTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testAddressSpaceCreate() throws Exception {
        Kubernetes kubernetes = mock(Kubernetes.class);
        when(kubernetes.getNamespace()).thenReturn("otherspace");

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setUid(UUID.randomUUID().toString())
                .setNamespace("mynamespace")
                .setType("type1")
                .setPlan("myplan")
                .build();


        EventLogger eventLogger = mock(EventLogger.class);
        InfraResourceFactory mockResourceFactory = mock(InfraResourceFactory.class);
        when(mockResourceFactory.createInfraResources(eq(addressSpace), any())).thenReturn(Arrays.asList(new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName("mymap")
                .endMetadata()
                .build()));

        SchemaProvider testSchema = new TestSchemaProvider();
        CreateController createController = new CreateController(kubernetes, testSchema, mockResourceFactory, eventLogger, null, "1.0", new TestAddressSpaceApi());

        createController.handle(addressSpace);

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

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .putAnnotation(AnnotationKeys.APPLIED_PLAN, "plan1")
                .setUid(UUID.randomUUID().toString())
                .putAnnotation(AnnotationKeys.APPLIED_INFRA_CONFIG,
                        mapper.writeValueAsString(testSchema.getSchema().findAddressSpaceType("type1").get().getInfraConfigs().get(0)))
                .setNamespace("mynamespace")
                .setType("type1")
                .setPlan("myplan")
                .build();


        TestAddressSpaceApi addressSpaceApi = new TestAddressSpaceApi();
        addressSpaceApi.createAddressSpace(addressSpace);

        AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);

        Address address = new Address.Builder()
                .setAddressSpace(addressSpace.getName())
                .setName("myspace.q1")
                .setNamespace("mynamespace")
                .setAddress("q1")
                .setType("queue")
                .setPlan("plan1")
                .build();

        Address address2 = new Address.Builder()
                .setAddressSpace(addressSpace.getName())
                .setName("myspace.q2")
                .setNamespace("mynamespace")
                .setAddress("q2")
                .setType("queue")
                .setPlan("plan1")
                .build();

        addressApi.createAddress(address);
        addressApi.createAddress(address2);

        EventLogger eventLogger = mock(EventLogger.class);
        when(kubernetes.existsAddressSpace(eq(addressSpace))).thenReturn(true);

        CreateController createController = new CreateController(kubernetes, testSchema, null, eventLogger, null, "1.0", addressSpaceApi);

        addressSpace = createController.handle(addressSpace);

        assertThat(addressSpace.getStatus().getMessages().size(), is(1));
        assertTrue(addressSpace.getStatus().getMessages().iterator().next().contains("quota exceeded for resource broker"));
    }
}
