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

package enmasse.storage.controller.openshift;

import com.openshift.internal.restclient.model.ReplicationController;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import enmasse.storage.controller.model.AddressType;
import enmasse.storage.controller.model.LabelKeys;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenshiftClientTest {
    @Test
    public void testListClusters() {
        IClient client = mock(IClient.class);
        OpenshiftClient osClient = new OpenshiftClient(client, "myspace");
        ReplicationController controller = new ReplicationController(new ModelNode(), client, Collections.emptyMap());
        controller.addLabel(LabelKeys.FLAVOR, "vanilla");
        controller.addLabel(LabelKeys.ADDRESS_TYPE, AddressType.QUEUE.value());
        controller.addLabel(LabelKeys.ADDRESS, "foo");
        when(client.list(ResourceKind.REPLICATION_CONTROLLER, "myspace")).thenReturn(Collections.singletonList(controller));

        List<StorageCluster> clusters = osClient.listClusters();
        verify(client).list(ResourceKind.REPLICATION_CONTROLLER, "myspace");
        assertThat(clusters.size(), is(1));
        StorageCluster cluster = clusters.get(0);
        assertThat(cluster.getDestination().address(), is("foo"));
        assertThat(cluster.getDestination().flavor(), is("vanilla"));
        assertTrue(cluster.getDestination().storeAndForward());
        assertFalse(cluster.getDestination().multicast());
    }
}
