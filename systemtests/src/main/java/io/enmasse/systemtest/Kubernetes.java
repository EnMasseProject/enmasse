/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import okhttp3.Response;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class Kubernetes {
    private static Logger log = CustomLogger.getLogger();
    protected final Environment environment;
    protected final KubernetesClient client;
    protected final String globalNamespace;
    private static Kubernetes instance;

    protected Kubernetes(KubernetesClient client, String globalNamespace) {
        this.environment = Environment.getInstance();
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

    public static Kubernetes getInstance() {
        if (instance == null) {
            Environment env = Environment.getInstance();
            if (env.useMinikube()) {
                instance = new Minikube(env.namespace());
            } else {
                instance = new OpenShift(env, env.namespace());
            }
        }
        return instance;
    }

    public String getApiToken() {
        return environment.getApiToken();
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

    public Map<String, String> getLogsOfTerminatedPods(String namespace) {
        Map<String, String> terminatedPodsLogs = new HashMap<>();
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

    public Map<String, String> getLogsByLables(String namespace, Map<String, String> labels) {
        Map<String, String> logs = new HashMap<>();
        try {
            client.pods().inNamespace(namespace).withLabels(labels).list().getItems().stream()
                    .forEach(pod -> {
                        logs.put(pod.getMetadata().getName(),
                                client.pods().inNamespace(namespace)
                                        .withName(pod.getMetadata().getName())
                                        .getLog());
                    });
        } catch (Exception e) {
            log.error("Error getting logs by labels.");
            e.printStackTrace();
        }
        return logs;
    }

    public void setDeploymentReplicas(String name, int numReplicas) {
        client.apps().deployments().inNamespace(globalNamespace).withName(name).scale(numReplicas, true);
    }

    public void setStatefulSetReplicas(String name, int numReplicas) {
        client.apps().statefulSets().inNamespace(globalNamespace).withName(name).scale(numReplicas, true);
    }

    public List<Pod> listPods(String namespace, String uuid) {
        return new ArrayList<>(client.pods().inNamespace(namespace).withLabel("enmasse.io/uuid", uuid).list().getItems());
    }

    public List<Pod> listPods(String namespace) {
        return new ArrayList<>(client.pods().inNamespace(namespace).list().getItems());
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
                return pod.getMetadata().getAnnotations() != null
                        && pod.getMetadata().getAnnotations().get(entry.getKey()) != null
                        && pod.getMetadata().getAnnotations().get(entry.getKey()).equals(entry.getValue());
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
        return new String(Base64.getDecoder().decode(secret.getData().get("tls.crt")), StandardCharsets.UTF_8);
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

    public StorageClass getStorageClass(String name) {
        return client.storage().storageClasses().withName(name).get();
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

    public void createNamespace(String namespace) {
        log.info("Following namespace will be created = {}", namespace);
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build();
        client.namespaces().create(ns);
    }

    public void deleteNamespace(String namespace) {
        log.info("Following namespace will be removed - {}", namespace);
        client.namespaces().withName(namespace).delete();
    }

    public boolean namespaceExists(String namespace) {
        return client.namespaces().list().getItems().stream().map(n -> n.getMetadata().getName())
                .collect(Collectors.toList()).contains(namespace);
    }

    public void deletePod(String namespace, Map<String, String> labels) {
        log.info("Delete pods with labels: {}", labels.toString());
        client.pods().inNamespace(namespace).withLabels(labels).delete();
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
    public void deletePod(String namespace, String podName) {
        client.pods().inNamespace(namespace).withName(podName).delete();
        log.info("Pod {} removed", podName);
    }

    /***
     * Returns pod ip
     * @param namespace namespace
     * @param podName name of pod
     * @return string ip
     */
    public String getPodIp(String namespace, String podName) {
        return client.pods().inNamespace(namespace).withName(podName).get().getStatus().getPodIP();
    }

    /***
     * Creates application from resources
     * @param namespace namespace
     * @param resources deployment resource
     * @throws Exception whe deployment failed
     */
    public void createDeploymentFromResource(String namespace, Deployment resources) throws Exception {
        if (!deploymentExists(namespace, resources.getMetadata().getName())) {
            Deployment depRes = client.apps().deployments().inNamespace(namespace).create(resources);
            Deployment result = client.apps().deployments().inNamespace(namespace)
                    .withName(depRes.getMetadata().getName()).waitUntilReady(2, TimeUnit.MINUTES);
            log.info("Deployment {} created", result.getMetadata().getName());
        } else {
            log.info("Deployment {} already exists", resources.getMetadata().getName());
        }
    }

    /**
     * Create service from resource
     *
     * @param namespace namespace
     * @param resources service resource
     * @return endpoint of new service
     */
    public Endpoint createServiceFromResource(String namespace, Service resources) {
        if (!serviceExists(namespace, resources.getMetadata().getName())) {
            Service serRes = client.services().inNamespace(namespace).create(resources);
            log.info("Service {} created", serRes.getMetadata().getName());
        } else {
            log.info("Service {} already exists", resources.getMetadata().getName());
        }
        return getEndpoint(resources.getMetadata().getName(), namespace, "http");
    }

    /**
     * Creates ingress from resource
     *
     * @param namespace namespace
     * @param resources resources
     */
    public void createIngressFromResource(String namespace, Ingress resources) {
        if (!ingressExists(namespace, resources.getMetadata().getName())) {
            Ingress serRes = client.extensions().ingresses().inNamespace(namespace).create(resources);
            log.info("Ingress {} created", serRes.getMetadata().getName());
        } else {
            log.info("Ingress {} already exists", resources.getMetadata().getName());
        }
    }

    /**
     * Deletes ingress
     *
     * @param namespace   namespace
     * @param ingressName ingress name
     */
    public void deleteIngress(String namespace, String ingressName) {
        client.extensions().ingresses().inNamespace(namespace).withName(ingressName).delete();
        log.info("Ingress {} deleted", ingressName);
    }

    /**
     * Test if ingress already exists
     *
     * @param namespace   namespace
     * @param ingressName name of ingress
     * @return boolean
     */
    public boolean ingressExists(String namespace, String ingressName) {
        return client.extensions().ingresses().inNamespace(namespace).list().getItems().stream()
                .map(ingress -> ingress.getMetadata().getName()).collect(Collectors.toList()).contains(ingressName);
    }

    /**
     * Return host of ingress
     *
     * @param namespace   namespace
     * @param ingressName name of ingress
     * @return string host
     */
    public String getIngressHost(String namespace, String ingressName) {
        return client.extensions().ingresses().inNamespace(namespace).withName(ingressName).get().getSpec().getRules().get(0).getHost();
    }

    /**
     * Create configmap from resource
     *
     * @param namespace kubernetes namespace
     * @param resources configmap resources
     */
    public void createConfigmapFromResource(String namespace, ConfigMap resources) {
        if (!configmapExists(namespace, resources.getMetadata().getName())) {
            client.configMaps().inNamespace(namespace).create(resources);
            log.info("Configmap {} in namespace {} created", resources.getMetadata().getName(), namespace);
        } else {
            log.info("Configmap {} in namespace {} already exists", resources.getMetadata().getName(), namespace);
        }
    }

    /**
     * Delete configmap from resource
     *
     * @param namespace     kubernetes namespace
     * @param configmapName configmap
     */
    public void deleteConfigmap(String namespace, String configmapName) {
        client.configMaps().inNamespace(namespace).withName(configmapName).delete();
        log.info("Configmap {} in namespace {} deleted", configmapName, namespace);
    }

    /**
     * Test if configmap plready exists
     *
     * @param namespace     kubernetes namespace
     * @param configmapName configmap
     * @return boolean
     */
    public boolean configmapExists(String namespace, String configmapName) {
        return client.configMaps().inNamespace(namespace).list().getItems().stream()
                .map(configMap -> configMap.getMetadata().getName()).collect(Collectors.toList()).contains(configmapName);
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
     * Check if deployment exists
     * @param namespace kuberntes namespace name
     * @param appName name of deployment
     * @return true if deployment exists
     */
    public boolean deploymentExists(String namespace, String appName) {
        return client.apps().deployments().inNamespace(namespace).list().getItems().stream()
                .map(deployment -> deployment.getMetadata().getName()).collect(Collectors.toList()).contains(appName);
    }

    /***
     * Delete service by name
     * @param namespace kubernetes namespace
     * @param serviceName service name
     */
    public void deleteService(String namespace, String serviceName) {
        client.services().inNamespace(namespace).withName(serviceName).delete();
        log.info("Service {} removed", serviceName);
    }

    /**
     * Test if service already exists
     *
     * @param namespace   namespace
     * @param serviceName service name
     * @return boolean
     */
    public boolean serviceExists(String namespace, String serviceName) {
        return client.services().inNamespace(namespace).list().getItems().stream()
                .map(service -> service.getMetadata().getName()).collect(Collectors.toList()).contains(serviceName);
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
     * @param pod pod instance
     * @throws Exception when pod is not ready in timeout
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


    /**
     * Run command on kubernetes pod
     *
     * @param pod       pod instance
     * @param container name of container
     * @param command   command to run
     * @return stdout
     */
    public String runOnPod(Pod pod, String container, String... command) {
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
            return data.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Exception running command {} on pod: {} with exception {}", command, pod.getMetadata().getName(), e);
            return "";
        }
    }

    /**
     * Creates service account
     *
     * @param name      name of servcie account
     * @param namespace namespace
     * @return full name
     */
    public String createServiceAccount(String name, String namespace) {
        log.info("Create serviceaccount {} in namespace {}", name, namespace);
        client.serviceAccounts().inNamespace(namespace)
                .create(new ServiceAccountBuilder().withNewMetadata().withName(name).endMetadata().build());
        return "system:serviceaccount:" + namespace + ":" + name;
    }

    /**
     * Deletes service account
     *
     * @param name      name
     * @param namespace namesapce
     * @return full name
     */
    public String deleteServiceAccount(String name, String namespace) {
        log.info("Delete serviceaccount {} from namespace {}", name, namespace);
        client.serviceAccounts().inNamespace(namespace).withName(name).delete();
        return "system:serviceaccount:" + namespace + ":" + name;
    }

    /**
     * Returns service account token
     *
     * @param name      name
     * @param namespace namespace
     * @return token
     */
    public String getServiceaccountToken(String name, String namespace) {
        return new String(Base64.getDecoder().decode(client.secrets().inNamespace(namespace).list().getItems().stream()
                .filter(secret -> secret.getMetadata().getName().contains(name + "-token")).collect(Collectors.toList())
                .get(0).getData().get("token")), StandardCharsets.UTF_8);
    }

    /**
     * Creates pvc from resource
     *
     * @param namespace namespace
     * @param resources resources
     */
    public void createPvc(String namespace, PersistentVolumeClaim resources) {
        if (!pvcExists(namespace, resources.getMetadata().getName())) {
            PersistentVolumeClaim serRes = client.persistentVolumeClaims().inNamespace(namespace).create(resources);
            log.info("PVC {} created", serRes.getMetadata().getName());
        } else {
            log.info("PVC {} already exists", resources.getMetadata().getName());
        }
    }

    /**
     * Deletes pvc
     *
     * @param namespace namespace
     * @param pvcName   pvc name
     */
    public void deletePvc(String namespace, String pvcName) {
        client.persistentVolumeClaims().inNamespace(namespace).withName(pvcName).delete();
        log.info("PVC {} deleted", pvcName);
    }

    /**
     * Test if pvc already exists
     *
     * @param namespace namespace
     * @param pvcName   of pvc
     * @return boolean
     */
    public boolean pvcExists(String namespace, String pvcName) {
        return client.persistentVolumeClaims().inNamespace(namespace).list().getItems().stream()
                .map(pvc -> pvc.getMetadata().getName()).collect(Collectors.toList()).contains(pvcName);
    }

    /**
     * Creates pvc from resource
     *
     * @param namespace namespace
     * @param resources resources
     */
    public void createSecret(String namespace, Secret resources) {
        if (!secretExists(namespace, resources.getMetadata().getName())) {
            Secret serRes = client.secrets().inNamespace(namespace).create(resources);
            log.info("Secret {} created", serRes.getMetadata().getName());
        } else {
            log.info("Secret {} already exists", resources.getMetadata().getName());
        }
    }

    /**
     * Deletes pvc
     *
     * @param namespace namespace
     * @param secret    secret name
     */
    public void deleteSecret(String namespace, String secret) {
        client.secrets().inNamespace(namespace).withName(secret).delete();
        log.info("Secret {} deleted", secret);
    }

    /**
     * Test if secret already exists
     *
     * @param namespace namespace
     * @param secret    of pvc
     * @return boolean
     */
    public boolean secretExists(String namespace, String secret) {
        return client.secrets().inNamespace(namespace).list().getItems().stream()
                .map(sec -> sec.getMetadata().getName()).collect(Collectors.toList()).contains(secret);
    }

    public CustomResourceDefinition getCRD(String name) {
        return client.customResourceDefinitions().withName(name).get();
    }
}
