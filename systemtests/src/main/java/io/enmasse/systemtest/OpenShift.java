package io.enmasse.systemtest;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles interaction with openshift cluster
 */
public class OpenShift {
    private final Environment environment;
    private final OpenShiftClient client;
    private final String globalNamespace;

    public OpenShift(Environment environment, String globalNamespace) {
        this.environment = environment;
        Config config = new ConfigBuilder().withMasterUrl(environment.openShiftUrl())
                .withOauthToken(environment.openShiftToken())
                .withUsername(environment.openShiftUser()).build();
        client = new DefaultOpenShiftClient(config);
        this.globalNamespace = globalNamespace;
    }

    public String getApiToken() {
        return environment.openShiftToken();
    }

    public OpenShiftClient getClient() {
        return client;
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

    public Endpoint getRestEndpoint() {
        Route route = client.routes().inNamespace(globalNamespace).withName("restapi").get();
        Endpoint endpoint = new Endpoint(route.getSpec().getHost(), 443);
        if (TestUtils.resolvable(endpoint)) {
            return endpoint;
        } else {
            Logging.log.info("Endpoint didn't resolve, falling back to service endpoint");
            return getEndpoint(globalNamespace, "address-controller", "https");
        }
    }

    public Endpoint getKeycloakEndpoint() throws InterruptedException {
        Route route = client.routes().inNamespace(globalNamespace).withName("keycloak").get();
        Endpoint endpoint = new Endpoint(route.getSpec().getHost(), 443);
        Logging.log.info("Testing endpoint : " + endpoint);
        if (TestUtils.resolvable(endpoint)) {
            return endpoint;
        } else {
            Logging.log.info("Endpoint didn't resolve, falling back to service endpoint");
            return getEndpoint(globalNamespace, "standard-authservice", "https");
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

    public Endpoint getRouteEndpoint(String namespace, String routeName) {
        Route route = client.routes().inNamespace(namespace).withName(routeName).get();
        return new Endpoint(route.getSpec().getHost(), 443);
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
        Secret secret = client.secrets().withName("standard-authservice-cert").get();
        if (secret == null) {
            throw new IllegalStateException("Unable to find CA cert for keycloak");
        }
        return new String(Base64.getDecoder().decode(secret.getData().get("tls.crt")), "UTF-8");
    }
}
