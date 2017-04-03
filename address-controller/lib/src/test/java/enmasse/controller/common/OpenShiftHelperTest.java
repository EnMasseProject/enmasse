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

package enmasse.controller.common;

import enmasse.controller.model.Destination;
import enmasse.controller.model.Flavor;
import enmasse.controller.model.InstanceId;
import enmasse.controller.address.DestinationCluster;
import enmasse.config.AddressEncoder;
import enmasse.config.LabelKeys;
import enmasse.controller.flavor.FlavorManager;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.extensions.DeploymentListBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ExtensionsAPIGroupDSL;
import io.fabric8.openshift.api.model.DeploymentConfigListBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenShiftHelperTest {

    @Test
    public void testListClusters() {
        Deployment config =
                new DeploymentBuilder()
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

        MixedOperation dcOp = mock(MixedOperation.class);
        MixedOperation dOp = mock(MixedOperation.class);
        MixedOperation pvcOp = mock(MixedOperation.class);
        MixedOperation mapOp = mock(MixedOperation.class);

        Resource mapQueueResource = mock(Resource.class);
        Resource mapDirectResource = mock(Resource.class);

        MixedOperation rcOp = mock(MixedOperation.class);
        ExtensionsAPIGroupDSL extensions = mock(ExtensionsAPIGroupDSL.class);

        OpenShiftClient mockClient = mock(OpenShiftClient.class);
        OpenShiftHelper helper = new OpenShiftHelper(InstanceId.withId("myinstance"), mockClient, new File("src/test/resources/templates"));
        when(mockClient.deploymentConfigs()).thenReturn(dcOp);
        when(mockClient.extensions()).thenReturn(extensions);
        when(extensions.deployments()).thenReturn(dOp);
        when(mockClient.persistentVolumeClaims()).thenReturn(pvcOp);
        when(mockClient.configMaps()).thenReturn(mapOp);
        when(mockClient.replicationControllers()).thenReturn(rcOp);

        when(dcOp.inNamespace(anyString())).thenReturn(dcOp);
        when(dOp.inNamespace(anyString())).thenReturn(dOp);
        when(pvcOp.inNamespace(anyString())).thenReturn(pvcOp);
        when(mapOp.inNamespace(anyString())).thenReturn(mapOp);
        when(rcOp.inNamespace(anyString())).thenReturn(rcOp);

        when(pvcOp.withLabel(anyString(), anyString())).thenReturn(pvcOp);
        when(dcOp.withLabel(anyString(), anyString())).thenReturn(dcOp);
        when(dOp.withLabel(anyString(), anyString())).thenReturn(dOp);
        when(mapOp.withLabel(anyString(), anyString())).thenReturn(mapOp);

        when(mapOp.withName("mygroup-config")).thenReturn(mapQueueResource);
        when(mapOp.withName("group2-config")).thenReturn(mapDirectResource);


        when(mapQueueResource.get()).thenReturn(map);
        when(mapDirectResource.get()).thenReturn(directMap);

        when(rcOp.withLabel(anyString(), anyString())).thenReturn(rcOp);
        when(pvcOp.list()).thenReturn(new PersistentVolumeClaimListBuilder().build());
        when(dcOp.list()).thenReturn(new DeploymentConfigListBuilder().build());
        when(dOp.list()).thenReturn(new DeploymentListBuilder().addToItems(config).build());
        when(mapOp.list()).thenReturn(new ConfigMapListBuilder().addToItems(direct).build());
        when(rcOp.list()).thenReturn(new ReplicationControllerListBuilder().build());

        FlavorManager flavorManager = new FlavorManager();
        flavorManager.flavorsUpdated(Collections.singletonMap("vanilla", new Flavor.Builder("vanilla", "test").build()));
        List<DestinationCluster> clusters = helper.listClusters();
        assertThat(clusters.size(), is(2));

        DestinationCluster cluster = clusters.get(1);
        Set<Destination> group = cluster.getDestinations();
        assertThat(group.size(), is(1));
        assertDestination(group, "anycast", false, false, Optional.empty());

        cluster = clusters.get(0);
        group = cluster.getDestinations();
        assertThat(group.size(), is(2));
        assertDestination(group, "myqueue", true, false, Optional.of("vanilla"));
        assertDestination(group, "myqueue2", true, false, Optional.of("vanilla"));
    }

    @Test
    public void testProcessTemplate() {
        OpenShiftHelper helper = new OpenShiftHelper(InstanceId.withId("myinstance"), new DefaultOpenShiftClient(), new File("src/test/resources/templates"));
        KubernetesList list = helper.processTemplate("test", new ParameterValue("MYPARAM", "value"), new ParameterValue("SECONDPARAM", ""));
        assertThat(list.getItems().size(), is(1));
    }

    @Test
    public void testCreateAddressConfig() {
        OpenShiftClient mockClient = mock(OpenShiftClient.class);
        OpenShiftHelper helper = new OpenShiftHelper(InstanceId.withId("myinstance"), mockClient, new File("src/test/resources/templates"));
        Set<Destination> group = Sets.newSet(new Destination("queue1", "group1", true, false, Optional.of("vanilla"), Optional.empty()),
                new Destination("queue2", "group1", true, false, Optional.of("vanilla"), Optional.empty()));

        ConfigMap addressConfig = helper.createAddressConfig(group);

        assertTrue(addressConfig.getMetadata().getLabels().containsKey(LabelKeys.GROUP_ID));
        assertThat(addressConfig.getMetadata().getLabels().get(LabelKeys.GROUP_ID), is("group1"));
        assertThat(addressConfig.getData().size(), is(2));
        assertTrue(addressConfig.getData().containsKey("queue1"));
        assertTrue(addressConfig.getData().containsKey("queue2"));
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
