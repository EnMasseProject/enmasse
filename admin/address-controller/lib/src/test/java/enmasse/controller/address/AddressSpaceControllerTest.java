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

package enmasse.controller.address;

import enmasse.controller.address.api.DestinationApi;
import enmasse.controller.common.DestinationClusterGenerator;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Flavor;
import enmasse.controller.flavor.FlavorManager;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.KubernetesList;
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

public class AddressSpaceControllerTest {
    private Kubernetes mockHelper;
    private DestinationApi mockApi;
    private AddressSpaceController controller;
    private OpenShiftClient mockClient;
    private FlavorManager flavorManager = new FlavorManager();
    private DestinationClusterGenerator mockGenerator;

    @Before
    public void setUp() {
        mockHelper = mock(Kubernetes.class);
        mockGenerator = mock(DestinationClusterGenerator.class);
        mockApi = mock(DestinationApi.class);
        mockClient = mock(OpenShiftClient.class);

        when(mockHelper.getInstanceId()).thenReturn(InstanceId.withId("myinstance"));
        controller = new AddressSpaceController(mockApi, mockHelper, mockGenerator);
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        flavorMap.put("vanilla", new Flavor.Builder("vanilla", "test").build());
        flavorMap.put("shared", new Flavor.Builder("shared", "test").build());
        flavorManager.flavorsUpdated(flavorMap);
    }

    @Test
    public void testClusterIsCreated() throws Exception {
        Destination queue = new Destination.Builder("myqueue", "gr0")
                .storeAndForward(true)
                .multicast(false)
                .flavor(Optional.of("vanilla"))
                .build();
        KubernetesList resources = new KubernetesList();
        resources.setItems(Arrays.asList(new ConfigMap()));
        DestinationCluster cluster = new DestinationCluster("gr0", resources);

        when(mockHelper.listClusters()).thenReturn(Collections.emptyList());
        when(mockGenerator.generateCluster(Collections.singleton(queue))).thenReturn(cluster);
        ArgumentCaptor<Set<Destination>> arg = ArgumentCaptor.forClass(Set.class);

        controller.resourcesUpdated(Collections.singleton(queue));
        verify(mockGenerator).generateCluster(arg.capture());
        assertThat(arg.getValue(), hasItem(queue));
        verify(mockHelper).create(resources);
    }


    @Test
    public void testNodesAreRetained() throws Exception {
        Destination queue = new Destination.Builder("myqueue", "gr0")
                .storeAndForward(true)
                .flavor(Optional.of("vanilla"))
                .build();

        KubernetesList resources = new KubernetesList();
        resources.setItems(Arrays.asList(new ConfigMap()));
        DestinationCluster existing = new DestinationCluster("gr0", resources);
        when(mockHelper.listClusters()).thenReturn(Collections.singletonList(existing));

        Destination newQueue = new Destination.Builder("newqueue", "gr1")
                .storeAndForward(true)
                .flavor(Optional.of("vanilla"))
                .build();
        DestinationCluster newCluster = new DestinationCluster("gr1", resources);

        when(mockGenerator.generateCluster(Collections.singleton(newQueue))).thenReturn(newCluster);
        ArgumentCaptor<Set<Destination>> arg = ArgumentCaptor.forClass(Set.class);

        controller.resourcesUpdated(Sets.newSet(queue, newQueue));

        verify(mockGenerator).generateCluster(arg.capture());
        assertThat(arg.getValue(), is(Sets.newSet(newQueue)));
        verify(mockHelper).create(resources);
    }

    @Test
    public void testClusterIsRemoved () throws Exception {
        Destination queue = new Destination.Builder("myqueue", "gr0")
                .storeAndForward(true)
                .flavor(Optional.of("vanilla"))
                .build();

        KubernetesList resources = new KubernetesList();
        resources.setItems(Arrays.asList(new ConfigMap()));
        DestinationCluster existing = new DestinationCluster("gr0", resources);

        Destination newQueue = new Destination.Builder("newqueue", "gr1")
                .storeAndForward(true)
                .flavor(Optional.of("vanilla"))
                .build();

        DestinationCluster newCluster = new DestinationCluster("gr1", resources);

        when(mockHelper.listClusters()).thenReturn(Arrays.asList(existing, newCluster));

        controller.resourcesUpdated(Collections.singleton(newQueue));

        verify(mockHelper, VerificationModeFactory.atMost(1)).delete(resources);
    }

    @Test
    public void testDestinationsAreGrouped() throws Exception {
        Destination addr0 = new Destination("myqueue0", "group0", true, false, Optional.of("vanilla"), Optional.empty(), new Destination.Status(false));
        Destination addr1 = new Destination("myqueue1", "group1", true, false, Optional.of("vanilla"), Optional.empty(), new Destination.Status(false));
        Destination addr2 = new Destination("myqueue2", "group1", true, false, Optional.of("vanilla"), Optional.empty(), new Destination.Status(false));
        Destination addr3 = new Destination("myqueue3", "group2", true, false, Optional.of("vanilla"), Optional.empty(), new Destination.Status(false));

        KubernetesList resources = new KubernetesList();
        resources.setItems(Arrays.asList(new ConfigMap()));
        DestinationCluster existing = new DestinationCluster("group0", resources);

        when(mockHelper.listClusters()).thenReturn(Collections.singletonList(existing));
        ArgumentCaptor<Set<Destination>> arg = ArgumentCaptor.forClass(Set.class);
        when(mockGenerator.generateCluster(arg.capture())).thenReturn(new DestinationCluster("foo", resources));

        controller.resourcesUpdated(Sets.newSet(addr0, addr1, addr2, addr3));

        Set<Destination> generated = arg.getAllValues().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        assertThat(generated.size(), is(3));
        Set<Destination> shared = filterDestinationsByGroup(generated, "group1");
        assertThat(shared.size(), is(2));
        assertNotNull(shared);
        assertThat(shared, hasItem(addr1));
        assertThat(shared, hasItem(addr2));
    }

    private Set<Destination> filterDestinationsByGroup(Set<Destination> destinations, String groupId) {
        return destinations.stream().filter(d -> d.group().equals(groupId)).collect(Collectors.toSet());
    }
}
