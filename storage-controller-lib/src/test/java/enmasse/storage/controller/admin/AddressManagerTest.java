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

package enmasse.storage.controller.admin;

import enmasse.storage.controller.generator.DestinationClusterGenerator;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.Flavor;
import enmasse.storage.controller.openshift.DestinationCluster;
import io.fabric8.kubernetes.api.model.KubernetesList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.hamcrest.CoreMatchers.is;
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

        manager = new AddressManagerImpl(mockHelper, mockGenerator);
        flavorManager.flavorsUpdated(Collections.singletonMap("vanilla", new Flavor.Builder().templateName("test").build()));
    }

    @Test
    public void testClusterIsCreated() {
        Destination queue = new Destination("myqueue", true, false, "vanilla");
        DestinationCluster cluster = mock(DestinationCluster.class);

        when(mockHelper.listClusters()).thenReturn(Collections.emptyList());
        when(mockGenerator.generateCluster(queue)).thenReturn(cluster);
        ArgumentCaptor<Destination> arg = ArgumentCaptor.forClass(Destination.class);

        manager.destinationsUpdated(Collections.singleton(queue));
        verify(mockGenerator).generateCluster(arg.capture());
        assertThat(arg.getValue(), is(queue));
        verify(cluster).create();
    }


    @Test
    public void testNodesAreRetained() {
        Destination queue = new Destination("myqueue", true, false, "vanilla");
        DestinationCluster existing = new DestinationCluster(mockHelper.getClient(), queue, new KubernetesList());
        when(mockHelper.listClusters()).thenReturn(Collections.singletonList(existing));

        Destination newQueue = new Destination("newqueue", true, false, "vanilla");
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
        Destination queue = new Destination("myqueue", true, false, "vanilla");
        DestinationCluster existing = mock(DestinationCluster.class);
        when(existing.getDestination()).thenReturn(queue);

        Destination newQueue = new Destination("newqueue", true, false, "vanilla");
        DestinationCluster newCluster = mock(DestinationCluster.class);
        when(newCluster.getDestination()).thenReturn(newQueue);

        when(mockHelper.listClusters()).thenReturn(Arrays.asList(existing, newCluster));


        manager.destinationsUpdated(Collections.singleton(newQueue));

        verify(existing, VerificationModeFactory.atLeastOnce()).getDestination();
        verify(newCluster, VerificationModeFactory.atLeastOnce()).getDestination();
        verify(existing).delete();
    }
}
