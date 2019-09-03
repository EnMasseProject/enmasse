/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BrokerClusterTest {
    @Test
    public void testPatchPvcIfSame() throws Exception {
        BrokerCluster oldCluster = new BrokerCluster("broker", new KubernetesListBuilder()
                .addToStatefulSetItems(new StatefulSetBuilder()
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
                .build());

        oldCluster.updateResources(oldCluster, new StandardInfraConfig());
        assertFalse(oldCluster.shouldReplace());
    }

    @Test
    public void testReplaceIfPvcIsDifferent() throws Exception {
        BrokerCluster oldCluster = new BrokerCluster("broker", new KubernetesListBuilder()
                .addToStatefulSetItems(new StatefulSetBuilder()
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
                .build());

        BrokerCluster newCluster = new BrokerCluster("broker", new KubernetesListBuilder()
                .addToStatefulSetItems(new StatefulSetBuilder()
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
        assertTrue(oldCluster.shouldReplace());
    }
}
