/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.TestUtils;
import org.slf4j.Logger;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.logs.CustomLogger;
import io.fabric8.kubernetes.api.model.Pod;

/**
 * Class represent abstract client which keeps common features of client
 */
public class KubeCMDClient {
    protected static final int DEFAULT_SYNC_TIMEOUT = 10000;
    protected static final int ONE_MINUTE_TIMEOUT = 60000;
    protected static final int FIVE_MINUTES_TIMEOUT = 300000;
    protected static final String CMD = getCMD();
    private static final Logger log = CustomLogger.getLogger();

    public static String getCMD() {
        return Kubernetes.getInstance().getCluster().getKubeCmd();
    }

    /**
     * Create Custom Resource Address or AddressSpace
     *
     * @param definition in yaml or json format
     * @return result of execution
     */
    public static ExecutionResultData createOrUpdateCR(String namespace, String definition, boolean replace, int timeout) throws IOException {
        final File defInFile = new File("crdefinition.file");
        try (FileWriter wr = new FileWriter(defInFile.getName())) {
            wr.write(definition);
            wr.flush();
            log.info("Resource '{}' created", defInFile.getAbsolutePath());
            return Exec.execute(Arrays.asList(CMD, replace ? "replace" : "apply", "-n", namespace, "-f", defInFile.getAbsolutePath()), timeout, true);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            Files.delete(Paths.get(defInFile.getAbsolutePath()));
        }
    }

    public static ExecutionResultData deleteCR(String namespace, String definition, int timeout) throws IOException {
        final File defInFile = new File("crdefinition.file");
        try (FileWriter wr = new FileWriter(defInFile.getName())) {
            wr.write(definition);
            wr.flush();
            return Exec.execute(Arrays.asList(CMD, "delete", "-n", namespace, "-f", defInFile.getAbsolutePath()), timeout, true);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            Files.delete(Paths.get(defInFile.getAbsolutePath()));
        }
    }

    public static ExecutionResultData createCR(String definition) throws IOException {
        return createCR(Environment.getInstance().namespace(), definition);
    }

    public static ExecutionResultData createCR(String namespace, String definition) throws IOException {
        return createCR(namespace, definition, DEFAULT_SYNC_TIMEOUT);
    }

    public static ExecutionResultData createCR(String namespace, String definition, int timeoutMillis) throws IOException {
        return createOrUpdateCR(namespace, definition, false, timeoutMillis);
    }

    public static ExecutionResultData updateCR(String definition) throws IOException {
        return updateCR(Environment.getInstance().namespace(), definition);
    }

    public static ExecutionResultData updateCR(String namespace, String definition) throws IOException {
        return createOrUpdateCR(namespace, definition, true, DEFAULT_SYNC_TIMEOUT);
    }

    public static ExecutionResultData patchCR(String kind, String name, String patchData) {
        log.info("Patching {} {} in {}", kind, name, Environment.getInstance().namespace());
        return Exec.execute(Arrays.asList(CMD, "patch", "--type=merge", "-n", Environment.getInstance().namespace(), kind, name, "-p", patchData), DEFAULT_SYNC_TIMEOUT, true);
    }

    public static String getOCUser() {
        List<String> command = Arrays.asList(CMD, "whoami");
        return Exec.execute(command, DEFAULT_SYNC_TIMEOUT, false).getStdOut().replace(System.getProperty("line.separator"), "");
    }

    public static void getCurrentProject() {
        List<String> command = Arrays.asList(CMD, "project");
        Exec.execute(command, DEFAULT_SYNC_TIMEOUT, true);
    }

