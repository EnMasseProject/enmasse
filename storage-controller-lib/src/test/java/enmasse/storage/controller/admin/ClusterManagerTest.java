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

import com.openshift.restclient.model.IReplicationController;
import enmasse.storage.controller.generator.StorageGenerator;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.openshift.OpenshiftClient;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ClusterManagerTest {
    private OpenshiftClient mockClient;
    private ClusterManager manager;
    private FlavorManager flavorManager = new FlavorManager();

    @Before
    public void setUp() {
        mockClient = mock(OpenshiftClient.class);
        manager = new ClusterManager(mockClient, new StorageGenerator(mockClient, flavorManager));
    }

    public void testModifiedBrokerDoesNotResetReplicaCount() {
        // Create simple queue and capture generated replication controller
        ArgumentCaptor<IReplicationController> arg = ArgumentCaptor.forClass(IReplicationController.class);

        Destination queue = new Destination("myqueue", true, false, "vanilla");
        manager.destinationsUpdated(Collections.singletonList(queue));
//        verify(mockClient, VerificationModeFactory.atLeast(1)).createResource(arg.capture());

        IReplicationController controller = arg.getValue();
//        when(mockClient.listClusters()).thenReturn(Collections.singletonList(new StorageCluster(mockClient, queue,

        // Modify replicas and update controller
        controller.setReplicas(3);
        Destination modifiedQueue = new Destination("myqueue", true, true, "vanilla");
        manager.destinationsUpdated(Collections.singletonList(modifiedQueue));

        verify(mockClient).updateResource(arg.capture());
        assertThat(arg.getValue().getReplicas(), is(3));
    }
}
