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
import enmasse.address.controller.model.Flavor;
import enmasse.address.controller.openshift.DestinationCluster;
import io.fabric8.kubernetes.api.model.KubernetesList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;

import java.util.*;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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

        manager = new AddressManagerImpl(mockHelper, mockGenerator, flavorManager);
        Map<String, Flavor> flavorMap = new LinkedHashMap<>();
        flavorMap.put("vanilla", new Flavor.Builder("vanilla", "test").build());
        flavorMap.put("shared", new Flavor.Builder("shared", "test").shared(true).build());
        flavorManager.flavorsUpdated(flavorMap);
    }

    @Test
    public void testClusterIsCreated() {
        Destination queue = new Destination("myqueue", true, false, Optional.of("vanilla"));
        DestinationCluster cluster = mock(DestinationCluster.class);

        when(mockHelper.listClusters(flavorManager)).thenReturn(Collections.emptyList());
        when(mockGenerator.generateCluster(queue)).thenReturn(cluster);
        ArgumentCaptor<Destination> arg = ArgumentCaptor.forClass(Destination.class);

        manager.destinationsUpdated(Collections.singleton(queue));
        verify(mockGenerator).generateCluster(arg.capture());
        assertThat(arg.getValue(), is(queue));
        verify(cluster).create();
    }


    @Test
    public void testNodesAreRetained() {
        Destination queue = new Destination("myqueue", true, false, Optional.of("vanilla"));
        DestinationCluster existing = new DestinationCluster(mockHelper.getClient(), queue, new KubernetesList(), false);
        when(mockHelper.listClusters(flavorManager)).thenReturn(Collections.singletonList(existing));

        Destination newQueue = new Destination("newqueue", true, false, Optional.of("vanilla"));
        DestinationCluster newCluster = mock(DestinationCluster.class);

        when(mockGenerator.generateCluster(newQueue)).thenReturn(newCluster);
        ArgumentCaptor<Destination> arg = ArgumentCaptor.forClass(Destination.class);

        manager.destinationsUpdated(new LinkedHashSet<>(Arrays.asList(queue, newQueue)));

        verify(mockGenerator).generateCluster(arg.capture());
        assertThat(arg.getValue(), is(newQueue));
        verify(newCluster).create();
    }

    @Test
    public void testClusterIsRemoved () {
        Destination queue = new Destination("myqueue", true, false, Optional.of("vanilla"));
        DestinationCluster existing = mock(DestinationCluster.class);
        when(existing.getDestination()).thenReturn(queue);

        Destination newQueue = new Destination("newqueue", true, false, Optional.of("vanilla"));
        DestinationCluster newCluster = mock(DestinationCluster.class);
        when(newCluster.getDestination()).thenReturn(newQueue);

        when(mockHelper.listClusters(flavorManager)).thenReturn(Arrays.asList(existing, newCluster));


        manager.destinationsUpdated(Collections.singleton(newQueue));

        verify(existing, VerificationModeFactory.atLeastOnce()).getDestination();
        verify(newCluster, VerificationModeFactory.atLeastOnce()).getDestination();
        verify(existing).delete();
    }

    @Test
    public void testDestinationsAreGrouped() {
        Destination addr1 = new Destination("myqueue1", true, false, Optional.of("shared"));
        Destination addr2 = new Destination("myqueue2", true, false, Optional.of("shared"));
        Destination addr3 = new Destination("myqueue3", true, false, Optional.of("vanilla"));

        DestinationCluster cluster = mock(DestinationCluster.class);

        when(mockHelper.listClusters(flavorManager)).thenReturn(Collections.emptyList());
        ArgumentCaptor<Destination> arg = ArgumentCaptor.forClass(Destination.class);
        when(mockGenerator.generateCluster(arg.capture())).thenReturn(cluster);

        Set<Destination> destinations = new LinkedHashSet<>();
        destinations.add(addr1);
        destinations.add(addr2);
        destinations.add(addr3);

        manager.destinationsUpdated(destinations);

        List<Destination> generated = arg.getAllValues();
        assertThat(generated.size(), is(2));
        Destination shared = getDestination(generated, "myqueue1");
        assertNotNull(shared);
        assertThat(shared.addresses(), hasItem("myqueue1"));
        assertThat(shared.addresses(), hasItem("myqueue2"));
    }

    private Destination getDestination(List<Destination> destinations, String address) {
        for (Destination destination : destinations) {
            if (destination.addresses().contains(address)) {
                return destination;
            }
        }
        return null;
    }
}
