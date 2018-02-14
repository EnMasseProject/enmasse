/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import io.enmasse.config.AnnotationKeys;
import io.enmasse.address.model.*;
import io.enmasse.k8s.api.TestSchemaApi;
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
        TestSchemaApi testSchemaApi = new TestSchemaApi();
        AddressResolver resolver = new AddressResolver(testSchemaApi.getSchema(), testSchemaApi.getSchema().findAddressSpaceType("type1").get());
        generator = new TemplateAddressClusterGenerator(kubernetes, resolver, new TemplateOptions(env));
    }

    @Test
    public void testDirect() {
        Address dest = createAddress("foo_bar_FOO", "anycast");
        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        AddressCluster clusterList = generateCluster(dest, captor);
        List<HasMetadata> resources = clusterList.getResources().getItems();
        assertThat(resources.size(), is(0));
        List<ParameterValue> parameters = captor.getAllValues();
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testStoreAndForward() {
        Address dest = createAddress("foo.bar", "queue");
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

    private Address createAddress(String address, String type) {
        return new Address.Builder()
                .setName(address)
                .setAddressSpace("myinstance")
                .setType(type)
                .setPlan("plan1")
                .build();
    }

    private AddressSpace createAddressSpace(String name) {
        return new AddressSpace.Builder()
                .setName(name)
                .setNamespace(name)
                .setType("standard")
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
