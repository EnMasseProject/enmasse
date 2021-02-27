/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BrokerClusterTest {
    @Test
    public void testPvcStaysTheSameIfNotChanged() throws Exception {
        BrokerCluster oldCluster = new BrokerCluster("broker", new KubernetesListBuilder()
                .addToItems(new StatefulSetBuilder()
                        .editOrNewMetadata()
                        .withName("broker")
                        .endMetadata()
                        .editOrNewSpec()
                        .withReplicas(1)
                        .addNewVolumeClaimTemplate()
                        .editOrNewMetadata()
                        .withName("myclaim")
                        .endMetadata()
                        .editOrNewSpec()
                        .addToAccessModes("ReadWriteOnce")
                        .withNewResources()
                        .addToRequests("storage", new Quantity("2Gi"))
                        .endResources()
                        .endSpec()
                        .endVolumeClaimTemplate()
                        .endSpec()
                        .build())
                .addToItems(new PersistentVolumeClaimBuilder()
                        .editOrNewMetadata()
                        .withName("myclaim")
                        .endMetadata()
                        .editOrNewSpec()
                        .addToAccessModes("ReadWriteOnce")
                        .withNewResources()
                        .addToRequests("storage", new Quantity("2Gi"))
                        .endResources()
                        .endSpec()
                        .build())
                .build());

        oldCluster.updateResources(oldCluster, new StandardInfraConfig());
        PersistentVolumeClaim newClaim = null;
        StatefulSet newBroker = null;
        for (HasMetadata resource : oldCluster.getResources().getItems()) {
            if (resource instanceof PersistentVolumeClaim) {
                newClaim = (PersistentVolumeClaim) resource;
            }
            if (resource instanceof StatefulSet) {
                newBroker = (StatefulSet) resource;
            }
        }

        assertEquals(new Quantity("2Gi"), newBroker.getSpec().getVolumeClaimTemplates().get(0).getSpec().getResources().getRequests().get("storage"));
        assertEquals(new Quantity("2Gi"), newClaim.getSpec().getResources().getRequests().get("storage"));
    }

    @Test
    public void testPvcIsModifiedStatefulStaysTheSameIfModified() throws Exception {
        BrokerCluster oldCluster = new BrokerCluster("broker", new KubernetesListBuilder()
                .addToItems(new StatefulSetBuilder()
                        .editOrNewMetadata()
                        .withName("broker")
                        .endMetadata()
                        .editOrNewSpec()
                        .withReplicas(1)
                        .addNewVolumeClaimTemplate()
                        .editOrNewMetadata()
                        .withName("myclaim")
                        .endMetadata()
                        .editOrNewSpec()
                        .addToAccessModes("ReadWriteOnce")
                        .withNewResources()
                        .addToRequests("storage", new Quantity("2Gi"))
                        .endResources()
                        .endSpec()
                        .endVolumeClaimTemplate()
                        .endSpec()
                        .build())
                .addToItems(new PersistentVolumeClaimBuilder()
                        .editOrNewMetadata()
                        .withName("myclaim")
                        .endMetadata()
                        .editOrNewSpec()
                        .addToAccessModes("ReadWriteOnce")
                        .withNewResources()
                        .addToRequests("storage", new Quantity("2Gi"))
                        .endResources()
                        .endSpec()
                        .build())
                .build());

        BrokerCluster newCluster = new BrokerCluster("broker", new KubernetesListBuilder()
                .addToItems(new StatefulSetBuilder()
                        .editOrNewMetadata()
                        .withName("broker")
                        .endMetadata()
                        .editOrNewSpec()
                        .withReplicas(1)
                        .addNewVolumeClaimTemplate()
                        .editOrNewMetadata()
                        .withName("myclaim")
                        .endMetadata()
                        .editOrNewSpec()
                        .addToAccessModes("ReadWriteOnce")
                        .withNewResources()
                        .addToRequests("storage", new Quantity("5Gi"))
                        .endResources()
                        .endSpec()
                        .endVolumeClaimTemplate()
                        .endSpec()
                        .build())
                .build());

        oldCluster.updateResources(newCluster, new StandardInfraConfig());
        PersistentVolumeClaim newClaim = null;
        StatefulSet newBroker = null;
        for (HasMetadata resource : oldCluster.getResources().getItems()) {
            if (resource instanceof PersistentVolumeClaim) {
                newClaim = (PersistentVolumeClaim) resource;
            }
            if (resource instanceof StatefulSet) {
                newBroker = (StatefulSet) resource;
            }
        }

        assertNotNull(newClaim);
        assertNotNull(newBroker);
        assertEquals(new Quantity("2Gi"), newBroker.getSpec().getVolumeClaimTemplates().get(0).getSpec().getResources().getRequests().get("storage"));
        assertEquals(new Quantity("5Gi"), newClaim.getSpec().getResources().getRequests().get("storage"));
    }
}
