/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.common;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

public class KubernetesHelperTest {

    /*
    @Test
    public void testListClusters() {

        MixedOperation dOp = mock(MixedOperation.class);
        MixedOperation pvcOp = mock(MixedOperation.class);
        MixedOperation mapOp = mock(MixedOperation.class);

        MixedOperation rcOp = mock(MixedOperation.class);
        ExtensionsAPIGroupDSL extensions = mock(ExtensionsAPIGroupDSL.class);

        OpenShiftClient mockClient = mock(OpenShiftClient.class);
        KubernetesHelper helper = new KubernetesHelper(AddressSpaceId.withId("myinstance"), mockClient, new File("src/test/resources/templates"));

        when(mockClient.extensions()).thenReturn(extensions);
        when(extensions.deployments()).thenReturn(dOp);
        when(mockClient.persistentVolumeClaims()).thenReturn(pvcOp);
        when(mockClient.configMaps()).thenReturn(mapOp);
        when(mockClient.replicationControllers()).thenReturn(rcOp);

        when(dOp.inNamespace(anyString())).thenReturn(dOp);
        when(pvcOp.inNamespace(anyString())).thenReturn(pvcOp);
        when(mapOp.inNamespace(anyString())).thenReturn(mapOp);
        when(rcOp.inNamespace(anyString())).thenReturn(rcOp);

        when(pvcOp.withLabel(anyString(), anyString())).thenReturn(pvcOp);
        when(dOp.withLabel(anyString(), anyString())).thenReturn(dOp);
        when(mapOp.withLabels(any())).thenReturn(mapOp);

        Deployment config =
                new DeploymentBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                            .withName("testdc")
                            .addToLabels(LabelKeys.CLUSTER_ID, "mygroup")
                            .addToLabels(LabelKeys.ADDRESS_CONFIG, "address-config-mygroup")
                            .build())
                        .build();



        ConfigMap map1 = helper.createAddressConfig(new Destination("myqueue", "mygroup", true, false, Optional.of("vanilla"), Optional.empty(), status));
        ConfigMap map2 = helper.createAddressConfig(new Destination("myqueue2", "mygroup", true, false, Optional.of("vanilla"), Optional.empty(), status));

        when(rcOp.withLabel(anyString(), anyString())).thenReturn(rcOp);
        when(pvcOp.list()).thenReturn(new PersistentVolumeClaimListBuilder().build());
        when(dOp.list()).thenReturn(new DeploymentListBuilder().addToItems(config).build());
        when(mapOp.list()).thenReturn(new ConfigMapListBuilder().addToItems(map1, map2).build());
        when(rcOp.list()).thenReturn(new ReplicationControllerListBuilder().build());

        PlanManager flavorManager = new PlanManager();
        flavorManager.flavorsUpdated(Collections.singletonMap("vanilla", new Flavor.Builder("vanilla", "test").build()));
        List<AddressCluster> clusters = helper.listClusters();
        assertThat(clusters.size(), is(1));

        AddressCluster cluster = clusters.get(0);
        Set<Destination> group = cluster.getDestinations();
        assertThat(group.size(), is(2));
        assertDestination(group, "myqueue", true, false, Optional.of("vanilla"));
        assertDestination(group, "myqueue2", true, false, Optional.of("vanilla"));
    }

    @Test
    public void testProcessTemplate() {
        KubernetesHelper helper = new KubernetesHelper(AddressSpaceId.withId("myinstance"), new DefaultOpenShiftClient(), new File("src/test/resources/templates"));
        KubernetesList list = helper.processTemplate("test", new ParameterValue("MYPARAM", "value"), new ParameterValue("SECONDPARAM", ""));
        assertThat(list.getItems().size(), is(1));
    }

    @Test
    public void testCreateAddressConfig() {
        OpenShiftClient mockClient = mock(OpenShiftClient.class);
        KubernetesHelper helper = new KubernetesHelper(AddressSpaceId.withId("myinstance"), mockClient, new File("src/test/resources/templates"));
        Destination destination = new Destination("queue1", "group1", true, false, Optional.of("vanilla"), Optional.empty(), status);

        ConfigMap addressConfig = helper.createAddressConfig(destination);

        assertTrue(addressConfig.getMetadata().getLabels().containsKey(LabelKeys.CLUSTER_ID));
        assertThat(addressConfig.getMetadata().getLabels().get(LabelKeys.CLUSTER_ID), is("group1"));
        assertThat(addressConfig.getData().size(), is(5));
        assertThat(addressConfig.getData().get(AddressConfigKeys.ADDRESS), is("queue1"));
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
    */
}