    /**
     * Use CRD for get address by addressName in 'namespace'
     *
     * @param namespace   name of namespace where addresses exists
     * @param addressName name of address in appropriate format, use '-a' for all addresses
     * @param format      output format (yaml/json accepted)
     * @return result of execution
     */
    public static ExecutionResultData getAddress(String namespace, String addressName, Optional<String> format) {
        List<String> getCmd = getRessourcesCmd("get", "addresses", namespace, addressName, format);
        return Exec.execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData getAddress(String namespace, String addressName) {
        return getAddress(namespace, addressName, Optional.empty());
    }

    public static ExecutionResultData getAddress(String namespace) {
        List<String> getCmd = getRessourcesCmd("get", "addresses", namespace, Optional.of("wide"));
        return Exec.execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static void dumpPodLogs(final Pod pod) {
        // we cannot use --all-containers, since kubectl is too old
        for (var c : pod.getStatus().getContainerStatuses()) {
            Exec.execute(DEFAULT_SYNC_TIMEOUT, true, CMD, "logs", pod.getMetadata().getName(), "-c", c.getName());
        }
    }

    /**
     * Use CRD for delete address by addressName in 'namespace'
     *
     * @param namespace   name of namespace where addresses exists
     * @param addressName name of address in appropriate format; --all means remove all addresses
     * @return result of execution
     */
    public static ExecutionResultData deleteAddress(String namespace, String addressName) {
        List<String> deleteCmd = getRessourcesCmd("delete", "addresses", namespace, addressName, Optional.empty());
        return Exec.execute(deleteCmd, DEFAULT_SYNC_TIMEOUT, true);
    }


    /**
     * Use CRD for get addressspace by addressspace name in 'namespace'
     *
     * @param namespace        name of namespace where addresses exists
     * @param addressSpaceName name of address in appropriate format
     * @param format           output format (yaml/json accepted)
     * @return result of execution
     */
    public static ExecutionResultData getAddressSpace(String namespace, String addressSpaceName, Optional<String> format) {
        List<String> getCmd = getRessourcesCmd("get", "addressspaces", namespace, addressSpaceName, format);
        return Exec.execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData getAddressSpace(String namespace, String addressSpaceName) {
        return getAddressSpace(namespace, addressSpaceName, Optional.empty());
    }

    public static ExecutionResultData getAddressSpace(String namespace, Optional<String> format) {
        List<String> getCmd = getRessourcesCmd("get", "addressspaces", namespace, format);
        return Exec.execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
    }


    /**
     * Use CRD for delete addressspace by addressspace name in 'namespace'
     *
     * @param namespace        name of namespace where addresses exists
     * @param addressSpaceName name of address in appropriate format
     * @return result of execution
     */
    public static ExecutionResultData deleteAddressSpace(String namespace, String addressSpaceName) {
        List<String> ressourcesCmd = getRessourcesCmd("delete", "addressspaces", namespace, addressSpaceName, Optional.empty());
        return Exec.execute(ressourcesCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    private static List<String> getRessourcesCmd(String operation, String kind, String namespace, String resourceName, Optional<String> format) {
        List<String> cmd = new LinkedList<>(Arrays.asList(CMD, operation, kind, resourceName, "-n", namespace));
        format.ifPresent(s -> {
            cmd.add("-o");
            cmd.add(s);
        });
        return cmd;
    }

    private static List<String> getRessourcesCmd(String operation, String kind, String namespace, Optional<String> format) {
        List<String> cmd = new LinkedList<>(Arrays.asList(CMD, operation, kind, "-n", namespace));
        format.ifPresent(s -> {
            cmd.add("-o");
            cmd.add(s);
        });
        return cmd;
    }

    public static ExecutionResultData checkPermission(String operation, String kind, String namespace, String serviceaccount) {
        List<String> cmd = new LinkedList<>(Arrays.asList(CMD, "auth", "can-i", operation, kind, "-n", namespace, "--as", "system:serviceaccount:enmasse-monitoring:" + serviceaccount));
        return Exec.execute(cmd, DEFAULT_SYNC_TIMEOUT, false);
    }

    public static void loginUser(String apiToken) {
        List<String> cmd = Arrays.asList(CMD, "login", "--token=" + apiToken);
        Exec.execute(cmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static String loginUser(String username, String password) {
        List<String> cmd = Arrays.asList(CMD, "login", "-u", username, "-p", password);
        ExecutionResultData execute = Exec.execute(cmd, DEFAULT_SYNC_TIMEOUT, true);
        if (!execute.getRetCode()) {
            throw new IllegalStateException(String.format("Failed to login user : %s : %s", username, execute.getStdErr()));
        }
        cmd = Arrays.asList(CMD, "whoami", "-t");
        return Exec.execute(cmd, DEFAULT_SYNC_TIMEOUT, true)
                .getStdOut().replace(System.getProperty("line.separator"), "");
    }

    public static void createNamespace(String namespace) {
        List<String> cmd = Arrays.asList(CMD, "new-project", namespace);
        Exec.execute(cmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static void switchProject(String namespace) {
        List<String> cmd = Arrays.asList(CMD, "project", namespace);
        Exec.execute(cmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData getUser(String namespace) {
        List<String> getCmd = getRessourcesCmd("get", "messaginguser", namespace, Optional.empty());
        return Exec.execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData getUser(String namespace, String addressSpace, String username) {
        List<String> getCmd = getRessourcesCmd("get", "messaginguser", namespace, String.format("%s.%s", addressSpace, username), Optional.empty());
        return Exec.execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData deleteUser(String namespace, String addressSpace, String username) {
        List<String> deleteCmd = getRessourcesCmd("delete", "messaginguser", namespace, String.format("%s.%s", addressSpace, username), Optional.empty());
        return Exec.execute(deleteCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData deletePodByLabel(String labelName, String labelValue) {
        List<String> deleteCmd = Arrays.asList(CMD, "delete", "pod", "-n", Environment.getInstance().namespace(), "-l", String.format("%s=%s", labelName, labelValue));
        return Exec.execute(deleteCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData setResourceEnvVarByLabel(Optional<String> namespace, String resourceType, Optional<String> containers, String envVarName, String resourceName, String envVarValue) {
        ArrayList<String> setEnvCmd = new ArrayList<>();
        setEnvCmd.add(CMD);
        setEnvCmd.add("set");
        setEnvCmd.add("env");
        setEnvCmd.add(resourceType);
        setEnvCmd.add(resourceName);
        containers.ifPresent(s -> setEnvCmd.add("--containers=" + s));
        namespace.ifPresent(s -> setEnvCmd.add("--namespace=" + s));

        if (envVarValue == null) {
            setEnvCmd.add(envVarName + "-");
        } else {
            setEnvCmd.add(envVarName + "=" + envVarValue);
        }

        return Exec.execute(setEnvCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData runOnPod(String namespace, String podName, Optional<String> container, String... args) {
        List<String> runCmd = new ArrayList<>();
        String[] base = container.map(s -> new String[]{CMD, "exec", podName, "-n", namespace, "--container", s, "--"})
                .orElseGet(() -> new String[]{CMD, "exec", podName, "-n", namespace, "--"});
        Collections.addAll(runCmd, base);
        Collections.addAll(runCmd, args);
        return Exec.execute(runCmd, DEFAULT_SYNC_TIMEOUT, false);
    }

    public static ExecutionResultData runQDstat(String namespace, String podName, String... args) {
        List<String> runCmd = new ArrayList<>();
        String[] base = new String[]{CMD, "exec", podName, "-n", namespace, "--", "qdstat", "-t 20"};
        Collections.addAll(runCmd, base);
        Collections.addAll(runCmd, args);
        return Exec.execute(runCmd, ONE_MINUTE_TIMEOUT, true);
    }

    public static ExecutionResultData copyPodContent(String podName, String source, String destination) {
        log.info("Copying file {} from pod {}", source, podName);
        List<String> deleteCmd = Arrays.asList(CMD, "cp", String.format("%s:%s", podName, source), destination);
        return Exec.execute(deleteCmd, DEFAULT_SYNC_TIMEOUT, false);
    }

    public static String getOpenshiftUserId(String username) {
        List<String> command = Arrays.asList(CMD, "get", "users", username, "-o", "jsonpath={.metadata.uid}");
        return Exec.execute(command, DEFAULT_SYNC_TIMEOUT, true).getStdOut()
                .replaceAll(System.getProperty("line.separator"), "");
    }

    /**
     * Note: there are lots of kubernetes components creating events with "<nil>" timestamps, so I'm forced to remove the sorting by this values
     */
    public static ExecutionResultData getEvents(String namespace) {
        List<String> command = Arrays.asList(CMD, "get", "events",
                "--namespace", namespace,
                "--sort-by={.metadata.creationTimestamp}");

        return Exec.execute(command, ONE_MINUTE_TIMEOUT, false);
    }

    /**
     * Note: there are lots of kubernetes components creating events with "<nil>" timestamps, so I'm forced to remove the sorting by this values
     */
    public static ExecutionResultData getAllEvents() {
        List<String> command = Arrays.asList(CMD, "get", "events",
                "--all-namespaces=true",
                "--sort-by={.metadata.creationTimestamp}");

        ExecutionResultData data = Exec.execute(command, ONE_MINUTE_TIMEOUT, false);
        if (!data.getRetCode()) {
            log.info("Error returning events: {}", data.getStdErr());
        }
        return data;
    }

    public static ExecutionResultData deleteIoTConfig(String namespace, String name) {
        List<String> ressourcesCmd = getRessourcesCmd("delete", "iotconfig", namespace, name, Optional.empty());
        return Exec.execute(ressourcesCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData describePods(String namespace) {
        return Exec.execute(DEFAULT_SYNC_TIMEOUT, false, CMD, "-n", namespace, "describe", "pods");
    }

    public static ExecutionResultData describeNodes() {
        return Exec.execute(DEFAULT_SYNC_TIMEOUT, false, CMD, "describe", "nodes");
    }

    public static ExecutionResultData createFromFile(String namespace, Path path) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(path);
        return Exec.executeAndCheck(CMD, "-n", namespace, "create", "-f", path.toString());
    }

    public static ExecutionResultData applyFromFile(String namespace, Path path) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(path);
        return Exec.executeAndCheck(CMD, "-n", namespace, "apply", "-f", path.toString());
    }

    public static ExecutionResultData deleteFromFile(String namespace, Path path) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(path);
        return Exec.execute(Arrays.asList(CMD, "-n", namespace, "delete", "-f", path.toString()), ONE_MINUTE_TIMEOUT, false);
    }

    public static String getMessagingEndpoint(String namespace, String addressspace) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(addressspace);
        return Exec.execute(Arrays.asList(CMD, "-n", namespace, "get", "addressspace", addressspace, "-o", "jsonpath={.status.endpointStatuses[?(@.name==\"messaging\")].externalHost}"), ONE_MINUTE_TIMEOUT, true, false).getStdOut();
    }

    public static ExecutionResultData deleteNamespace(String name) {
        return Exec.execute(Arrays.asList(CMD, "delete", "namespace", name), FIVE_MINUTES_TIMEOUT, true);
    }

    public static ExecutionResultData deleteResource(String namespace, String resource, String name) {
        List<String> ressourcesCmd = getRessourcesCmd("delete", resource, namespace, name, Optional.empty());
        return Exec.execute(ressourcesCmd, ONE_MINUTE_TIMEOUT, true);
    }

    public static ExecutionResultData createGroupAndAddUser(String groupName, String username) {
        return Exec.execute(Arrays.asList(CMD, "adm", "groups", "new", groupName, username), ONE_MINUTE_TIMEOUT, true);
    }

    public static ExecutionResultData getConfigmaps(String namespace) {
        List<String> command = Arrays.asList(CMD, "get", "configmaps",
                "--namespace", namespace,
                "--output", "yaml");
        return Exec.execute(command, ONE_MINUTE_TIMEOUT, false);
    }

    public static ExecutionResultData getSecrets(String namespace) {
        List<String> command = Arrays.asList(CMD, "get", "secrets",
                "--namespace", namespace,
                "--output", "yaml");
        return Exec.execute(command, ONE_MINUTE_TIMEOUT, false);
    }

    public static ExecutionResultData getIoTConfig(String namespace) {
        List<String> command = Arrays.asList(CMD, "get", "iotconfig", "--namespace", namespace, "--output", "yaml");
        return Exec.execute(command, ONE_MINUTE_TIMEOUT, false);
    }

    public static ExecutionResultData runOnCluster(String... args) {
        return runOnClusterWithTimeout(ONE_MINUTE_TIMEOUT, args);
    }

    public static ExecutionResultData runOnClusterWithTimeout(int timeoutMillis, String... args) {
        List<String> command = new LinkedList<>();
        command.add(CMD);
        command.addAll(Arrays.asList(args));
        return Exec.execute(command, timeoutMillis, true);
    }

    public static ExecutionResultData runOnClusterWithoutLogger(String... args) {
        List<String> command = new LinkedList<>();
        command.add(CMD);
        command.addAll(Arrays.asList(args));
        return Exec.execute(command, ONE_MINUTE_TIMEOUT, false);
    }

    public static void awaitRollout(TimeoutBudget budget, String namespace, String deploymentName) {
        TestUtils.waitUntilCondition(String.format("Wait for deployment %s in namespace %s rollout to complete", deploymentName, namespace), waitPhase -> {
            ExecutionResultData rollout = Exec.execute(Arrays.asList(ITestBase.kubernetes.getCluster().getKubeCmd(), "rollout",
                    "status", "deployment", deploymentName,
                    "--namespace", namespace), true);
            return rollout.getRetCode();
        }, budget);
    }
}
