/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.cmdclients;

import io.enmasse.systemtest.executor.ExecutionResultData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class represent abstract client which keeps common features of client
 */
public class DockerCmdClient extends CmdClient {
    private static int DEFAULT_SYNC_TIMEOUT = 120000;
    private static String DOCKER_COMMAND = setUpDockerCommand();

    public static ExecutionResultData pull(String repository, String name, String tag) {
        if (tag == null)
            tag = "latest";
        String image = String.format("%s/%s:%s", repository, name, tag);
        return execute(Arrays.asList(DOCKER_COMMAND, "pull", image), DEFAULT_SYNC_TIMEOUT, false);
    }

    public static ExecutionResultData restartContainer(String containerName) {
        return execute(Arrays.asList(DOCKER_COMMAND, "restart", containerName), DEFAULT_SYNC_TIMEOUT, false);
    }

    public static ExecutionResultData runContainer(String image, String name, String... options) {
        List<String> command = new ArrayList<>(Arrays.asList(DOCKER_COMMAND, "run"));
        command.addAll(Arrays.asList(options));
        command.addAll(Arrays.asList("--name", name, image));
        return execute(command, DEFAULT_SYNC_TIMEOUT, false);
    }

    public static ExecutionResultData stopContainer(String containerName) {
        return execute(Arrays.asList(DOCKER_COMMAND, "stop", containerName), DEFAULT_SYNC_TIMEOUT, false);
    }

    public static ExecutionResultData removeContainer(String containerName) {
        return execute(Arrays.asList(DOCKER_COMMAND, "rm", containerName, "-f"), DEFAULT_SYNC_TIMEOUT, false);
    }

    public static ExecutionResultData removeImage(String imageName) {
        return execute(Arrays.asList(DOCKER_COMMAND, "rmi", imageName, "-f"), DEFAULT_SYNC_TIMEOUT, false);
    }

    public static boolean isContainerRunning(String containerName) {
        return execute(Arrays.asList(DOCKER_COMMAND, "inspect", containerName, "--format", "'{{.State.Running}}'"),
                DEFAULT_SYNC_TIMEOUT, false).getStdOut().contains("true");
    }

    public static ExecutionResultData copyToContainer(String containerName, String src, String dest) {
        return execute(Arrays.asList(DOCKER_COMMAND, "cp", src, String.format("%s:%s", containerName, dest)),
                DEFAULT_SYNC_TIMEOUT, false);
    }

    private static String setUpDockerCommand() {
        String cmd = DOCKER_COMMAND;
        if (cmd == null) {
            if (execute(Collections.singletonList("docker"), DEFAULT_SYNC_TIMEOUT, false).getRetCode()) {
                cmd = "docker";
            } else {
                cmd = "docker-latest";
            }
        }
        return cmd;
    }
}
