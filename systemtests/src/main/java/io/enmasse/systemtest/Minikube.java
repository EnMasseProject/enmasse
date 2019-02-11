/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.util.Arrays;

import org.slf4j.Logger;

import io.enmasse.systemtest.executor.Executor;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class Minikube extends Kubernetes {
    private static Logger log = CustomLogger.getLogger();

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

    private static String removeQuotes(String value) {
        String cleaned = value;
        if(value.startsWith("\"")) {
            cleaned = cleaned.substring(1);
        }
        if(value.endsWith("\"")) {
            cleaned = cleaned.substring(0, cleaned.length()-1);
        }
        return cleaned;
    }

    private String getIp(String namespace, String serviceName) {
        return removeQuotes(runCommand("minikube", "service", "-n", namespace, "--format", "{{.IP}}", serviceName));
    }

    private String getPort(String namespace, String serviceName) {
        return removeQuotes(runCommand("minikube", "service", "-n", namespace, "--format", "{{.Port}}", serviceName));
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
        Endpoint endpoint = new Endpoint(getIp(globalNamespace, externalName), Integer.parseInt(getPort(globalNamespace, externalName)));
        log.info("Minikube external endpoint - " + endpoint.toString());
        return endpoint;
    }
}
