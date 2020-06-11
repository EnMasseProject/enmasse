/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform.cluster;

import io.enmasse.systemtest.executor.Exec;

import java.util.Arrays;

public class KubernetesCluster implements KubeCluster {

    public static final String IDENTIFIER = "kubernetes";

    @Override
    public boolean isAvailable() {
        return Exec.isExecutableOnPath(IDENTIFIER);
    }

    @Override
    public boolean isClusterUp() {
        return Exec.execute(Arrays.asList(getKubeCmd(), "cluster-info"), false).getRetCode();
    }

    @Override
    public String getKubeCmd() {
        return "kubectl";
    }

    @Override
    public String toString() {
        return IDENTIFIER;
    }
}
