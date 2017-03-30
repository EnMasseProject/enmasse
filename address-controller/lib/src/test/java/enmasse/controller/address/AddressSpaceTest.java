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

import enmasse.controller.common.DestinationClusterGenerator;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Flavor;
import enmasse.controller.common.OpenShiftHelper;
import enmasse.controller.flavor.FlavorManager;
import io.fabric8.kubernetes.api.model.KubernetesList;
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

public class AddressSpaceTest {
    private OpenShiftHelper mockHelper;
    private AddressSpace manager;
    private FlavorManager flavorManager = new FlavorManager();
    private DestinationClusterGenerator mockGenerator;

    @Before
    public void setUp() {
        mockHelper = mock(OpenShiftHelper.class);
        mockGenerator = mock(DestinationClusterGenerator.class);

        manager = new AddressSpaceImpl(mockHelper, mockGenerator);
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        flavorMap.put("vanilla", new Flavor.Builder("vanilla", "test").build());
        flavorMap.put("shared", new Flavor.Builder("shared", "test").build());
        flavorManager.flavorsUpdated(flavorMap);
    }

    @Test
    public void testClusterIsCreated() {
        Destination queue = new Destination("myqueue", "gr0", true, false, Optional.of("vanilla"), Optional.empty());
        DestinationCluster cluster = mock(DestinationCluster.class);

        when(mockHelper.listClusters()).thenReturn(Collections.emptyList());
        when(mockGenerator.generateCluster(Collections.singleton(queue))).thenReturn(cluster);
        ArgumentCaptor<Set<Destination>> arg = ArgumentCaptor.forClass(Set.class);

        manager.setDestinations(Collections.singleton(queue));
        verify(mockGenerator).generateCluster(arg.capture());
        assertThat(arg.getValue(), hasItem(queue));
        verify(cluster).create();
    }


    @Test
    public void testNodesAreRetained() {
        Destination queue = new Destination("myqueue", "gr0", true, false, Optional.of("vanilla"), Optional.empty());
        DestinationCluster existing = new DestinationCluster(mockHelper, Sets.newSet(queue), new KubernetesList());
        when(mockHelper.listClusters()).thenReturn(Collections.singletonList(existing));

        Destination newQueue = new Destination("newqueue", "gr1", true, false, Optional.of("vanilla"), Optional.empty());
        DestinationCluster newCluster = mock(DestinationCluster.class);

        when(mockGenerator.generateCluster(Collections.singleton(newQueue))).thenReturn(newCluster);
        ArgumentCaptor<Set<Destination>> arg = ArgumentCaptor.forClass(Set.class);

        manager.setDestinations(Sets.newSet(queue, newQueue));

        verify(mockGenerator).generateCluster(arg.capture());
        assertThat(arg.getValue(), is(Sets.newSet(newQueue)));
        verify(newCluster).create();
    }

    @Test
    public void testClusterIsRemoved () {
        Destination queue = new Destination("myqueue", "gr0", true, false, Optional.of("vanilla"), Optional.empty());
        DestinationCluster existing = mock(DestinationCluster.class);
        when(existing.getDestinations()).thenReturn(Sets.newSet(queue));
        when(existing.getClusterId()).thenReturn("gr0");

        Destination newQueue = new Destination("newqueue", "gr1", true, false, Optional.of("vanilla"), Optional.empty());
        DestinationCluster newCluster = mock(DestinationCluster.class);
        when(newCluster.getDestinations()).thenReturn(Sets.newSet(newQueue));
        when(newCluster.getClusterId()).thenReturn("gr1");

        when(mockHelper.listClusters()).thenReturn(Arrays.asList(existing, newCluster));

        manager.setDestinations(Collections.singleton(newQueue));

        verify(existing, VerificationModeFactory.atLeastOnce()).getDestinations();
        verify(newCluster, VerificationModeFactory.atLeastOnce()).getDestinations();
        verify(existing).delete();
    }

    @Test
    public void testDestinationsAreGrouped() {
        Destination addr0 = new Destination("myqueue0", "group0", true, false, Optional.of("vanilla"), Optional.empty());
        Destination addr1 = new Destination("myqueue1", "group1", true, false, Optional.of("vanilla"), Optional.empty());
        Destination addr2 = new Destination("myqueue2", "group1", true, false, Optional.of("vanilla"), Optional.empty());
        Destination addr3 = new Destination("myqueue3", "group2", true, false, Optional.of("vanilla"), Optional.empty());

        DestinationCluster existing = mock(DestinationCluster.class);
        when(existing.getDestinations()).thenReturn(Sets.newSet(addr0));
        when(existing.getClusterId()).thenReturn("group0");
        DestinationCluster cluster = mock(DestinationCluster.class);

        when(mockHelper.listClusters()).thenReturn(Collections.singletonList(existing));
        ArgumentCaptor<Set<Destination>> arg = ArgumentCaptor.forClass(Set.class);
        when(mockGenerator.generateCluster(arg.capture())).thenReturn(cluster);

        manager.setDestinations(Sets.newSet(addr0, addr1, addr2, addr3));

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
