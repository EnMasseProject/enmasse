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

import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.types.Plan;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.types.standard.StandardType;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.collections.Sets;
import org.mockito.internal.verification.VerificationModeFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AddressControllerTest {
    private Kubernetes mockHelper;
    private AddressApi mockApi;
    private AddressController controller;
    private OpenShiftClient mockClient;
    private AddressClusterGenerator mockGenerator;

    @Before
    public void setUp() {
        mockHelper = mock(Kubernetes.class);
        mockGenerator = mock(AddressClusterGenerator.class);
        mockApi = mock(AddressApi.class);
        mockClient = mock(OpenShiftClient.class);
        EventLogger eventLogger = mock(EventLogger.class);

        controller = new AddressController("me", mockApi, mockHelper, mockGenerator, null, eventLogger);
    }

    private Address createAddress(String address, AddressType type) {
        return createAddress(address, type, type.getPlans().get(0));
    }

    private Address createAddress(String address, StandardType type, String planName) {
        for (Plan p : type.getPlans()) {
            if (p.getName().equals(planName)) {
                return createAddress(address, type, p);
            }
        }
        return null;
    }

    private Address createAddress(String address, AddressType type, Plan plan) {
        return new Address.Builder()
                .setName(address)
                .setAddress(address)
                .setAddressSpace("unknown")
                .setType(type)
                .setPlan(plan)
                .setUuid(UUID.randomUUID().toString())
                .build();

    }

    @Test
    public void testClusterIsCreated() throws Exception {
        Address queue = createAddress("myqueue", StandardType.QUEUE);
        KubernetesList resources = new KubernetesList();
        resources.setItems(Arrays.asList(new ConfigMap()));
        AddressCluster cluster = new AddressCluster("myqueue", resources);

        when(mockHelper.listClusters()).thenReturn(Collections.emptyList());
        when(mockGenerator.generateCluster("myqueue", Collections.singleton(queue))).thenReturn(cluster);
        ArgumentCaptor<Set<io.enmasse.address.model.Address>> arg = ArgumentCaptor.forClass(Set.class);

        controller.resourcesUpdated(Collections.singleton(queue));
        verify(mockGenerator).generateCluster(eq("myqueue"), arg.capture());
        assertThat(arg.getValue(), hasItem(queue));
        verify(mockHelper).create(resources);
    }


    @Test
    public void testNodesAreRetained() throws Exception {
        Address queue = createAddress("myqueue", StandardType.QUEUE);

        KubernetesList resources = new KubernetesList();
        resources.setItems(Arrays.asList(new ConfigMap()));
        AddressCluster existing = new AddressCluster(queue.getAddress(), resources);
        when(mockHelper.listClusters()).thenReturn(Collections.singletonList(existing));

        Address newQueue = createAddress("newqueue", StandardType.QUEUE);
        AddressCluster newCluster = new AddressCluster(newQueue.getAddress(), resources);

        when(mockGenerator.generateCluster("newqueue", Collections.singleton(newQueue))).thenReturn(newCluster);
        ArgumentCaptor<Set<io.enmasse.address.model.Address>> arg = ArgumentCaptor.forClass(Set.class);

        controller.resourcesUpdated(Sets.newSet(queue, newQueue));

        verify(mockGenerator).generateCluster(anyString(), arg.capture());
        assertThat(arg.getValue(), is(Sets.newSet(newQueue)));
        verify(mockHelper).create(resources);
    }

    @Test
    public void testClusterIsRemoved() throws Exception {
        Address queue = createAddress("myqueue", StandardType.QUEUE);

        KubernetesList resources = new KubernetesList();
        resources.setItems(Arrays.asList(new ConfigMap()));
        AddressCluster existing = new AddressCluster("myqueue", resources);

        Address newQueue = createAddress("newqueue", StandardType.QUEUE);

        AddressCluster newCluster = new AddressCluster("newqueue", resources);

        when(mockHelper.listClusters()).thenReturn(Arrays.asList(existing, newCluster));

        controller.resourcesUpdated(Collections.singleton(newQueue));

        verify(mockHelper, VerificationModeFactory.atMost(1)).delete(resources);
    }

    @Test
    public void testAddressesAreGrouped() throws Exception {
        Address addr0 = createAddress("myqueue0", StandardType.QUEUE);
        Address addr1 = createAddress("myqueue1", StandardType.QUEUE, "pooled-inmemory");
        Address addr2 = createAddress("myqueue2", StandardType.QUEUE, "pooled-inmemory");
        Address addr3 = createAddress("myqueue3", StandardType.QUEUE);

        KubernetesList resources = new KubernetesList();
        resources.setItems(Arrays.asList(new ConfigMap()));
        AddressCluster existing = new AddressCluster("myqueue0", resources);

        when(mockHelper.listClusters()).thenReturn(Collections.singletonList(existing));
        ArgumentCaptor<Set<io.enmasse.address.model.Address>> arg = ArgumentCaptor.forClass(Set.class);
        when(mockGenerator.generateCluster(anyString(), arg.capture())).thenReturn(new AddressCluster("foo", resources));

        controller.resourcesUpdated(Sets.newSet(addr0, addr1, addr2, addr3));

        Set<io.enmasse.address.model.Address> generated = arg.getAllValues().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        assertThat(generated.size(), is(3));
    }

    @Test
    public void testAddressesAreNotRecreated() throws Exception {
        Address address = createAddress("addr1", StandardType.ANYCAST);
        Address newAddress = createAddress("addr2", StandardType.ANYCAST);

        KubernetesList resources = new KubernetesList();
        when(mockGenerator.generateCluster(eq("addr1"), anySet())).thenReturn(new AddressCluster("addr1", resources));
        when(mockGenerator.generateCluster(eq("addr2"), anySet())).thenReturn(new AddressCluster("addr2", resources));

        doThrow(new KubernetesClientException("Unable to replace resource")).when(mockApi).replaceAddress(address);

        try {
            controller.resourcesUpdated(Sets.newSet(address, newAddress));

            ArgumentCaptor<Address> addressArgumentCaptor = ArgumentCaptor.forClass(Address.class);
            verify(mockApi, times(2)).replaceAddress(addressArgumentCaptor.capture());
            List<Address> replaced = addressArgumentCaptor.getAllValues();
            assertThat(replaced, hasItem(newAddress));
        } catch (KubernetesClientException e) {
            fail("Should not throw exception with multiple items");
        }
    }
}
