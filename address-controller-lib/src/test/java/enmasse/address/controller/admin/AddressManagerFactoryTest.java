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
import enmasse.address.controller.model.TenantId;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddressManagerFactoryTest {
    @Test
    public void testGetAddressManager() {
        OpenShiftClient mockClient = mock(OpenShiftClient.class);

        when(mockClient.getConfiguration()).thenReturn(new ConfigBuilder().build());

        Map<String, String> presentLabels = new HashMap<>();
        presentLabels.put("app", "enmasse");
        presentLabels.put("tenant", "mytenant");

        Map<String, String> notPresentLabels = new HashMap<>();
        notPresentLabels.put("app", "enmasse");
        notPresentLabels.put("tenant", "notpresent");

        ClientNonNamespaceOperation presentOp = mock(ClientNonNamespaceOperation.class);
        ClientNonNamespaceOperation notPresentOp = mock(ClientNonNamespaceOperation.class);
        when(mockClient.namespaces()).thenReturn(presentOp);

        when(presentOp.withLabels(notPresentLabels)).thenReturn(notPresentOp);
        when(presentOp.withLabels(presentLabels)).thenReturn(presentOp);

        when(notPresentOp.list()).thenReturn(new NamespaceListBuilder().build());
        when(presentOp.list()).thenReturn(new NamespaceListBuilder().addToItems(new NamespaceBuilder().withMetadata(new ObjectMetaBuilder().withName("enmasse-mytenant").build()).build()).build());

        AddressManagerFactoryImpl tenantManager = new AddressManagerFactoryImpl(mockClient, tenant -> mockClient, new FlavorManager(), true, false);
        assertFalse(tenantManager.getAddressManager(TenantId.fromString("notpresent")).isPresent());
        assertTrue(tenantManager.getAddressManager(TenantId.fromString("mytenant")).isPresent());
    }

    @Test
    public void testGetOrCreateAddressManager() {
        OpenShiftClient mockClient = mock(OpenShiftClient.class);

        when(mockClient.getConfiguration()).thenReturn(new ConfigBuilder().build());
        ClientKubernetesListMixedOperation listOp = mock(ClientKubernetesListMixedOperation.class);
        when(mockClient.lists()).thenReturn(listOp);

        ClientNonNamespaceOperation nsOp = mock(ClientNonNamespaceOperation.class);
        when(mockClient.namespaces()).thenReturn(nsOp);
        when(nsOp.withLabels(any())).thenReturn(nsOp);
        when(nsOp.list()).thenReturn(new NamespaceListBuilder().build());
        when(nsOp.createNew()).thenReturn(new DoneableNamespace(new NamespaceBuilder().withMetadata(new ObjectMetaBuilder().build()).build()));

        ClientMixedOperation templateOp = mock(ClientMixedOperation.class);
        when(mockClient.templates()).thenReturn(templateOp);
        ClientTemplateResource templateResource = mock(ClientTemplateResource.class);
        when(templateOp.withName("enmasse-tenant-infra")).thenReturn(templateResource);

        ArgumentCaptor<ParameterValue> captor = ArgumentCaptor.forClass(ParameterValue.class);
        when(templateResource.process(captor.capture())).thenReturn(new KubernetesListBuilder().addNewConfigMapItem().withNewMetadata().withName("testmap").endMetadata().endConfigMapItem().build());

        ArgumentCaptor<KubernetesList> listCaptor = ArgumentCaptor.forClass(KubernetesList.class);
        when(listOp.create(listCaptor.capture())).thenReturn(null);

        AddressManagerFactoryImpl tenantManager = new AddressManagerFactoryImpl(mockClient, tenant -> mockClient, new FlavorManager(), true, false);

        AddressManager manager = tenantManager.getOrCreateAddressManager(TenantId.fromString("mytenant"));
        assertNotNull(manager);
        List<ParameterValue> values = captor.getAllValues();
        assertThat(values.size(), is(1));
        assertThat(values.get(0).getName(), is(TemplateParameter.TENANT));
        assertThat(values.get(0).getValue(), is("mytenant"));
        assertThat(listCaptor.getValue().getItems().size(), is(1));
    }
}
