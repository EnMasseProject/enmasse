package enmasse.storage.controller.admin;

import enmasse.storage.controller.model.AddressType;
import enmasse.storage.controller.model.LabelKeys;
import enmasse.storage.controller.openshift.DestinationCluster;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Test;

import java.util.List;

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
                            .addToLabels(LabelKeys.ADDRESS, "myqueue")
                            .addToLabels(LabelKeys.ADDRESS_TYPE, AddressType.QUEUE.value())
                            .build())
                        .build();

        ConfigMap map =
                new ConfigMapBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                                .withName("testmap")
                                .addToLabels(LabelKeys.FLAVOR, "")
                                .addToLabels(LabelKeys.ADDRESS, "anycast")
                                .addToLabels(LabelKeys.ADDRESS_TYPE, AddressType.QUEUE.value())
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
        when(pvcOp.list()).thenReturn(new PersistentVolumeClaimListBuilder().build());
        when(dcOp.list()).thenReturn(new DeploymentConfigListBuilder().addToItems(config).build());
        when(mapOp.list()).thenReturn(new ConfigMapListBuilder().addToItems(map).build());
        when(rcOp.list()).thenReturn(new ReplicationControllerListBuilder().build());

        List<DestinationCluster> clusters = helper.listClusters();
        assertThat(clusters.size(), is(2));

        DestinationCluster cluster = clusters.get(0);
        assertThat(cluster.getDestination().address(), is("anycast"));
        assertThat(cluster.getDestination().flavor(), is(""));
        assertFalse(cluster.getDestination().storeAndForward());
        assertFalse(cluster.getDestination().multicast());

        cluster = clusters.get(1);
        assertThat(cluster.getDestination().address(), is("myqueue"));
        assertThat(cluster.getDestination().flavor(), is("vanilla"));
        assertTrue(cluster.getDestination().storeAndForward());
        assertFalse(cluster.getDestination().multicast());
    }
}
