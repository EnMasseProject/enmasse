package io.enmasse.systemtest;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles interaction with openshift cluster
 */
public class OpenShift {
    private final Environment environment;
    private final OpenShiftClient client;
    private final String globalNamespace;
    private final String tenantNamespace;

    public OpenShift(Environment environment, String globalNamespace, String tenantNamespace) {
        this.environment = environment;
        Config config = new ConfigBuilder().withMasterUrl(environment.openShiftUrl())
                .withOauthToken(environment.openShiftToken())
                .withUsername(environment.openShiftUser()).build();
        client = new DefaultOpenShiftClient(config);
        this.globalNamespace = globalNamespace;
        this.tenantNamespace = tenantNamespace;
    }

    public OpenShiftClient getClient() {
        return client;
    }

    public Endpoint getEndpoint(String serviceName, String port) {
        return getEndpoint(tenantNamespace, serviceName, port);
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

    public Endpoint getRestEndpoint() throws InterruptedException {
        Route route = client.routes().inNamespace(globalNamespace).withName("restapi").get();
        Endpoint endpoint = new Endpoint(route.getSpec().getHost(), 80);
        Logging.log.info("Testing endpoint : " + endpoint);
        if (TestUtils.resolvable(endpoint)) {
            return endpoint;
        } else {
            Logging.log.info("Endpoint didn't resolve, falling back to service endpoint");
            return getEndpoint(globalNamespace,"address-controller", "http");
        }
    }

    public Endpoint getKeycloakEndpoint() throws InterruptedException {
        Route route = client.routes().inNamespace(globalNamespace).withName("keycloak").get();
        Endpoint endpoint = new Endpoint(route.getSpec().getHost(), 80);
        Logging.log.info("Testing endpoint : " + endpoint);
        if (TestUtils.resolvable(endpoint)) {
            return endpoint;
        } else {
            Logging.log.info("Endpoint didn't resolve, falling back to service endpoint");
            return getEndpoint(globalNamespace, "standard-authservice", "http");
        }
    }

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

    public void setDeploymentReplicas(String name, int numReplicas) {
        client.extensions().deployments().inNamespace(tenantNamespace).withName(name).scale(numReplicas, true);
    }

    public List<Pod> listPods(String addressSpace) {
        return new ArrayList<>(client.pods().inNamespace(addressSpace).list().getItems());
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
        if (environment.isMultitenant()) {
            return 5; // admin, qdrouterd, subscription, mqtt gateway, mqtt lwt
        } else {
            return 7; // address-controller, none-authservice, admin, qdrouterd, subscription, mqtt
                      // gateway, mqtt lwt
        }
    }

    public Endpoint getRouteEndpoint(String addressSpace, String routeName) {
        Route route = client.routes().inNamespace(addressSpace).withName(routeName).get();
        return new Endpoint(route.getSpec().getHost(), 443);
    }

    public Watch watchPods(String namespace, Watcher<Pod> podWatcher) {
        return client.pods().inNamespace(namespace).watch(podWatcher);
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
}
