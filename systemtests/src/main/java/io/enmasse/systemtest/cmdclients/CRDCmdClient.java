/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.cmdclients;

import io.enmasse.systemtest.executor.ExecutionResultData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Class represent abstract client which keeps common features of client
 */
public class CRDCmdClient extends CmdClient {
    protected static int DEFAULT_SYNC_TIMEOUT = 10000;
    protected static String CMD = setUpKubernetesCmd();

    private static String setUpKubernetesCmd() {
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
    public static ExecutionResultData createCR(String namespace, String definition) throws IOException {
        File defInFile = null;
        try {
            defInFile = new File("crdefinition.file");
            FileWriter wr = new FileWriter(defInFile.getName());
            wr.write(definition);
            wr.flush();
            return execute(Arrays.asList(CMD, "create", "-n", namespace, "-f", defInFile.getAbsolutePath()), DEFAULT_SYNC_TIMEOUT, true);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (defInFile != null) {
                Files.delete(Paths.get(defInFile.getPath()));
            }
        }
    }

    public static ExecutionResultData createCR(String definition) throws IOException {
        return createCR(env.namespace(), definition);
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
        List<String> cmd = Arrays.asList(CMD, operation, kind, resourceName, "-n", namespace);
        format.ifPresent(s -> {
            cmd.add("-o");
            cmd.add(s);
        });
        return cmd;
    }

    public static void loginUser(String username, String password) {
        List<String> cmd = Arrays.asList(CMD, "login", "-u", username, "-p", password);
        execute(cmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static void createNamespace(String namespace) {
        List<String> cmd = Arrays.asList(CMD, "new-project", namespace);
        execute(cmd, DEFAULT_SYNC_TIMEOUT, true);
    }

    public static void switchProject(String namespace) {
        List<String> cmd = Arrays.asList(CMD, "project", namespace);
        execute(cmd, DEFAULT_SYNC_TIMEOUT, true);
    }
}
