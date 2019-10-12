/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform.cluster;

import io.enmasse.systemtest.logs.CustomLogger;
import org.slf4j.Logger;

import java.util.Arrays;

public interface KubeCluster {

    boolean isAvailable();

    boolean isClusterUp();

    String getKubeCmd();

    static KubeCluster detect() {
        Logger LOGGER = CustomLogger.getLogger();

        KubeCluster[] clusters = new KubeCluster[]{new MinikubeCluster(), new OpenShiftCluster()};
        KubeCluster cluster = null;
        for (KubeCluster kc : clusters) {
            if (kc.isAvailable()) {
                LOGGER.debug("Cluster {} is installed", kc);
                if (kc.isClusterUp()) {
                    LOGGER.debug("Cluster {} is running", kc);
                    cluster = kc;
                    break;
                } else {
                    LOGGER.debug("Cluster {} is not running", kc);
                }
            } else {
                LOGGER.debug("Cluster {} is not installed", kc);
            }
        }
        if (cluster == null) {
            throw new NoClusterException(
                    "Unable to find a cluster; tried " + Arrays.toString(clusters));
        }
        return cluster;
    }
}
