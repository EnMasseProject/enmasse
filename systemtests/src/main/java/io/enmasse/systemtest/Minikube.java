/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class Minikube extends Kubernetes {
    protected Minikube(Environment environment, String globalNamespace) {
        super(environment, new DefaultKubernetesClient(new ConfigBuilder().withMasterUrl(environment.openShiftUrl())
                .withOauthToken(environment.openShiftToken())
                .withUsername(environment.openShiftUser()).build()), globalNamespace);
    }

    private static String runCommand(String... cmd) {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd).redirectErrorStream(true);

        Process proc = null;
        try {
            proc = processBuilder.start();
            InputStream stdout = proc.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            reader.close();
            if (!proc.waitFor(1, TimeUnit.MINUTES)) {
                throw new RuntimeException("Command timed out");
            }
            return output.toString();
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
    public Endpoint getRestEndpoint() {
        return getExternalEndpoint(globalNamespace, "restapi");
    }

    @Override
    public Endpoint getKeycloakEndpoint() {
        return getExternalEndpoint(globalNamespace, "standard-authservice");
    }

    @Override
    public Endpoint getExternalEndpoint(String namespace, String name) {
        String externalName = name;
        if (!name.endsWith("-external")) {
            externalName += "-external";
        }
        return new Endpoint(getIp(namespace, externalName), Integer.parseInt(getPort(namespace, externalName)));
    }
}
