/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.cmdclients;

import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.logs.CustomLogger;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;

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

/**
 * Class represent abstract client which keeps common features of client
 */
public class KubeCMDClient extends CmdClient {
    protected static final int DEFAULT_SYNC_TIMEOUT = 10000;
    protected static final int ONE_MINUTE_TIMEOUT = 60000;
    protected static final int FIVE_MINUTES_TIMEOUT = 300000;
    protected static String CMD = getCMD();
    private static Logger log = CustomLogger.getLogger();

    public static String getCMD() {
        String cmd = CMD;
        if (cmd == null) {
            if (env.useMinikube()) {
                cmd = "kubectl";
            } else {
                cmd = "oc";
            }
        }
        return cmd;
    }

    /**
     * Create Custom Resource Address or AddressSpace
     *
     * @param definition in yaml or json format
     * @return result of execution
     */
    public static ExecutionResultData createOrUpdateCR(String namespace, String definition, boolean replace) throws IOException {
        final File defInFile = new File("crdefinition.file");
        try (FileWriter wr = new FileWriter(defInFile.getName())) {
            wr.write(definition);
            wr.flush();
            log.info("User '{}' created", defInFile.getAbsolutePath());
            return execute(Arrays.asList(CMD, replace ? "replace" : "apply", "-n", namespace, "-f", defInFile.getAbsolutePath()), DEFAULT_SYNC_TIMEOUT, true);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            Files.delete(Paths.get(defInFile.getAbsolutePath()));
        }
    }

    public static ExecutionResultData createCR(String definition) throws IOException {
        return createOrUpdateCR(env.namespace(), definition, false);
    }

    public static ExecutionResultData createCR(String namespace, String definition) throws IOException {
        return createOrUpdateCR(namespace, definition, false);
    }

    public static ExecutionResultData updateCR(String definition) throws IOException {
        return createOrUpdateCR(env.namespace(), definition, true);
    }

