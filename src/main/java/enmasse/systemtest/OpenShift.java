package enmasse.systemtest;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles interaction with openshift cluster
 */
public class OpenShift {
    private final Environment environment;
    private final OpenShiftClient client;

    public OpenShift(Environment environment) {
        this.environment = environment;
        Config config = new ConfigBuilder()
                .withMasterUrl(environment.openShiftUrl())
                .withOauthToken(environment.openShiftToken())
                .withNamespace(environment.namespace())
                .withUsername(environment.openShiftUser())
                .build();
        client = new DefaultOpenShiftClient(config);

    }

    public Endpoint getEndpoint(String serviceName, String port) {
        Service service = client.services().inNamespace(environment.namespace()).withName(serviceName).get();
        return new Endpoint(service.getSpec().getPortalIP(), getPort(service, port));
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

    public String getRouteHost() {
        Route route = client.routes().inNamespace(environment.namespace()).withName("messaging").get();
        return route.getSpec().getHost();
    }

    public Endpoint getRestEndpoint() {
        if (isFullTemplate()) {
            return getEndpoint("restapi", "http");
        } else {
            return getEndpoint("admin", "restapi");
        }
    }

    public void setDeploymentReplicas(String name, int numReplicas) {
        client.deploymentConfigs()
                .inNamespace(environment.namespace())
                .withName(name)
                .scale(numReplicas, true);
    }

    public List<Pod> listPods() {
        return client.pods()
                .inNamespace(environment.namespace())
                .list()
                .getItems().stream()
                .filter(pod -> !pod.getMetadata().getName().endsWith("-deploy"))
                .collect(Collectors.toList());
    }

    public List<Pod> listPods(Map<String, String> labelSelector) {
        return client.pods()
                .inNamespace(environment.namespace())
                .withLabels(labelSelector)
                .list()
                .getItems().stream()
                    .filter(pod -> !pod.getMetadata().getName().endsWith("-deploy"))
                    .collect(Collectors.toList());
    }

    // Heuristic: if restapi service exists, we are running with a full template
    public boolean isFullTemplate() {
        Service service = client.services().withName("admin").get();
        return service == null;
    }

    public int getExpectedPods() {
        if (isFullTemplate()) {
            return 7; // config, storagecontroller, restapi, ragent, qdrouterd, subscription
        } else {
            return 4; // admin, qdrouterd, subscription
        }
    }
}
