/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform.cluster;

import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.ThrowableRunner;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Node;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Do the operation with cluster (scale, restart etc...)
 */
public class KubeClusterManager {

    private static Logger LOGGER = CustomLogger.getLogger();
    private Kubernetes kube = Kubernetes.getInstance();
    private static KubeClusterManager instance;
    private final String CMD = Kubernetes.getInstance().getCluster().getKubeCmd();

    private KubeClusterManager() {
    }

    public static synchronized KubeClusterManager getInstance() {
        if (instance == null) {
            instance = new KubeClusterManager();
        }
        return instance;
    }

    //////////////////////////////////////////////////////////////
    // User operations
    //////////////////////////////////////////////////////////////
    public String loginUser(UserCredentials credentials) {
        return loginUser(credentials.getUsername(), credentials.getPassword());
    }

    public String loginUser(String username, String password) {
        return KubeCMDClient.loginUser(username, password);
    }

    public String getToken() {
        return Exec.execute(Arrays.asList(CMD, "whoami", "-t")).getStdOut();
    }

    public List<Node> getWorkerNodes() {
        return kube.getClient().nodes().list().getItems().stream().filter(node ->
                node.getMetadata().getName().contains("worker")).collect(Collectors.toList());
    }

    public List<Node> getMasterNodes() {
        return kube.getClient().nodes().list().getItems().stream().filter(node ->
                node.getMetadata().getName().contains("master")).collect(Collectors.toList());
    }

    //////////////////////////////////////////////////////////////
    // Scale openshift cluster
    //////////////////////////////////////////////////////////////
    public void scaleOpenshiftWorkers(int workerCount) {
        LOGGER.info("Cluster is going to be scaled to {} worker nodes", workerCount);
        String machineSetName = Exec.execute(Arrays.asList(CMD, "get", "machinesets", "-n", "openshift-machine-api", "-o", "jsonpath='{.items[0].metadata.name}'")).getTrimmedStdOut();
        Exec.executeAndCheck(CMD, "scale", "--replicas", Integer.toString(workerCount), "machineset", machineSetName, "-n", "openshift-machine-api");

        TestUtils.waitUntilCondition(String.format("Cluster worker scale to %d", workerCount), waitPhase -> {
            String count = Exec.execute(Arrays.asList(CMD, "get", "machinesets", "-n", "openshift-machine-api", "-o", "jsonpath='{.items[0].status.readyReplicas}'"), false)
                    .getTrimmedStdOut();
            LOGGER.info("Ready worker replicas {}/{}", count, workerCount);
            return count.equals(Integer.toString(workerCount));
        }, new TimeoutBudget(20, TimeUnit.MINUTES));
    }

    //////////////////////////////////////////////////////////////
    // Evacuate pods from nodes
    //////////////////////////////////////////////////////////////
    public void drainNode(String nodeName) {
        LOGGER.info("Cluster node {} is going to drain", nodeName);
        setNodeSchedule(nodeName, false);
        Exec.executeAndCheck(CMD, "adm", "drain", nodeName, "--delete-local-data", "--ignore-daemonsets");
    }

    public void setNodeSchedule(String node, boolean schedule) {
        LOGGER.info("Set {} schedule {}", node, schedule);
        Exec.executeAndCheck(CMD, "adm", schedule ? "uncordon" : "cordon", node);
    }

    //////////////////////////////////////////////////////////////
    // Restart cluster components
    //////////////////////////////////////////////////////////////
    public void restartKubeApi() {
        LOGGER.info("Restart kube-api server");
        kube.getClient().pods().inAnyNamespace().withLabel("apiserver", "true").list().getItems().forEach(
                pod -> kube.deletePod(pod.getMetadata().getNamespace(), pod.getMetadata().getName())
        );
    }
}
