/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform.cluster;

import io.enmasse.systemtest.executor.Exec;

import java.util.Arrays;

public class KindCluster implements KubeCluster{

    public static final String IDENTIFIER = "kind";

    @Override
    public boolean isAvailable() {
        return Exec.isExecutableOnPath(IDENTIFIER)
                && !Exec.execute(IDENTIFIER, "get", "clusters").getStdOut().contains("No kind clusters found.");
    }

    @Override
    public boolean isClusterUp() {
        return Exec.execute(Arrays.asList(IDENTIFIER, "get", "nodes")).getStdOut().contains("kind-control-plane");
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
