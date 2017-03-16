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
package enmasse.address.controller.admin;

import enmasse.address.controller.generator.TemplateParameter;
import enmasse.address.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.dsl.ClientKubernetesListMixedOperation;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.fabric8.kubernetes.client.dsl.ClientNonNamespaceOperation;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.fabric8.openshift.client.dsl.ClientTemplateResource;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddressManagerFactoryTest {
    @Test
    public void testGetAddressManager() {
        OpenShift mockClient = mock(OpenShift.class);

        Map<String, String> presentLabels = new HashMap<>();
        presentLabels.put("app", "enmasse");
        presentLabels.put("instance", "myinstance");

        Map<String, String> notPresentLabels = new HashMap<>();
        notPresentLabels.put("app", "enmasse");
        notPresentLabels.put("instance", "notpresent");

        when(mockClient.hasNamespace(notPresentLabels)).thenReturn(false);
        when(mockClient.hasNamespace(presentLabels)).thenReturn(true);

        AddressManagerFactoryImpl instanceManager = new AddressManagerFactoryImpl(mockClient, new FlavorManager(), true, false);
        assertFalse(instanceManager.getAddressManager(InstanceId.withId("notpresent")).isPresent());
        assertTrue(instanceManager.getAddressManager(InstanceId.withId("myinstance")).isPresent());
    }

    @Test
    public void testGetOrCreateAddressManager() {
        OpenShift mockClient = mock(OpenShift.class);

        when(mockClient.mutateClient(any())).thenReturn(mockClient);
        KubernetesList list = new KubernetesListBuilder().addToItems(new ConfigMapBuilder().withMetadata(new ObjectMetaBuilder().withName("foo").build()).build()).build();
        when(mockClient.processTemplate(anyString(), any())).thenReturn(list);
        ArgumentCaptor<KubernetesList> listCaptor = ArgumentCaptor.forClass(KubernetesList.class);

        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);


        AddressManagerFactoryImpl instanceManager = new AddressManagerFactoryImpl(mockClient, new FlavorManager(), true, false);

        AddressManager manager = instanceManager.getOrCreateAddressManager(InstanceId.withId("myinstance"));
        assertNotNull(manager);
        verify(mockClient).create(listCaptor.capture());
        verify(mockClient).processTemplate(anyString(), captor.capture());

        List<ParameterValue> values = captor.getAllValues();
        assertThat(values.size(), is(1));
        assertThat(values.get(0).getName(), is(TemplateParameter.INSTANCE));
        assertThat(values.get(0).getValue(), is("myinstance"));
        assertThat(listCaptor.getValue().getItems().size(), is(1));
    }
}
