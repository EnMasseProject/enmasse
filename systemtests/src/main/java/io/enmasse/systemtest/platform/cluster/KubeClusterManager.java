/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform.cluster;

import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.framework.ThrowableRunner;
import io.enmasse.systemtest.framework.LoggerUtils;
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

import static io.enmasse.systemtest.platform.Kubernetes.getClient;

/**
 * Do the operation with cluster (scale, restart etc...)
 */
public class KubeClusterManager {

    private static final Logger LOGGER = LoggerUtils.getLogger();
    private static final Kubernetes KUBE = Kubernetes.getInstance();
    private static final KubeClusterManager INSTANCE = new KubeClusterManager();
    private static final String CMD = Kubernetes.getInstance().getCluster().getKubeCmd();
    private final Stack<ThrowableRunner> classConfigurations = new Stack<>();
    private final Stack<ThrowableRunner> methodConfigurations = new Stack<>();
    private Stack<ThrowableRunner> pointerConfigurations = classConfigurations;

    private KubeClusterManager() {
    }

    public static synchronized KubeClusterManager getInstance() {
        return INSTANCE;
    }

    public void setMethodConfigurations() {
        LOGGER.info("Setting pointer to method configurations");
        pointerConfigurations = methodConfigurations;
    }

    public void setClassConfigurations() {
        LOGGER.info("Setting pointer to class configurations");
        pointerConfigurations = classConfigurations;
    }

    public void restoreClassConfigurations() throws Exception {
        LoggerUtils.logDelimiter("-");
        LOGGER.info("Going to restore all class configurations");
        LoggerUtils.logDelimiter("-");
        if (classConfigurations.isEmpty()) {
            LOGGER.info("Nothing to delete");
        }
        while (!classConfigurations.empty()) {
            classConfigurations.pop().run();
        }
        LoggerUtils.logDelimiter("-");
        LOGGER.info("");
        classConfigurations.clear();
    }

    public void restoreMethodConfigurations() throws Exception {
        LoggerUtils.logDelimiter("-");
        LOGGER.info("Going to restore all method configurations");
        LoggerUtils.logDelimiter("-");
        if (methodConfigurations.isEmpty()) {
            LOGGER.info("Nothing to delete");
        }
        while (!methodConfigurations.empty()) {
            methodConfigurations.pop().run();
        }
        LoggerUtils.logDelimiter("-");
        LOGGER.info("");
        methodConfigurations.clear();
        pointerConfigurations = methodConfigurations;
    }

    public void scheduleConfigurationRestore(String what, ThrowableRunner runner) {
        LOGGER.info("Schedule restore of {}", what);
        pointerConfigurations.push(runner);
    }

    //////////////////////////////////////////////////////////////
    // User operations
    //////////////////////////////////////////////////////////////

    public List<Node> getWorkerNodes() {
        return getClient().nodes().list().getItems().stream().filter(node ->
                node.getMetadata().getName().contains("worker")).collect(Collectors.toList());
    }

    public List<Node> getMasterNodes() {
        return getClient().nodes().list().getItems().stream().filter(node ->
                node.getMetadata().getName().contains("master")).collect(Collectors.toList());
    }

    //////////////////////////////////////////////////////////////
    // Scale openshift cluster
    //////////////////////////////////////////////////////////////
    public void scaleOpenshiftWorkers(int workerCount) {
        LOGGER.info("Cluster is going to be scaled to {} worker nodes", workerCount);
        String machineSetName = Exec.execute(Arrays.asList(CMD, "get", "machinesets", "-n", "openshift-machine-api", "-o", "jsonpath='{.items[0].metadata.name}'")).getTrimmedStdOut();
        int originalCount = getReadyWorkerCount();

        scheduleConfigurationRestore("Scale worker nodes", () -> {
            Exec.executeAndCheck(CMD, "scale", "--replicas", Integer.toString(originalCount), "machineset", machineSetName, "-n", "openshift-machine-api");
            waitUtilWorkerScaled(originalCount);
        });

        Exec.executeAndCheck(CMD, "scale", "--replicas", Integer.toString(workerCount), "machineset", machineSetName, "-n", "openshift-machine-api");
        waitUtilWorkerScaled(workerCount);
    }

    private void waitUtilWorkerScaled(int workerCount) {
        TestUtils.waitUntilCondition(String.format("Cluster worker scale to %d", workerCount), waitPhase -> {
            int count = getReadyWorkerCount();
            LOGGER.info("Ready worker replicas {}/{}", count, workerCount);
            return count == workerCount;
        }, new TimeoutBudget(20, TimeUnit.MINUTES));
    }

    private int getReadyWorkerCount() {
        return Integer.parseInt(Exec.execute(Arrays.asList(CMD, "get", "machinesets", "-n", "openshift-machine-api", "-o", "jsonpath='{.items[0].status.readyReplicas}'"), false).getTrimmedStdOut());
    }

    //////////////////////////////////////////////////////////////
    // Evacuate pods from nodes
    //////////////////////////////////////////////////////////////
    public void drainNode(String nodeName) {
        LOGGER.info("Cluster node {} is going to drain", nodeName);
        setNodeSchedule(nodeName, false);
        Exec.executeAndCheck(600_000, CMD, "adm", "drain", nodeName, "--delete-local-data", "--ignore-daemonsets");
    }

    public void setNodeSchedule(String node, boolean schedule) {
        LOGGER.info("Set {} schedule {}", node, schedule);

        scheduleConfigurationRestore("Set node schedule", () -> Exec.executeAndCheck(CMD, "adm", !schedule ? "uncordon" : "cordon", node));

        Exec.executeAndCheck(CMD, "adm", schedule ? "uncordon" : "cordon", node);
    }

    //////////////////////////////////////////////////////////////
    // Restart cluster components
    //////////////////////////////////////////////////////////////
    public void restartKubeApi() {
        LOGGER.info("Restart kube-api server");
        getClient().pods().inAnyNamespace().withLabel("apiserver", "true").list().getItems().forEach(
                pod -> KUBE.deletePod(pod.getMetadata().getNamespace(), pod.getMetadata().getName())
        );
    }
}