    public static ExecutionResultData patchCR(String kind, String name, String patchData) {
        log.info("Patching {} {} in {}", kind, name, env.namespace());
        return execute(Arrays.asList(CMD, "patch", "-n", env.namespace(), kind, name, "-p", patchData), DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData updateCR(String namespace, String definition) throws IOException {
        return createOrUpdateCR(namespace, definition, true);
    }

    public static String getOCUser() {
        List<String> command = Arrays.asList(CMD, "whoami");
        return execute(command, DEFAULT_SYNC_TIMEOUT, false).getStdOut().replace(System.getProperty("line.separator"), "");
    }

    public static void getCurrentProject() {
        List<String> command = Arrays.asList(CMD, "project");
        execute(command, DEFAULT_SYNC_TIMEOUT, true);
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
        return execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData getAddress(String namespace, String addressName) {
        return getAddress(namespace, addressName, Optional.empty());
    }

    public static ExecutionResultData getAddress(String namespace) {
        List<String> getCmd = getRessourcesCmd("get", "addresses", namespace, Optional.of("wide"));
        return execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static void dumpPodLogs(final Pod pod) {
        // we cannot use --all-containers, since kubectl is too old
        for (var c : pod.getStatus().getContainerStatuses()) {
            execute(DEFAULT_SYNC_TIMEOUT, true, CMD, "logs", pod.getMetadata().getName(), "-c", c.getName());
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
        return execute(deleteCmd, DEFAULT_SYNC_TIMEOUT, true);
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
        return execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData getAddressSpace(String namespace, String addressSpaceName) {
        return getAddressSpace(namespace, addressSpaceName, Optional.empty());
    }

    public static ExecutionResultData getAddressSpace(String namespace, Optional<String> format) {
        List<String> getCmd = getRessourcesCmd("get", "addressspaces", namespace, format);
        return execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
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
        return execute(ressourcesCmd, DEFAULT_SYNC_TIMEOUT, true);
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

    public static void loginUser(String apiToken) {
        List<String> cmd = Arrays.asList(CMD, "login", "--token=" + apiToken);
        execute(cmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static String loginUser(String username, String password) {
        List<String> cmd = Arrays.asList(CMD, "login", "-u", username, "-p", password);
        execute(cmd, DEFAULT_SYNC_TIMEOUT, true);
        cmd = Arrays.asList(CMD, "whoami", "-t");
        return execute(cmd, DEFAULT_SYNC_TIMEOUT, true)
                .getStdOut().replace(System.getProperty("line.separator"), "");
    }

    public static void createNamespace(String namespace) {
        List<String> cmd = Arrays.asList(CMD, "new-project", namespace);
        execute(cmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static void switchProject(String namespace) {
        List<String> cmd = Arrays.asList(CMD, "project", namespace);
        execute(cmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData getUser(String namespace) {
        List<String> getCmd = getRessourcesCmd("get", "messaginguser", namespace, Optional.of("wide"));
        return execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData getUser(String namespace, String addressSpace, String username) {
        List<String> getCmd = getRessourcesCmd("get", "messaginguser", namespace, String.format("%s.%s", addressSpace, username), Optional.empty());
        return execute(getCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData deleteUser(String namespace, String addressSpace, String username) {
        List<String> deleteCmd = getRessourcesCmd("delete", "messaginguser", namespace, String.format("%s.%s", addressSpace, username), Optional.empty());
        return execute(deleteCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData deletePodByLabel(String labelName, String labelValue) {
        List<String> deleteCmd = Arrays.asList(CMD, "delete", "pod", "-l", String.format("%s=%s", labelName, labelValue));
        return execute(deleteCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData runOnPod(String namespace, String podName, Optional<String> container, String... args) {
        List<String> runCmd = new ArrayList<>();
        String[] base = container.map(s -> new String[]{CMD, "exec", podName, "-n", namespace, "--container", s, "--"})
                .orElseGet(() -> new String[]{CMD, "exec", podName, "-n", namespace, "--"});
        Collections.addAll(runCmd, base);
        Collections.addAll(runCmd, args);
        return execute(runCmd, DEFAULT_SYNC_TIMEOUT, false);
    }

    public static ExecutionResultData runQDstat(String podName, String... args) {
        List<String> runCmd = new ArrayList<>();
        String[] base = new String[]{CMD, "exec", podName, "--", "qdstat", "-t 20"};
        Collections.addAll(runCmd, base);
        Collections.addAll(runCmd, args);
        return execute(runCmd, ONE_MINUTE_TIMEOUT, true);
    }

    public static ExecutionResultData copyPodContent(String podName, String source, String destination) {
        log.info("Copying file {} from pod {}", source, podName);
        List<String> deleteCmd = Arrays.asList(CMD, "cp", String.format("%s:%s", podName, source), destination);
        return execute(deleteCmd, DEFAULT_SYNC_TIMEOUT, false);
    }

    public static String getOpenshiftUserId(String username) {
        List<String> command = Arrays.asList(CMD, "get", "users", username, "-o", "jsonpath={.metadata.uid}");
        return execute(command, DEFAULT_SYNC_TIMEOUT, true).getStdOut()
                .replaceAll(System.getProperty("line.separator"), "");
    }

    public static ExecutionResultData getEvents(String namespace) {
        List<String> command = Arrays.asList(CMD, "get", "events",
                "--namespace", namespace,
                "--output", "custom-columns=LAST SEEN:{lastTimestamp},FIRST SEEN:{firstTimestamp},COUNT:{count},NAME:{metadata.name},KIND:{involvedObject.kind},SUBOBJECT:{involvedObject.fieldPath},TYPE:{type},REASON:{reason},SOURCE:{source.component},MESSAGE:{message}",
                "--sort-by={.lastTimestamp}");

        return execute(command, ONE_MINUTE_TIMEOUT, false);
    }

    public static ExecutionResultData getApiServices(String name) {
        List<String> command = Arrays.asList(CMD, "get", "apiservices", name,
                "--output", "custom-columns=NAME:{.name}",
                "--no-headers=true",
                "--sort-by={.name}");

        return execute(command, ONE_MINUTE_TIMEOUT, false);
    }

    public static ExecutionResultData deleteIoTConfig(String namespace, String name) {
        List<String> ressourcesCmd = getRessourcesCmd("delete", "iotconfig", namespace, name, Optional.empty());
        return execute(ressourcesCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData describePods(String namespace) {
        return execute(DEFAULT_SYNC_TIMEOUT, false, CMD, "-n", namespace, "describe", "pods");
    }

    public static ExecutionResultData describeNodes() {
        return execute(DEFAULT_SYNC_TIMEOUT, false, CMD, "describe", "nodes");
    }

    public static ExecutionResultData createFromFile(String namespace, Path path) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(path);
        return execute(Arrays.asList(CMD, "-n", namespace, "create", "-f", path.toString()), DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData applyFromFile(String namespace, Path path) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(path);
        return execute(Arrays.asList(CMD, "-n", namespace, "apply", "-f", path.toString()), DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData deleteFromFile(String namespace, Path path) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(path);
        return execute(Arrays.asList(CMD, "-n", namespace, "delete", "-f", path.toString()), DEFAULT_SYNC_TIMEOUT, true);
    }

    public static String getMessagingEndpoint(String namespace, String addressspace) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(addressspace);
        return execute(Arrays.asList(CMD, "-n", namespace, "get", "addressspace", addressspace, "-o", "jsonpath={.status.endpointStatuses[?(@.name==\"messaging\")].externalHost}"), DEFAULT_SYNC_TIMEOUT, true, false).getStdOut();
    }

    public static ExecutionResultData deleteNamespace(String name) {
        return execute(Arrays.asList(CMD, "delete", "namespace", name), FIVE_MINUTES_TIMEOUT, true);
    }


    public static ExecutionResultData applyCR(String definition) throws IOException {
        final File defInFile = new File("crdefinition.file");
        try (FileWriter wr = new FileWriter(defInFile.getName())) {
            wr.write(definition);
            wr.flush();
            log.info("User '{}' created", defInFile.getAbsolutePath());
            return execute(Arrays.asList(CMD, "apply", "-f", defInFile.getAbsolutePath()), DEFAULT_SYNC_TIMEOUT, true);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            Files.delete(Paths.get(defInFile.getAbsolutePath()));
        }
    }

    public static ExecutionResultData deleteResource(String namespace, String resource, String name) {
        List<String> ressourcesCmd = getRessourcesCmd("delete", resource, namespace, name, Optional.empty());
        return execute(ressourcesCmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static ExecutionResultData createGroupAndAddUser(String groupName, String username) {
        return execute(Arrays.asList(CMD, "adm", "groups", "new", groupName, username), DEFAULT_SYNC_TIMEOUT, true);
    }

}
