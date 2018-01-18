/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.systemtest;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.LogWatch;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Kubernetes {
    protected final Environment environment;
    protected final KubernetesClient client;
    protected final String globalNamespace;

    protected Kubernetes(Environment environment, KubernetesClient client, String globalNamespace) {
        this.environment = environment;
        this.client = client;
        this.globalNamespace = globalNamespace;
    }

    public String getApiToken() {
        return environment.openShiftToken();
    }

    public Endpoint getEndpoint(String namespace, String serviceName, String port) {
        Service service = client.services().inNamespace(namespace).withName(serviceName).get();
        return new Endpoint(service.getSpec().getClusterIP(), getPort(service, port));
    }

    private static int getPort(Service service, String portName) {
        List<ServicePort> ports = service.getSpec().getPorts();
        for (ServicePort port : ports) {
            if (port.getName().equals(portName)) {
                return port.getPort();
            }
        }
        throw new IllegalArgumentException(
                "Unable to find port " + portName + " for service " + service.getMetadata().getName());
    }

    public abstract Endpoint getRestEndpoint();

    public abstract Endpoint getKeycloakEndpoint();

    public abstract Endpoint getExternalEndpoint(String namespace, String name);

    public KeycloakCredentials getKeycloakCredentials() {
        Secret creds = client.secrets().inNamespace(globalNamespace).withName("keycloak-credentials").get();
        if (creds != null) {
            String username = new String(Base64.getDecoder().decode(creds.getData().get("admin.username")));
            String password = new String(Base64.getDecoder().decode(creds.getData().get("admin.password")));
            return new KeycloakCredentials(username, password);
        } else {
            return null;
        }
    }

    public HashMap<String, String> getLogsOfTerminatedPods(String namespace) {
        HashMap<String, String> terminatedPodsLogs = new HashMap<>();
        client.pods().inNamespace(namespace).list().getItems().forEach(pod -> {
            pod.getStatus().getContainerStatuses().forEach(containerStatus -> {
                Logging.log.info("pod:'{}' : restart count '{}'",
                        pod.getMetadata().getName(),
                        containerStatus.getRestartCount());
                if (containerStatus.getRestartCount() > 0) {
                    terminatedPodsLogs.put(
                            pod.getMetadata().getName(),
                            client.pods().inNamespace(namespace).withName(pod.getMetadata().getName()).terminated().getLog());
                }
            });
        });
        return terminatedPodsLogs;
    }

    public void setDeploymentReplicas(String tenantNamespace, String name, int numReplicas) {
        client.extensions().deployments().inNamespace(tenantNamespace).withName(name).scale(numReplicas, true);
    }

    public List<Pod> listPods(String addressSpace) {
        return new ArrayList<>(client.pods().inNamespace(addressSpace).list().getItems());
    }

    public List<Pod> listPods(String addressSpace, Map<String, String> labelSelector) {
        return client.pods().inNamespace(addressSpace).withLabels(labelSelector).list().getItems();
    }

    public List<Pod> listPods(String addressSpace, Map<String, String> labelSelector, Map<String, String> annotationSelector) {
        return client.pods().inNamespace(addressSpace).withLabels(labelSelector).list().getItems().stream().filter(pod -> {
            for (Map.Entry<String, String> entry : annotationSelector.entrySet()) {
                if (pod.getMetadata().getAnnotations() == null
                        || pod.getMetadata().getAnnotations().get(entry.getKey()) == null
                        || !pod.getMetadata().getAnnotations().get(entry.getKey()).equals(entry.getValue())) {
                    return false;
                }
                return true;
            }
            return true;
        }).collect(Collectors.toList());
    }

    public int getExpectedPods() {
        return 5;
    }

    public Watch watchPods(String namespace, Watcher<Pod> podWatcher) {
        return client.pods().inNamespace(namespace).watch(podWatcher);
    }

    public List<Event> listEvents(String namespace) {
        return client.events().inNamespace(namespace).list().getItems();
    }

    public LogWatch watchPodLog(String namespace, String name, String container, OutputStream outputStream) {
        return client.pods().inNamespace(namespace).withName(name).inContainer(container).watchLog(outputStream);
    }

    public Pod getPod(String namespace, String name) {
        return client.pods().inNamespace(namespace).withName(name).get();
    }

    public Set<String> listNamespaces() {
        return client.namespaces().list().getItems().stream()
                .map(ns -> ns.getMetadata().getName())
                .collect(Collectors.toSet());
    }

    public String getKeycloakCA() throws UnsupportedEncodingException {
        Secret secret = client.secrets().inNamespace(globalNamespace).withName("standard-authservice-cert").get();
        if (secret == null) {
            throw new IllegalStateException("Unable to find CA cert for keycloak");
        }
        return new String(Base64.getDecoder().decode(secret.getData().get("tls.crt")), "UTF-8");
    }

    public static Kubernetes create(Environment environment) {
        if (environment.useMinikube()) {
            return new Minikube(environment, environment.namespace());
        } else {
            return new OpenShift(environment, environment.namespace());
        }
    }
}
