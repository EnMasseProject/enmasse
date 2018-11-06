/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.executor.Executor;
import io.enmasse.systemtest.resources.*;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import okhttp3.Response;
import org.slf4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public abstract class Kubernetes {
    private static Logger log = CustomLogger.getLogger();
    protected final Environment environment;
    protected final KubernetesClient client;
    protected final String globalNamespace;

    protected Kubernetes(Environment environment, KubernetesClient client, String globalNamespace) {
        this.environment = environment;
        this.client = client;
        this.globalNamespace = globalNamespace;
    }

    public String getNamespace() {
        return globalNamespace;
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

    public static Kubernetes create(Environment environment) {
        if (environment.useMinikube()) {
            return new Minikube(environment, environment.namespace());
        } else {
            return new OpenShift(environment, environment.namespace());
        }
    }

    public String getApiToken() {
        return environment.openShiftToken();
    }

    public Endpoint getEndpoint(String serviceName, String port) {
        return getEndpoint(serviceName, globalNamespace, port);
    }

    public Endpoint getEndpoint(String serviceName, String namespace, String port) {
        Service service = client.services().inNamespace(namespace).withName(serviceName).get();
        return new Endpoint(service.getSpec().getClusterIP(), getPort(service, port));
    }

    public Endpoint getOSBEndpoint() {
        return getEndpoint("service-broker", "https");
    }

    public abstract Endpoint getMasterEndpoint();

    public abstract Endpoint getRestEndpoint();

    public abstract Endpoint getKeycloakEndpoint();

    public abstract Endpoint getExternalEndpoint(String name);

    public UserCredentials getKeycloakCredentials() {
        Secret creds = client.secrets().inNamespace(globalNamespace).withName("keycloak-credentials").get();
        if (creds != null) {
            String username = new String(Base64.getDecoder().decode(creds.getData().get("admin.username")));
            String password = new String(Base64.getDecoder().decode(creds.getData().get("admin.password")));
            return new UserCredentials(username, password);
        } else {
            return null;
        }
    }

    public HashMap<String, String> getLogsOfTerminatedPods(String namespace) {
        HashMap<String, String> terminatedPodsLogs = new HashMap<>();
        try {
            client.pods().inNamespace(namespace).list().getItems().forEach(pod -> {
                pod.getStatus().getContainerStatuses().forEach(containerStatus -> {
                    log.info("pod:'{}' : restart count '{}'",
                            pod.getMetadata().getName(),
                            containerStatus.getRestartCount());
                    if (containerStatus.getRestartCount() > 0) {
                        terminatedPodsLogs.put(
                                pod.getMetadata().getName(),
                                client.pods().inNamespace(namespace)
                                        .withName(pod.getMetadata().getName())
                                        .inContainer(containerStatus.getName())
                                        .terminated().getLog());
                    }
                });
            });
        } catch (Exception allExceptions) {
            log.warn("Searching in terminated pods failed! No logs of terminated pods will be stored.");
            allExceptions.printStackTrace();
        }
        return terminatedPodsLogs;
    }

    public void setDeploymentReplicas(String name, int numReplicas) {
        client.apps().deployments().inNamespace(globalNamespace).withName(name).scale(numReplicas, true);
    }

    public void setStatefulSetReplicas(String name, int numReplicas) {
        client.apps().statefulSets().inNamespace(globalNamespace).withName(name).scale(numReplicas, true);
    }

    public List<Pod> listPods(String uuid) {
        return new ArrayList<>(client.pods().inNamespace(globalNamespace).withLabel("enmasse.io/uuid", uuid).list().getItems());
    }

    public List<Pod> listPods() {
        return new ArrayList<>(client.pods().inNamespace(globalNamespace).list().getItems());
    }

    public List<Pod> listPods(Map<String, String> labelSelector) {
        return client.pods().withLabels(labelSelector).list().getItems();
    }

    public List<Pod> listPods(Map<String, String> labelSelector, Map<String, String> annotationSelector) {
        return client.pods().withLabels(labelSelector).list().getItems().stream().filter(pod -> {
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

    public int getExpectedPods(String plan) {
        if (plan.endsWith("with-mqtt")) {
            return 6;
        } else if (plan.endsWith("medium") || plan.endsWith("unlimited")) {
            return 3;
        } else {
            return 2;
        }
    }

    public Watch watchPods(String uuid, Watcher<Pod> podWatcher) {
        return client.pods().withLabel("enmasse.io/infra", uuid).watch(podWatcher);
    }

    public List<Event> listEvents(String namespace) {
        return client.events().inNamespace(namespace).list().getItems();
    }

    public LogWatch watchPodLog(String name, String container, OutputStream outputStream) {
        return client.pods().withName(name).inContainer(container).watchLog(outputStream);
    }

    public Pod getPod(String name) {
        return client.pods().withName(name).get();
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

    public List<ConfigMap> listConfigMaps(String type) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("type", type);
        return listConfigMaps(labels);
    }

    public List<ConfigMap> listConfigMaps(Map<String, String> labels) {
        return client.configMaps().inNamespace(globalNamespace).withLabels(labels).list().getItems();
    }

    public List<Service> listServices(Map<String, String> labels) {
        return client.services().inNamespace(globalNamespace).withLabels(labels).list().getItems();
    }

    public List<Secret> listSecrets(Map<String, String> labels) {
        return client.secrets().inNamespace(globalNamespace).withLabels(labels).list().getItems();
    }

    public List<Deployment> listDeployments(Map<String, String> labels) {
        return client.apps().deployments().inNamespace(globalNamespace).withLabels(labels).list().getItems();
    }

    public List<StatefulSet> listStatefulSets(Map<String, String> labels) {
        return client.apps().statefulSets().inNamespace(globalNamespace).withLabels(labels).list().getItems();
    }

    public List<ServiceAccount> listServiceAccounts(Map<String, String> labels) {
        return client.serviceAccounts().inNamespace(globalNamespace).withLabels(labels).list().getItems();
    }

    public List<PersistentVolumeClaim> listPersistentVolumeClaims(Map<String, String> labels) {
        return client.persistentVolumeClaims().inNamespace(globalNamespace).withLabels(labels).list().getItems();
    }

    public ConfigMapList getAllConfigMaps(String namespace) {
        return client.configMaps().inNamespace(namespace).list();
    }

    public ConfigMap getConfigMap(String namespace, String configMapName) {
        return getAllConfigMaps(namespace).getItems().stream()
                .filter(configMap -> configMap.getMetadata().getName().equals(configMapName))
                .findFirst().get();
    }

    public void replaceConfigMap(String namespace, ConfigMap newConfigMap) {
        client.configMaps().inNamespace(namespace).createOrReplace(newConfigMap);
    }

    public void deleteNamespace(String namespace) {
        log.info("Following namespace will be removed - {}", namespace);
        client.namespaces().withName(namespace).delete();
    }

    /***
     * Creates pod from resources
     * @param namespace
     * @param configName
     * @throws Exception
     */
    public void createPodFromTemplate(String namespace, String configName) throws Exception {
        List<HasMetadata> resources = client.load(getClass().getResourceAsStream(configName)).inNamespace(namespace).get();
        HasMetadata resource = resources.get(0);
        Pod podRes = client.pods().inNamespace(namespace).create((Pod) resource);
        Pod result = client.pods().inNamespace(namespace)
                .withName(podRes.getMetadata().getName()).waitUntilReady(5, TimeUnit.SECONDS);
        log.info("Pod created {}", result.getMetadata().getName());
    }

    /***
     * Delete pod by name
     * @param namespace
     * @param podName
     * @throws Exception
     */
    public void deletePod(String namespace, String podName) throws Exception {
        client.pods().inNamespace(namespace).withName(podName).delete();
        log.info("Pod {} removed", podName);
    }

    /***
     * Returns pod ip
     * @param namespace
     * @param podName
     * @return string ip
     */
    public String getPodIp(String namespace, String podName) {
        return client.pods().inNamespace(namespace).withName(podName).get().getStatus().getPodIP();
    }

    /***
     * Creates application from resources
     * @param namespace
     * @param resources
     * @return String name of application
     * @throws Exception
     */
    public String createDeploymentFromResource(String namespace, Deployment resources) throws Exception {
        Deployment depRes = client.apps().deployments().inNamespace(namespace).create(resources);
        Deployment result = client.apps().deployments().inNamespace(namespace)
                .withName(depRes.getMetadata().getName()).waitUntilReady(2, TimeUnit.MINUTES);
        log.info("Deployment {} created", result.getMetadata().getName());
        return result.getMetadata().getName();
    }

    /***
     * Creates service from resource
     * @param resources
     * @return endpoint of service
     */
    public Endpoint createServiceFromResource(String namespace, Service resources) {
        Service serRes = client.services().inNamespace(namespace).create(resources);
        log.info("Service {} created", serRes.getMetadata().getName());
        return getEndpoint(serRes.getMetadata().getName(), namespace, "http");
    }

    /***
     * Deletes deployment by name
     * @param namespace
     * @param appName
     */
    public void deleteDeployment(String namespace, String appName) {
        client.apps().deployments().inNamespace(namespace).withName(appName).delete();
        log.info("Deployment {} removed", appName);
    }

    /***
     * Delete service by name
     * @param namespace
     * @param serviceName
     */
    public void deleteService(String namespace, String serviceName) {
        client.services().inNamespace(namespace).withName(serviceName).delete();
        log.info("Service {} removed", serviceName);
    }

    /***
     * Returns list of running containers in pod
     * @param podName name of pod
     * @return list of containers
     */
    public List<Container> getContainersFromPod(String podName) {
        return client.pods().inNamespace(globalNamespace).withName(podName).get().getSpec().getContainers();
    }

    /***
     * Returns log of conrainer in pod
     * @param podName name of pod
     * @param containerName name of container in pod
     * @return log
     */
    public String getLog(String podName, String containerName) {
        return client.pods().inNamespace(globalNamespace).withName(podName).inContainer(containerName).getLog();
    }

    /***
     * Wait until pod ready
     * @param pod
     * @throws Exception
     */
    public void waitUntilPodIsReady(Pod pod) throws InterruptedException {
        log.info("Waiting until pod: {} is ready", pod.getMetadata().getName());
        client.resource(pod).inNamespace(globalNamespace).waitUntilReady(5, TimeUnit.MINUTES);
    }

    /***
     * Get app label value
     * @return app label value
     */
    public String getEnmasseAppLabel() {
        return listPods().get(0).getMetadata().getLabels().get("app");
    }

    public String runOnPod(Pod pod, String container, String ... command) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        log.info("Running command on pod {}: {}", pod.getMetadata().getName(), command);
        CompletableFuture<String> data = new CompletableFuture<>();
        try (ExecWatch execWatch = client.pods().inNamespace(pod.getMetadata().getNamespace())
                .withName(pod.getMetadata().getName()).inContainer(container)
                .readingInput(null)
                .writingOutput(baos)
                .usingListener(new ExecListener() {
                    @Override
                    public void onOpen(Response response) {
                        log.info("Reading data...");
                    }

                    @Override
                    public void onFailure(Throwable throwable, Response response) {
                        data.completeExceptionally(throwable);

                    }

                    @Override
                    public void onClose(int i, String s) {
                        data.complete(baos.toString());
                    }
                }).exec(command)) {
            return data.get(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Exception running command {} on pod: {}", command, e.getMessage());
            return "";
        }
    }
}
