package enmasse.address.controller.admin;

import enmasse.address.controller.model.AddressType;
import enmasse.address.controller.model.Flavor;
import enmasse.address.controller.model.LabelKeys;
import enmasse.address.controller.openshift.DestinationCluster;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenShiftHelperTest {

    @Test
    public void testListClusters() {
        DeploymentConfig config =
                new DeploymentConfigBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                            .withName("testdc")
                            .addToLabels(LabelKeys.FLAVOR, "vanilla")
                            .addToAnnotations(LabelKeys.ADDRESS_LIST, "[\"myqueue\"]")
                            .addToLabels(LabelKeys.STORE_AND_FORWARD, "true")
                            .addToLabels(LabelKeys.MULTICAST, "false")
                            .build())
                        .build();

        ConfigMap map =
                new ConfigMapBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                                .withName("testmap")
                                .addToAnnotations(LabelKeys.ADDRESS_LIST, "[\"anycast\"]")
                                .addToLabels(LabelKeys.STORE_AND_FORWARD, "false")
                                .addToLabels(LabelKeys.MULTICAST, "false")
                                .build())
                        .build();

        ClientMixedOperation dcOp = mock(ClientMixedOperation.class);
        ClientMixedOperation pvcOp = mock(ClientMixedOperation.class);
        ClientMixedOperation mapOp = mock(ClientMixedOperation.class);
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
        when(rcOp.withLabel(anyString(), anyString())).thenReturn(rcOp);
        when(pvcOp.list()).thenReturn(new PersistentVolumeClaimListBuilder().build());
        when(dcOp.list()).thenReturn(new DeploymentConfigListBuilder().addToItems(config).build());
        when(mapOp.list()).thenReturn(new ConfigMapListBuilder().addToItems(map).build());
        when(rcOp.list()).thenReturn(new ReplicationControllerListBuilder().build());

        FlavorManager flavorManager = new FlavorManager();
        flavorManager.flavorsUpdated(Collections.singletonMap("vanilla", new Flavor.Builder("vanilla", "test").build()));
        List<DestinationCluster> clusters = helper.listClusters(flavorManager);
        assertThat(clusters.size(), is(2));

        DestinationCluster cluster = clusters.get(0);
        assertThat(cluster.getDestination().addresses(), hasItem("anycast"));
        assertFalse(cluster.getDestination().flavor().isPresent());
        assertFalse(cluster.getDestination().storeAndForward());
        assertFalse(cluster.getDestination().multicast());

        cluster = clusters.get(1);
        assertThat(cluster.getDestination().addresses(), hasItem("myqueue"));
        assertThat(cluster.getDestination().flavor().get(), is("vanilla"));
        assertTrue(cluster.getDestination().storeAndForward());
        assertFalse(cluster.getDestination().multicast());
    }
}
