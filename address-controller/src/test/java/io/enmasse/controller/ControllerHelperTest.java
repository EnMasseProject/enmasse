/*
 * Copyright 2017 Red Hat Inc.
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
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.common.Plan;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.NoneAuthenticationServiceResolver;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.collections.Sets;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ControllerHelperTest {

    @Test
    public void testAddressSpaceCreate() {
        Kubernetes kubernetes = mock(Kubernetes.class);
        when(kubernetes.withNamespace(any())).thenReturn(kubernetes);
        when(kubernetes.hasService(any())).thenReturn(false);
        when(kubernetes.getNamespace()).thenReturn("otherspace");

        AddressSpaceType mockType = mock(AddressSpaceType.class);

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setType(mockType)
                .setPlan(new Plan("myplan", "myplan", "myplan", "myuuid", null))
                .build();


        ControllerHelper helper = new ControllerHelper(kubernetes, authenticationServiceType -> new NoneAuthenticationServiceResolver("localhost", 12345));
        helper.create(addressSpace);
        ArgumentCaptor<String> nameCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> namespaceCapture = ArgumentCaptor.forClass(String.class);
        verify(kubernetes).createNamespace(nameCapture.capture(), namespaceCapture.capture());
        assertThat(nameCapture.getValue(), is("myspace"));
        assertThat(namespaceCapture.getValue(), is("mynamespace"));
    }

    @Test
    public void testAddressSpaceRetain() {
        Kubernetes kubernetes = mock(Kubernetes.class);
        when(kubernetes.withNamespace(any())).thenReturn(kubernetes);
        when(kubernetes.hasService(any())).thenReturn(false);
        when(kubernetes.getNamespace()).thenReturn("otherspace");

        Map<String, String> devLabels = new HashMap<>();
        devLabels.put(LabelKeys.TYPE, "address-space");
        devLabels.put(LabelKeys.APP, "enmasse");
        devLabels.put(LabelKeys.ENVIRONMENT, "development");

        Map<String, String> prodLabels = new HashMap<>();
        prodLabels.put(LabelKeys.TYPE, "address-space");
        prodLabels.put(LabelKeys.APP, "enmasse");
        prodLabels.put(LabelKeys.ENVIRONMENT, "production");

        List<Namespace> namespaceList = Arrays.asList(new NamespaceBuilder()
                .editOrNewMetadata()
                    .withName("mynamespace")
                    .withAnnotations(Collections.singletonMap(AnnotationKeys.ADDRESS_SPACE, "myspace"))
                    .withLabels(devLabels)
                .endMetadata()
                .build(),
                new NamespaceBuilder()
                .editOrNewMetadata()
                    .withName("todelete")
                    .withAnnotations(Collections.singletonMap(AnnotationKeys.ADDRESS_SPACE, "myspace2"))
                    .withLabels(devLabels)
                .endMetadata()
                .build(),
                new NamespaceBuilder()
                .editOrNewMetadata()
                    .withName("toignore")
                    .withAnnotations(Collections.singletonMap(AnnotationKeys.ADDRESS_SPACE, "myspace"))
                    .withLabels(prodLabels)
                .endMetadata()
                .build()
                );

        when(kubernetes.listNamespaces()).thenReturn(namespaceList);

        AddressSpaceType mockType = mock(AddressSpaceType.class);

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .setType(mockType)
                .setPlan(new Plan("myplan", "myplan", "myplan", "myuuid", null))
                .build();


        ControllerHelper helper = new ControllerHelper(kubernetes, authenticationServiceType -> new NoneAuthenticationServiceResolver("localhost", 12345));
        helper.retainAddressSpaces(Sets.newSet(addressSpace));

        verify(kubernetes).deleteNamespace(eq("todelete"));
    }
}
