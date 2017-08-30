package enmasse.systemtest;

import io.fabric8.kubernetes.api.model.Pod;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles interaction with openshift cluster
 */
public class OpenShift {
    private final Environment environment;
    private final OpenShiftClient client;
    private final String namespace;

    public OpenShift(Environment environment, String namespace) {
        this.environment = environment;
        Config config = new ConfigBuilder()
                .withMasterUrl(environment.openShiftUrl())
                .withOauthToken(environment.openShiftToken())
                .withUsername(environment.openShiftUser())
                .build();
        client = new DefaultOpenShiftClient(config);
        this.namespace = namespace;
    }

    public OpenShiftClient getClient() {
        return client;
    }

    public Endpoint getEndpoint(String serviceName, String port) {
        Service service = client.services().inNamespace(namespace).withName(serviceName).get();
        return new Endpoint(service.getSpec().getClusterIP(), getPort(service, port));
    }

    public Endpoint getSecureEndpoint() {
        return getEndpoint("messaging", "amqps");
    }

    public Endpoint getInsecureEndpoint() {
        return getEndpoint("messaging", "amqp");
    }

    private static int getPort(Service service, String portName) {
        List<ServicePort> ports = service.getSpec().getPorts();
        for (ServicePort port : ports) {
            if (port.getName().equals(portName)) {
                return port.getPort();
            }
        }
        throw new IllegalArgumentException("Unable to find port " + portName + " for service " + service.getMetadata().getName());
    }

    public Endpoint getRestEndpoint() throws InterruptedException {
        Route route = client.routes().inNamespace(environment.namespace()).withName("restapi").get();
        Endpoint endpoint = new Endpoint(route.getSpec().getHost(), 80);
        Logging.log.info("Testing endpoint : " + endpoint);
        if (TestUtils.resolvable(endpoint)) {
            return endpoint;
        } else {
            Logging.log.info("Endpoint didn't resolve, falling back to address controller service");
            return getEndpoint("address-controller", "http");
        }
    }

    public void setDeploymentReplicas(String name, int numReplicas) {
        client.extensions().deployments()
                .inNamespace(namespace)
                .withName(name)
                .scale(numReplicas, true);
    }

    public List<Pod> listPods() {
        return client.pods()
                .inNamespace(namespace)
                .list()
                .getItems().stream()
                .collect(Collectors.toList());
    }

    public List<Pod> listPods(Map<String, String> labelSelector) {
        return listPods(labelSelector, Collections.emptyMap());
    }

    public List<Pod> listPods(Map<String, String> labelSelector, Map<String, String> annotationSelector) {
        return client.pods()
                .inNamespace(namespace)
                .withLabels(labelSelector)
                .list()
                .getItems().stream()
                    .filter(pod -> {
                        for (Map.Entry<String, String> entry : annotationSelector.entrySet()) {
                            if (pod.getMetadata().getAnnotations() == null || pod.getMetadata().getAnnotations().get(entry.getKey()) == null || !pod.getMetadata().getAnnotations().get(entry.getKey()).equals(entry.getValue())) {
                                return false;
                            }
                            return true;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
    }

    public int getExpectedPods() {
        if (environment.isMultitenant()) {
            return 5; // admin, qdrouterd, subscription, mqtt gateway, mqtt lwt
        } else {
            return 7; // address-controller, none-authservice, admin, qdrouterd, subscription, mqtt gateway, mqtt lwt
        }
    }

    public Endpoint getRouteEndpoint(String routeName) {
        Route route = client.routes().inNamespace(namespace).withName(routeName).get();
        return new Endpoint(route.getSpec().getHost(), 443);
    }

    public Watch watchPods(Watcher<Pod> podWatcher) {
        return client.pods().watch(podWatcher);
    }

    public LogWatch watchPodLog(String name, String container, OutputStream outputStream) {
        return client.pods().withName(name).inContainer(container).watchLog(outputStream);
    }

    public Pod getPod(String name) {
        return client.pods().withName(name).get();
    }
}
