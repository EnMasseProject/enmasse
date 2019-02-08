/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.util.Arrays;

import io.enmasse.systemtest.executor.Executor;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class Minikube extends Kubernetes {
    protected Minikube(String globalNamespace) {
        super(new DefaultKubernetesClient(), globalNamespace);
    }

    private static String runCommand(String... cmd) {
        try {
            Executor executor = new Executor();
            int returnCode = executor.execute(Arrays.asList(cmd), 10000);
            if(returnCode == 0) {
                return executor.getStdOut();
            }else {
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
        String externalName = name;
        if (!name.endsWith("-external")) {
            externalName += "-external";
        }
        return new Endpoint(getIp(globalNamespace, externalName), Integer.parseInt(getPort(globalNamespace, externalName)));
    }
}
