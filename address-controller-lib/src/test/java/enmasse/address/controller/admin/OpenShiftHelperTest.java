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

import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.model.Flavor;
import enmasse.address.controller.openshift.DestinationCluster;
import enmasse.config.AddressEncoder;
import enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenShiftHelperTest {

    @Test
    public void testListClusters() {
        DeploymentConfig config =
                new DeploymentConfigBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                            .withName("testdc")
                            .addToLabels(LabelKeys.GROUP_ID, "mygroup")
                            .addToLabels(LabelKeys.ADDRESS_CONFIG, "mygroup-config")
                            .build())
                        .build();

        AddressEncoder encoder = new AddressEncoder();
        encoder.encode(true, false, Optional.of("vanilla"));
        ConfigMap map =
                new ConfigMapBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                                .withName("mygroup-config")
                                .build())
                        .addToData("myqueue", encoder.toJson())
                        .addToData("myqueue2", encoder.toJson())
                        .build();

        ConfigMap direct =
                new ConfigMapBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                                .withName("anycast")
                                .addToLabels(LabelKeys.GROUP_ID, "group2")
                                .addToLabels(LabelKeys.ADDRESS_CONFIG, "group2-config")
                                .build())
                        .build();

        encoder = new AddressEncoder();
        encoder.encode(false, false, Optional.empty());
        ConfigMap directMap =
                new ConfigMapBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                                .withName("group2-config")
                                .build())
                        .addToData("anycast", encoder.toJson())
                        .build();

        ClientMixedOperation dcOp = mock(ClientMixedOperation.class);
        ClientMixedOperation pvcOp = mock(ClientMixedOperation.class);
        ClientMixedOperation mapOp = mock(ClientMixedOperation.class);

        ClientMixedOperation mapOpQueue = mock(ClientMixedOperation.class);
        ClientMixedOperation mapOpDirect = mock(ClientMixedOperation.class);

        ClientMixedOperation rcOp = mock(ClientMixedOperation.class);

        OpenShiftClient mockClient = mock(OpenShiftClient.class);
        OpenShiftHelper helper = new OpenShiftHelper(mockClient);
        when(mockClient.deploymentConfigs()).thenReturn(dcOp);
        when(mockClient.persistentVolumeClaims()).thenReturn(pvcOp);
        when(mockClient.configMaps()).thenReturn(mapOp);
        when(mockClient.replicationControllers()).thenReturn(rcOp);

        when(pvcOp.withLabel(anyString(), anyString())).thenReturn(pvcOp);
        when(dcOp.withLabel(anyString(), anyString())).thenReturn(dcOp);
        when(mapOp.withLabel(anyString(), anyString())).thenReturn(mapOp);

        when(mapOp.withName("mygroup-config")).thenReturn(mapOpQueue);
        when(mapOp.withName("group2-config")).thenReturn(mapOpDirect);

        when(mapOpQueue.get()).thenReturn(map);
        when(mapOpDirect.get()).thenReturn(directMap);

        when(rcOp.withLabel(anyString(), anyString())).thenReturn(rcOp);
        when(pvcOp.list()).thenReturn(new PersistentVolumeClaimListBuilder().build());
        when(dcOp.list()).thenReturn(new DeploymentConfigListBuilder().addToItems(config).build());
        when(mapOp.list()).thenReturn(new ConfigMapListBuilder().addToItems(direct).build());
        when(mapOpDirect.list()).thenReturn(new ConfigMapListBuilder().addToItems(direct).build());
        when(rcOp.list()).thenReturn(new ReplicationControllerListBuilder().build());

        FlavorManager flavorManager = new FlavorManager();
        flavorManager.flavorsUpdated(Collections.singletonMap("vanilla", new Flavor.Builder("vanilla", "test").build()));
        List<DestinationCluster> clusters = helper.listClusters();
        assertThat(clusters.size(), is(2));

        DestinationCluster cluster = clusters.get(1);
        DestinationGroup group = cluster.getDestinationGroup();
        assertThat(group.getDestinations().size(), is(1));
        assertDestination(group.getDestinations(), "anycast", false, false, Optional.empty());

        cluster = clusters.get(0);
        group = cluster.getDestinationGroup();
        assertThat(group.getDestinations().size(), is(2));
        assertDestination(group.getDestinations(), "myqueue", true, false, Optional.of("vanilla"));
        assertDestination(group.getDestinations(), "myqueue2", true, false, Optional.of("vanilla"));
    }

    @Test
    public void testUpdateDestinationGroup() {
        OpenShiftClient mockClient = mock(OpenShiftClient.class);
        OpenShiftHelper helper = new OpenShiftHelper(mockClient);

        DoneableConfigMap map = new DoneableConfigMap(new ConfigMap("v1", Collections.<String, String>emptyMap(), "ConfigMap", new ObjectMetaBuilder().withName("address-config-group1").build()));
        ClientMixedOperation mapOp = mock(ClientMixedOperation.class);
        when(mockClient.configMaps()).thenReturn(mapOp);
        when(mapOp.withName("address-config-group1")).thenReturn(mapOp);
        when(mapOp.createOrReplaceWithNew()).thenReturn(map);

        DestinationGroup group = new DestinationGroup("group1", Sets.newSet(new Destination("queue1", "group1", true, false, Optional.of("vanilla")),
                new Destination("queue2", "group1", true, false, Optional.of("vanilla"))));
        helper.updateDestinations(group);

        ConfigMap edited = map.done();
        assertTrue(edited.getMetadata().getLabels().containsKey(LabelKeys.GROUP_ID));
        assertThat(edited.getMetadata().getLabels().get(LabelKeys.GROUP_ID), is("group1"));
        assertThat(edited.getData().size(), is(2));
        assertTrue(edited.getData().containsKey("queue1"));
        assertTrue(edited.getData().containsKey("queue2"));
    }

    private void assertDestination(Set<Destination> destinations, String address, boolean storeAndForward, boolean multicast, Optional<String> flavor) {
        Destination actual = null;
        for (Destination destination : destinations) {
            if (destination.address().equals(address)) {
                actual = destination;
                break;
            }
        }
        assertNotNull(actual);
        assertThat(actual.address(), is(address));
        assertThat(actual.storeAndForward(), is(storeAndForward));
        assertThat(actual.multicast(), is(multicast));
        assertThat(actual.flavor(), is(flavor));
    }
}
