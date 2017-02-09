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

package enmasse.address.controller.admin;

import enmasse.address.controller.generator.DestinationClusterGenerator;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.model.Flavor;
import enmasse.address.controller.openshift.DestinationCluster;
import io.fabric8.kubernetes.api.model.KubernetesList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AddressManagerTest {
    private OpenShiftHelper mockHelper;
    private AddressManager manager;
    private FlavorManager flavorManager = new FlavorManager();
    private DestinationClusterGenerator mockGenerator;

    @Before
    public void setUp() {
        mockHelper = mock(OpenShiftHelper.class);
        mockGenerator = mock(DestinationClusterGenerator.class);

        manager = new AddressManagerImpl(mockHelper, mockGenerator);
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        flavorMap.put("vanilla", new Flavor.Builder("vanilla", "test").build());
        flavorMap.put("shared", new Flavor.Builder("shared", "test").build());
        flavorManager.flavorsUpdated(flavorMap);
    }

    @Test
    public void testClusterIsCreated() {
        Destination queue = new Destination("myqueue", "gr0", true, false, Optional.of("vanilla"));
        DestinationGroup group = new DestinationGroup("myqueue", Collections.singleton(queue));
        DestinationCluster cluster = mock(DestinationCluster.class);

        when(mockHelper.listClusters()).thenReturn(Collections.emptyList());
        when(mockGenerator.generateCluster(group)).thenReturn(cluster);
        ArgumentCaptor<DestinationGroup> arg = ArgumentCaptor.forClass(DestinationGroup.class);

        manager.destinationsUpdated(Collections.singleton(group));
        verify(mockGenerator).generateCluster(arg.capture());
        assertThat(arg.getValue(), is(group));
        verify(cluster).create();
    }


    @Test
    public void testNodesAreRetained() {
        Destination queue = new Destination("myqueue", "gr0", true, false, Optional.of("vanilla"));
        DestinationGroup group = new DestinationGroup("myqueue", Collections.singleton(queue));
        DestinationCluster existing = new DestinationCluster(mockHelper, group, new KubernetesList());
        when(mockHelper.listClusters()).thenReturn(Collections.singletonList(existing));

        Destination newQueue = new Destination("newqueue", "gr0", true, false, Optional.of("vanilla"));
        DestinationGroup newGroup = new DestinationGroup("newqueue", Collections.singleton(newQueue));
        DestinationCluster newCluster = mock(DestinationCluster.class);

        when(mockGenerator.generateCluster(newGroup)).thenReturn(newCluster);
        ArgumentCaptor<DestinationGroup> arg = ArgumentCaptor.forClass(DestinationGroup.class);

        manager.destinationsUpdated(new LinkedHashSet<>(Arrays.asList(group, newGroup)));

        verify(mockGenerator).generateCluster(arg.capture());
        assertThat(arg.getValue(), is(newGroup));
        verify(newCluster).create();
    }

    @Test
    public void testClusterIsRemoved () {
        Destination queue = new Destination("myqueue", "gr0", true, false, Optional.of("vanilla"));
        DestinationGroup group = new DestinationGroup("myqueue", Collections.singleton(queue));
        DestinationCluster existing = mock(DestinationCluster.class);
        when(existing.getDestinationGroup()).thenReturn(group);

        Destination newQueue = new Destination("newqueue", "gr0", true, false, Optional.of("vanilla"));
        DestinationGroup newGroup = new DestinationGroup("newqueue", Collections.singleton(newQueue));
        DestinationCluster newCluster = mock(DestinationCluster.class);
        when(newCluster.getDestinationGroup()).thenReturn(newGroup);

        when(mockHelper.listClusters()).thenReturn(Arrays.asList(existing, newCluster));


        manager.destinationsUpdated(Collections.singleton(newGroup));

        verify(existing, VerificationModeFactory.atLeastOnce()).getDestinationGroup();
        verify(newCluster, VerificationModeFactory.atLeastOnce()).getDestinationGroup();
        verify(existing).delete();
    }

    @Test
    public void testDestinationsAreGrouped() {
        Destination addr0 = new Destination("myqueue0", "group0", true, false, Optional.of("vanilla"));
        Destination addr1 = new Destination("myqueue1", "group1", true, false, Optional.of("vanilla"));
        Destination addr2 = new Destination("myqueue2", "group1", true, false, Optional.of("vanilla"));
        Destination addr3 = new Destination("myqueue3", "group2", true, false, Optional.of("vanilla"));


        DestinationGroup group0 = new DestinationGroup("group0", Collections.singleton(addr0));
        DestinationGroup group1 = new DestinationGroup.Builder("group1").destination(addr1).destination(addr2).build();
        DestinationGroup group2 = new DestinationGroup("group2", Collections.singleton(addr3));

        DestinationCluster existing = mock(DestinationCluster.class);
        when(existing.getDestinationGroup()).thenReturn(group0);
        DestinationCluster cluster = mock(DestinationCluster.class);

        when(mockHelper.listClusters()).thenReturn(Collections.singletonList(existing));
        ArgumentCaptor<DestinationGroup> arg = ArgumentCaptor.forClass(DestinationGroup.class);
        when(mockGenerator.generateCluster(arg.capture())).thenReturn(cluster);

        Set<DestinationGroup> destinationGroups = new LinkedHashSet<>();
        destinationGroups.add(group0);
        destinationGroups.add(group1);
        destinationGroups.add(group2);

        manager.destinationsUpdated(destinationGroups);

        List<DestinationGroup> generated = arg.getAllValues();
        assertThat(generated.size(), is(2));
        DestinationGroup shared = getDestinationGroup(generated, "group1");
        assertNotNull(shared);
        assertDestination(shared, addr1);
        assertDestination(shared, addr2);
    }

    private void assertDestination(DestinationGroup group, Destination dest) {
        Destination actual = null;
        for (Destination d : group.getDestinations()) {
            if (d.address().equals(dest.address())) {
                actual = d;
                break;
            }
        }
        assertNotNull(actual);
        assertTrue(actual.equals(dest));
    }

    private DestinationGroup getDestinationGroup(List<DestinationGroup> destinationGroups, String groupId) {
        for (DestinationGroup destinationGroup : destinationGroups) {
            if (destinationGroup.getGroupId().equals(groupId)) {
                return destinationGroup;
            }
        }
        return null;
    }
}
