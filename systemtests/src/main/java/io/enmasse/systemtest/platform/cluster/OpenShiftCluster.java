/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform.cluster;

import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.executor.ExecutionResultData;

import java.util.Arrays;

public class OpenShiftCluster implements KubeCluster {
    private static final String IDENTIFIER = "openshift";

    @Override
    public boolean isAvailable() {
        return Exec.isExecutableOnPath(getKubeCmd());
    }

    @Override
    public boolean isClusterUp() {
        ExecutionResultData data = Exec.execute(Arrays.asList(getKubeCmd(), "status"), false);
        if (!data.getRetCode()) {
            return false;
        } else return !data.getStdErr().contains("refused");
    }

    @Override
    public String getKubeCmd() {
        return "oc";
    }

    @Override
    public String toString() {
        return IDENTIFIER;
    }
}
