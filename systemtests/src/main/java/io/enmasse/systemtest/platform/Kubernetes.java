/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.AddressSpaceSchema;
import io.enmasse.address.model.AddressSpaceSchemaList;
import io.enmasse.address.model.CoreCrd;
import io.enmasse.address.model.DoneableAddress;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.address.model.DoneableAddressSpaceSchema;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressPlanList;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanList;
import io.enmasse.admin.model.v1.AdminCrd;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceList;
import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.BrokeredInfraConfigList;
import io.enmasse.admin.model.v1.ConsoleService;
import io.enmasse.admin.model.v1.ConsoleServiceList;
import io.enmasse.admin.model.v1.DoneableAddressPlan;
import io.enmasse.admin.model.v1.DoneableAddressSpacePlan;
import io.enmasse.admin.model.v1.DoneableAuthenticationService;
import io.enmasse.admin.model.v1.DoneableBrokeredInfraConfig;
import io.enmasse.admin.model.v1.DoneableConsoleService;
import io.enmasse.admin.model.v1.DoneableStandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigList;
import io.enmasse.iot.model.v1.DoneableIoTConfig;
import io.enmasse.iot.model.v1.DoneableIoTProject;
import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTConfigList;
import io.enmasse.iot.model.v1.IoTCrd;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectList;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.cluster.KubeCluster;
import io.enmasse.systemtest.platform.cluster.MinikubeCluster;
import io.enmasse.systemtest.platform.cluster.NoClusterException;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserCrd;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import okhttp3.Response;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class Kubernetes {
    private static Logger log = CustomLogger.getLogger();
    private static Kubernetes instance;
    protected final Environment environment;
    protected final KubernetesClient client;
    protected final String infraNamespace;
    protected static KubeCluster cluster;

    protected Kubernetes(String infraNamespace, Supplier<KubernetesClient> clientSupplier) {
        this.environment = Environment.getInstance();
        this.client = clientSupplier.get();
        this.infraNamespace = infraNamespace;
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
            try {
                cluster = KubeCluster.detect();
            } catch (NoClusterException ex) {
                log.error(ex.getMessage());
            }
            if (cluster.toString().equals(MinikubeCluster.IDENTIFIER)) {
                instance = new Minikube(Environment.getInstance().namespace());
            } else {
                instance = new OpenShift(Environment.getInstance(), Environment.getInstance().namespace());
            }
        }
        return instance;
    }

    public double getKubernetesVersion() {
        VersionInfo versionInfo = new DefaultKubernetesClient().getVersion();
        return Double.parseDouble(versionInfo.getMajor() + "." + versionInfo.getMinor().replace("+", ""));
    }

    public int getOcpVersion() {
        return getKubernetesVersion() >= 1.13 ? 4 : 3;
    }

    public String getInfraNamespace() {
        return infraNamespace;
    }

    public KubernetesClient getClient() {
        return client;
    }

    public KubeCluster getCluster() {
        return cluster;
    }

    ///////////////////////////////////////////////////////////////////////////////
    // client and crd clients
    ///////////////////////////////////////////////////////////////////////////////

    public MixedOperation<AddressSpace, AddressSpaceList, DoneableAddressSpace, Resource<AddressSpace, DoneableAddressSpace>> getAddressSpaceClient() {
        return getAddressSpaceClient(infraNamespace);
    }

    public MixedOperation<AddressSpace, AddressSpaceList, DoneableAddressSpace,
            Resource<AddressSpace, DoneableAddressSpace>> getAddressSpaceClient(String namespace) {
        return (MixedOperation<AddressSpace, AddressSpaceList, DoneableAddressSpace, Resource<AddressSpace, DoneableAddressSpace>>) client.customResources(CoreCrd.addressSpaces(), AddressSpace.class, AddressSpaceList.class, DoneableAddressSpace.class).inNamespace(namespace);
    }

    public MixedOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> getAddressClient() {
        return getAddressClient(infraNamespace);
    }

    public MixedOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> getAddressClient(String namespace) {
        return (MixedOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>>) client.customResources(CoreCrd.addresses(), Address.class, AddressList.class, DoneableAddress.class).inNamespace(namespace);
    }

    public MixedOperation<User, UserList, DoneableUser, Resource<User, DoneableUser>> getUserClient() {
        return getUserClient(infraNamespace);
    }

    public MixedOperation<User, UserList, DoneableUser,
            Resource<User, DoneableUser>> getUserClient(String namespace) {
        return (MixedOperation<User, UserList, DoneableUser, Resource<User, DoneableUser>>) client.customResources(UserCrd.messagingUser(), User.class, UserList.class, DoneableUser.class).inNamespace(namespace);
    }

    public MixedOperation<AddressSpacePlan, AddressSpacePlanList, DoneableAddressSpacePlan,
            Resource<AddressSpacePlan, DoneableAddressSpacePlan>> getAddressSpacePlanClient() {
        return getAddressSpacePlanClient(infraNamespace);
    }

    public MixedOperation<AddressSpacePlan, AddressSpacePlanList, DoneableAddressSpacePlan,
            Resource<AddressSpacePlan, DoneableAddressSpacePlan>> getAddressSpacePlanClient(String namespace) {
        return (MixedOperation<AddressSpacePlan, AddressSpacePlanList, DoneableAddressSpacePlan,
                Resource<AddressSpacePlan, DoneableAddressSpacePlan>>) client.customResources(AdminCrd.addressSpacePlans(), AddressSpacePlan.class, AddressSpacePlanList.class, DoneableAddressSpacePlan.class).inNamespace(namespace);
    }

    public MixedOperation<AddressPlan, AddressPlanList, DoneableAddressPlan,
            Resource<AddressPlan, DoneableAddressPlan>> getAddressPlanClient() {
        return getAddressPlanClient(infraNamespace);
    }

    public MixedOperation<AddressPlan, AddressPlanList, DoneableAddressPlan,
            Resource<AddressPlan, DoneableAddressPlan>> getAddressPlanClient(String namespace) {
        return (MixedOperation<AddressPlan, AddressPlanList, DoneableAddressPlan,
                Resource<AddressPlan, DoneableAddressPlan>>) client.customResources(AdminCrd.addressPlans(), AddressPlan.class, AddressPlanList.class, DoneableAddressPlan.class).inNamespace(namespace);
    }

    public MixedOperation<BrokeredInfraConfig, BrokeredInfraConfigList, DoneableBrokeredInfraConfig,
            Resource<BrokeredInfraConfig, DoneableBrokeredInfraConfig>> getBrokeredInfraConfigClient() {
        return getBrokeredInfraConfigClient(infraNamespace);
    }

    public MixedOperation<BrokeredInfraConfig, BrokeredInfraConfigList, DoneableBrokeredInfraConfig,
            Resource<BrokeredInfraConfig, DoneableBrokeredInfraConfig>> getBrokeredInfraConfigClient(String namespace) {
        return (MixedOperation<BrokeredInfraConfig, BrokeredInfraConfigList, DoneableBrokeredInfraConfig,
                Resource<BrokeredInfraConfig, DoneableBrokeredInfraConfig>>) client.customResources(AdminCrd.brokeredInfraConfigs(), BrokeredInfraConfig.class, BrokeredInfraConfigList.class, DoneableBrokeredInfraConfig.class).inNamespace(namespace);
    }

    public MixedOperation<StandardInfraConfig, StandardInfraConfigList, DoneableStandardInfraConfig,
            Resource<StandardInfraConfig, DoneableStandardInfraConfig>> getStandardInfraConfigClient() {
        return getStandardInfraConfigClient(infraNamespace);
    }

    public MixedOperation<StandardInfraConfig, StandardInfraConfigList, DoneableStandardInfraConfig,
            Resource<StandardInfraConfig, DoneableStandardInfraConfig>> getStandardInfraConfigClient(String namespace) {
        return (MixedOperation<StandardInfraConfig, StandardInfraConfigList, DoneableStandardInfraConfig,
                Resource<StandardInfraConfig, DoneableStandardInfraConfig>>) client.customResources(AdminCrd.standardInfraConfigs(), StandardInfraConfig.class, StandardInfraConfigList.class, DoneableStandardInfraConfig.class).inNamespace(namespace);
    }

    public MixedOperation<AuthenticationService, AuthenticationServiceList, DoneableAuthenticationService,
            Resource<AuthenticationService, DoneableAuthenticationService>> getAuthenticationServiceClient() {
        return getAuthenticationServiceClient(infraNamespace);
    }

    public MixedOperation<AuthenticationService, AuthenticationServiceList, DoneableAuthenticationService,
            Resource<AuthenticationService, DoneableAuthenticationService>> getAuthenticationServiceClient(String namespace) {
        return (MixedOperation<AuthenticationService, AuthenticationServiceList, DoneableAuthenticationService,
                Resource<AuthenticationService, DoneableAuthenticationService>>) client.customResources(AdminCrd.authenticationServices(), AuthenticationService.class, AuthenticationServiceList.class, DoneableAuthenticationService.class).inNamespace(namespace);
    }

    public MixedOperation<AddressSpaceSchema, AddressSpaceSchemaList, DoneableAddressSpaceSchema,
            Resource<AddressSpaceSchema, DoneableAddressSpaceSchema>> getSchemaClient() {
        return getSchemaClient(infraNamespace);
    }

    public MixedOperation<AddressSpaceSchema, AddressSpaceSchemaList, DoneableAddressSpaceSchema,
            Resource<AddressSpaceSchema, DoneableAddressSpaceSchema>> getSchemaClient(String namespace) {
        return (MixedOperation<AddressSpaceSchema, AddressSpaceSchemaList, DoneableAddressSpaceSchema,
                Resource<AddressSpaceSchema, DoneableAddressSpaceSchema>>) client.customResources(CoreCrd.addresseSpaceSchemas(), AddressSpaceSchema.class, AddressSpaceSchemaList.class, DoneableAddressSpaceSchema.class).inNamespace(namespace);
    }

    public MixedOperation<ConsoleService, ConsoleServiceList, DoneableConsoleService,
            Resource<ConsoleService, DoneableConsoleService>> getConsoleServiceClient() {
        return getConsoleServiceClient(infraNamespace);
    }

    public MixedOperation<ConsoleService, ConsoleServiceList, DoneableConsoleService,
            Resource<ConsoleService, DoneableConsoleService>> getConsoleServiceClient(String namespace) {
        return (MixedOperation<ConsoleService, ConsoleServiceList, DoneableConsoleService,
                Resource<ConsoleService, DoneableConsoleService>>) client.customResources(AdminCrd.consoleServices(), ConsoleService.class, ConsoleServiceList.class, DoneableConsoleService.class).inNamespace(namespace);
    }

    public MixedOperation<IoTConfig, IoTConfigList, DoneableIoTConfig, Resource<IoTConfig, DoneableIoTConfig>> getIoTConfigClient() {
        return getIoTConfigClient(infraNamespace);
    }

    public MixedOperation<IoTConfig, IoTConfigList, DoneableIoTConfig, Resource<IoTConfig, DoneableIoTConfig>> getIoTConfigClient(String namespace) {
        return (MixedOperation<IoTConfig, IoTConfigList, DoneableIoTConfig, Resource<IoTConfig, DoneableIoTConfig>>) client.customResources(IoTCrd.config(), IoTConfig.class, IoTConfigList.class, DoneableIoTConfig.class).inNamespace(namespace);
    }

    public MixedOperation<IoTProject, IoTProjectList, DoneableIoTProject, Resource<IoTProject, DoneableIoTProject>> getNonNamespacedIoTProjectClient() {
        return getIoTProjectClient(null);
    }

    public MixedOperation<IoTProject, IoTProjectList, DoneableIoTProject, Resource<IoTProject, DoneableIoTProject>> getIoTProjectClient(String namespace) {
        if (namespace == null) {
            return client.customResources(IoTCrd.project(), IoTProject.class, IoTProjectList.class, DoneableIoTProject.class);
        } else {
            return (MixedOperation<IoTProject, IoTProjectList, DoneableIoTProject, Resource<IoTProject, DoneableIoTProject>>) client.customResources(IoTCrd.project(), IoTProject.class, IoTProjectList.class, DoneableIoTProject.class).inNamespace(namespace);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    // help methods
    ///////////////////////////////////////////////////////////////////////////////

    public String getApiToken() {
        return environment.getApiToken();
    }

    public Endpoint getEndpoint(String serviceName, String namespace, String port) {
        Service service = client.services().inNamespace(namespace).withName(serviceName).get();
        Objects.requireNonNull(service, () -> String.format("Unable to find service '%s' in namespace '%s'", serviceName, namespace));
        return new Endpoint(service.getSpec().getClusterIP(), getPort(service, port));
    }

    public Endpoint getOSBEndpoint() {
        return getEndpoint("service-broker", infraNamespace, "https");
    }

    public abstract Endpoint getMasterEndpoint();

    public abstract Endpoint getRestEndpoint();

    public abstract Endpoint getKeycloakEndpoint();

    /**
     * Assumes infra namespace
     *
     * @param name
     * @return
     */
    public abstract Endpoint getExternalEndpoint(String name);

    public abstract Endpoint getExternalEndpoint(String name, String namespace);

    public UserCredentials getKeycloakCredentials() {
        Secret creds = client.secrets().inNamespace(infraNamespace).withName("keycloak-credentials").get();
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
        client.pods().inNamespace(namespace).list().getItems().forEach(pod -> {
            pod.getStatus().getContainerStatuses().forEach(containerStatus -> {
                log.info("pod:'{}', container:'{}' : restart count '{}'",
                        pod.getMetadata().getName(),
                        containerStatus.getName(),
                        containerStatus.getRestartCount());
                if (containerStatus.getRestartCount() > 0) {
                    String name = String.format("%s_%s", pod.getMetadata().getName(), containerStatus.getName());
                    try {
                        String log = client.pods().inNamespace(namespace)
                                .withName(pod.getMetadata().getName())
                                .inContainer(containerStatus.getName())
                                .terminated().getLog();
                        terminatedPodsLogs.put(
                                name,
                                log);
                    } catch (Exception e) {
                        log.warn("Failed to gather terminated log for {} with termination count {} (ignored)", name, containerStatus.getRestartCount());
                    }
                }
            });
        });
        return terminatedPodsLogs;
    }

    public Map<String, String> getLogsByLables(String namespace, Map<String, String> labels) {
        return getLogs(namespace, client.pods().inNamespace(namespace).withLabels(labels).list().getItems());
    }

    public Map<String, String> getLogsOfAllPods(String namespace) {
        return getLogs(namespace, client.pods().inNamespace(namespace).list().getItems());
    }

    public Map<String, String> getLogs(String namespace, List<Pod> pods) {
        Map<String, String> logs = new HashMap<>();
        try {
            if (pods == null || pods.isEmpty()) {
                log.info("No pods to get logs");
                return logs;
            }
            pods.forEach(pod -> {
                pod.getSpec().getContainers().forEach(container -> {
                    logs.put(pod.getMetadata().getName() + "-" + container.getName(),
                            client.pods().inNamespace(namespace)
                                    .withName(pod.getMetadata().getName())
                                    .inContainer(container.getName())
                                    .getLog());
                });
            });
        } catch (Exception e) {
            log.error("Error getting logs of pods", e);
        }
        return logs;
    }

    public void setDeploymentReplicas(String namespace, String name, int numReplicas) {
        client.apps().deployments().inNamespace(namespace).withName(name).scale(numReplicas, true);
    }

    public void setStatefulSetReplicas(String name, int numReplicas) {
        client.apps().statefulSets().inNamespace(infraNamespace).withName(name).scale(numReplicas, true);
    }

    public List<Pod> listPods(String namespace, String uuid) {
        return new ArrayList<>(client.pods().inNamespace(namespace).withLabel("enmasse.io/uuid", uuid).list().getItems());
    }

    public List<Pod> listPods(String namespace) {
        return new ArrayList<>(client.pods().inNamespace(namespace).list().getItems());
    }

    public List<Pod> listPods() {
        return new ArrayList<>(client.pods().inNamespace(infraNamespace).list().getItems());
    }

    public List<Pod> listPods(Map<String, String> labelSelector) {
        return client.pods().inNamespace(infraNamespace).withLabels(labelSelector).list().getItems();
    }

    public List<Pod> listPods(String namespace, Map<String, String> labelSelector) {
        return client.pods().inNamespace(namespace).withLabels(labelSelector).list().getItems();
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
        Secret secret = client.secrets().inNamespace(infraNamespace).withName("standard-authservice-cert").get();
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
        return client.configMaps().inNamespace(infraNamespace).withLabels(labels).list().getItems();
    }

    public List<Service> listServices(Map<String, String> labels) {
        return client.services().inNamespace(infraNamespace).withLabels(labels).list().getItems();
    }

    public List<Secret> listSecrets(Map<String, String> labels) {
        return client.secrets().inNamespace(infraNamespace).withLabels(labels).list().getItems();
    }

    public List<Deployment> listDeployments(Map<String, String> labels) {
        return client.apps().deployments().inNamespace(infraNamespace).withLabels(labels).list().getItems();
    }

    public List<StatefulSet> listStatefulSets(Map<String, String> labels) {
        return client.apps().statefulSets().inNamespace(infraNamespace).withLabels(labels).list().getItems();
    }

    public List<ServiceAccount> listServiceAccounts(Map<String, String> labels) {
        return client.serviceAccounts().inNamespace(infraNamespace).withLabels(labels).list().getItems();
    }

    public List<PersistentVolumeClaim> listPersistentVolumeClaims(Map<String, String> labels) {
        return client.persistentVolumeClaims().inNamespace(infraNamespace).withLabels(labels).list().getItems();
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
        if (!namespaceExists(namespace)) {
            Namespace ns = new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build();
            client.namespaces().create(ns);
        } else {
            log.info("Namespace {} already exists", namespace);
        }
    }

    public void createNamespace(String namespace, Map<String, String> labels) {
        log.info("Following namespace will be created = {}", namespace);
        if (!namespaceExists(namespace)) {
            Namespace ns = new NamespaceBuilder().withNewMetadata().withName(namespace).withLabels(labels).endMetadata().build();
            client.namespaces().create(ns);
        } else {
            log.info("Namespace {} already exists", namespace);
        }
    }

    public void deleteNamespace(String namespace) throws Exception {
        log.info("Following namespace will be removed - {}", namespace);
        if (namespaceExists(namespace)) {
            client.namespaces().withName(namespace).cascading(true).delete();

            TestUtils.waitUntilCondition("Namespace will be deleted", phase ->
                    !namespaceExists(namespace), new TimeoutBudget(5, TimeUnit.MINUTES));
        } else {
            log.info("Namespace {} already removed", namespace);
        }
    }

    public boolean namespaceExists(String namespace) {
        return client.namespaces().list().getItems().stream().map(n -> n.getMetadata().getName())
                .collect(Collectors.toList()).contains(namespace);
    }

    public void deletePod(String namespace, Map<String, String> labels) {
        log.info("Delete pods with labels: {}", labels.toString());
        client.pods().inNamespace(namespace).withLabels(labels).withPropagationPolicy("Background").delete();
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
        client.pods().inNamespace(namespace).withName(podName).cascading(true).delete();
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
    public void createServiceFromResource(String namespace, Service resources) {
        if (!serviceExists(namespace, resources.getMetadata().getName())) {
            Service serRes = client.services().inNamespace(namespace).create(resources);
            log.info("Service {} created", serRes.getMetadata().getName());
        } else {
            log.info("Service {} already exists", resources.getMetadata().getName());
        }
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
        client.extensions().ingresses().inNamespace(namespace).withName(ingressName).cascading(true).delete();
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
        client.configMaps().inNamespace(namespace).withName(configmapName).cascading(true).delete();
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
        client.apps().deployments().inNamespace(namespace).withName(appName).cascading(true).delete();
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
        client.services().inNamespace(namespace).withName(serviceName).cascading(true).delete();
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
        return client.pods().inNamespace(infraNamespace).withName(podName).get().getSpec().getContainers();
    }

    /***
     * Returns log of container in pod
     * @param podName name of pod
     * @param containerName name of container in pod
     * @return log
     */
    public String getLog(String podName, String containerName) {
        return client.pods().inNamespace(infraNamespace).withName(podName).inContainer(containerName).getLog();
    }

    /***
     * Wait until pod ready
     * @param pod pod instance
     * @throws Exception when pod is not ready in timeout
     */
    public void waitUntilPodIsReady(Pod pod) throws InterruptedException {
        log.info("Waiting until pod: {} is ready", pod.getMetadata().getName());
        client.resource(pod).inNamespace(pod.getMetadata().getNamespace()).waitUntilReady(5, TimeUnit.MINUTES);
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

    public ServiceAccount getServiceAccount(String namespace, String name) {
        return client.serviceAccounts().inNamespace(namespace).withName(name).get();
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
        client.serviceAccounts().inNamespace(namespace).withName(name).cascading(true).delete();
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
        client.secrets().inNamespace(namespace).withName(secret).cascading(true).delete();
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

    public abstract void createExternalEndpoint(String name, String namespace, Service service, ServicePort targetPort);

    public abstract void deleteExternalEndpoint(String namespace, String name);
}
