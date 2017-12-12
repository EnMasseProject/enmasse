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

package io.enmasse.controller.standard;

import io.enmasse.config.AnnotationKeys;
import io.enmasse.address.model.*;
import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.address.model.types.standard.StandardType;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.client.ParameterValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TemplateAddressClusterGeneratorTest {
    private Kubernetes kubernetes;
    private AddressClusterGenerator generator;

    @Before
    public void setUp() {
        kubernetes = mock(Kubernetes.class);
        Map<String, String> env = new HashMap<>();
        generator = new TemplateAddressClusterGenerator(kubernetes, new TemplateOptions(env));
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
            assertThat(annotations.get(AnnotationKeys.CLUSTER_ID), is(dest.getName()));
        }
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(10));
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
        when(kubernetes.processTemplate(anyString(), captor.capture())).thenReturn(new KubernetesListBuilder().addNewConfigMapItem().withNewMetadata().withName("testmap").endMetadata().endConfigMapItem().build());

        return generator.generateCluster(address.getName(), Collections.singleton(address));
    }

}
