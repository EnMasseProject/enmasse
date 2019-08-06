/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.executor.Executor;
import io.enmasse.systemtest.logs.CustomLogger;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;

public class Minikube extends Kubernetes {
    private static Logger log = CustomLogger.getLogger();

    protected Minikube(String globalNamespace) {
        super(globalNamespace, () -> {
            Config config = new ConfigBuilder().build();

            OkHttpClient httpClient = HttpClientUtils.createHttpClient(config);
            // Workaround https://github.com/square/okhttp/issues/3146
            httpClient = httpClient.newBuilder().protocols(Collections.singletonList(Protocol.HTTP_1_1)).build();

            return new DefaultKubernetesClient(httpClient, config);
        });
    }

    private static String runCommand(String... cmd) {
        try {
            Executor executor = new Executor(false);
            int returnCode = executor.execute(Arrays.asList(cmd), 10000);
            if (returnCode == 0) {
                return executor.getStdOut();
            } else {
                throw new RuntimeException(executor.getStdErr());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getIp(String namespace, String serviceName) {
        return runCommand("minikube", "service", "-n", namespace, "--format", "{{.IP}}", serviceName);
    }

    private String getPort(String namespace, String serviceName) {
        return runCommand("minikube", "service", "-n", namespace, "--format", "{{.Port}}", serviceName);
    }

    @Override
    public Endpoint getMasterEndpoint() {
        return new Endpoint(client.getMasterUrl());
    }

    @Override
    public Endpoint getRestEndpoint() {
        return new Endpoint(client.getMasterUrl());
    }

    @Override
    public Endpoint getKeycloakEndpoint() {
        return getExternalEndpoint("standard-authservice");
    }

    @Override
    public Endpoint getExternalEndpoint(String name) {
        return getExternalEndpoint(name, infraNamespace);
    }

    @Override
    public Endpoint getExternalEndpoint(String name, String namespace) {
        String externalName = name;
        if (!name.endsWith("-external")) {
            externalName += "-external";
        }
        Endpoint endpoint = new Endpoint(getIp(namespace, externalName), Integer.parseInt(getPort(namespace, externalName)));
        log.info("Minikube external endpoint - " + endpoint.toString());
        return endpoint;
    }
}
