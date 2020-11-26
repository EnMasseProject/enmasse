package io.enmasse.systemtest.platform.cluster;

import io.enmasse.systemtest.executor.Exec;

import java.util.Arrays;

public class MicroK8sCluster implements KubeCluster {
    public static final String IDENTIFIER = "microk8s";

    @Override
    public boolean isAvailable() {
        return Exec.isExecutableOnPath(IDENTIFIER) && Exec.execute(IDENTIFIER, "status").getRetCode();
    }

    @Override
    public boolean isClusterUp() {
        return Exec.execute(Arrays.asList(IDENTIFIER, "status"), false).getRetCode();
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
