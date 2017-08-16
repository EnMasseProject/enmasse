/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.controller.common;

import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.api.TestAddressSpaceApi;
import io.enmasse.address.model.*;
import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.address.model.types.standard.StandardType;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.fabric8.openshift.client.dsl.TemplateResource;
import io.fabric8.openshift.client.dsl.TemplateOperation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TemplateAddressClusterGeneratorTest {
    private OpenShiftClient mockClient;
    private AddressClusterGenerator generator;
    private TestAddressSpaceApi testAddressSpaceApi = new TestAddressSpaceApi();

    @Before
    public void setUp() {
        mockClient = mock(OpenShiftClient.class);
        testAddressSpaceApi.createAddressSpace(createAddressSpace("myinstance"));
        generator = new TemplateAddressClusterGenerator(testAddressSpaceApi,
                new KubernetesHelper("myinstance", mockClient, Optional.of(new File("src/test/resources/templates"))));
    }

    @Test
    public void testDirect() {
        Address dest = createAddress("foo_bar_FOO", StandardType.ANYCAST);
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        AddressCluster clusterList = generateCluster(dest, captor);
        List<HasMetadata> resources = clusterList.getResources().getItems();
        assertThat(resources.size(), is(0));
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testStoreAndForward() {
        Address dest = createAddress("foo.bar", StandardType.QUEUE);
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        AddressCluster clusterList = generateCluster(dest, captor);
        List<HasMetadata> resources = clusterList.getResources().getItems();
        assertThat(resources.size(), is(1));
        for (HasMetadata resource : resources) {
            Map<String, String> annotations = resource.getMetadata().getAnnotations();
            assertNotNull(annotations.get(AnnotationKeys.CLUSTER_ID));
            assertThat(annotations.get(AnnotationKeys.CLUSTER_ID), is("cluster1"));
        }
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(5));
    }

    private Address createAddress(String address, AddressType type) {
        return new Address.Builder()
                .setName(address)
                .setAddressSpace("myinstance")
                .setType(type)
                .build();
    }

    private AddressSpace createAddressSpace(String name) {
        return new AddressSpace.Builder()
                .setName(name)
                .setNamespace(name)
                .setType(new StandardAddressSpaceType())
                .appendEndpoint(new Endpoint.Builder()
                        .setName("foo")
                        .setService("messaging")
                        .setCertProvider(new SecretCertProvider("mysecret"))
                        .build())
                .build();
    }

    private AddressCluster generateCluster(Address address, ArgumentCaptor<ParameterValue> captor) {
        TemplateOperation templateOp = mock(TemplateOperation.class);
        TemplateResource templateResource = mock(TemplateResource.class);
        when(templateOp.load(any(File.class))).thenReturn(templateResource);
        when(templateResource.processLocally(captor.capture())).thenReturn(new KubernetesListBuilder().addNewConfigMapItem().withNewMetadata().withName("testmap").endMetadata().endConfigMapItem().build());
        when(mockClient.templates()).thenReturn(templateOp);

        return generator.generateCluster("cluster1", Collections.singleton(address));
    }

}
