/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.resources.*;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
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

    public Endpoint getEndpoint(String namespace, String serviceName, String port) {
        Service service = client.services().inNamespace(namespace).withName(serviceName).get();
        return new Endpoint(service.getSpec().getClusterIP(), getPort(service, port));
    }

    public Endpoint getOSBEndpoint() {
        return getEndpoint(globalNamespace, "service-broker", "https");
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
        return terminatedPodsLogs;
    }

    public void setDeploymentReplicas(String tenantNamespace, String name, int numReplicas) {
        client.extensions().deployments().inNamespace(tenantNamespace).withName(name).scale(numReplicas, true);
    }

    public void setStatefulSetReplicas(String tenantNamespace, String name, int numReplicas) {
        client.apps().statefulSets().inNamespace(tenantNamespace).withName(name).scale(numReplicas, true);
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

    public int getExpectedPods(String plan) {
        if (plan.endsWith("without-mqtt")) {
            return 2;
        } else {
            return 5;
        }
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

    private ConfigMapList listConfigMaps(String type) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("type", type);
        return client.configMaps().inNamespace(globalNamespace).withLabels(labels).list();
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
        client.namespaces().withName(namespace).delete();
    }

    //------------------------------------------------------------------------------------------------
    // Create config maps
    //------------------------------------------------------------------------------------------------

    /**
     * create new ConfigMap with new address space plan definition
     */
    public void createAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan, boolean replaceExisting) {
        createConfigMap("address-space-plan", addressSpacePlan.getConfigName(),
                createAddressSpacePlanData(addressSpacePlan), replaceExisting);
    }

    /**
     * create new ConfigMap with new address plan definition
     */
    public void createAddressPlanConfig(AddressPlan addressPlan, boolean replaceExisting) {
        createConfigMap("address-plan", addressPlan.getName(),
                createAddressPlanData(addressPlan), replaceExisting);
    }

    /**
     * create new ConfigMap with new resource definition
     */
    public void createResourceDefinitionConfig(ResourceDefinition resourceDefinition, boolean replaceExisting) {
        createConfigMap("resource-definition", resourceDefinition.getName(),
                createResourceDefinitionData(resourceDefinition), replaceExisting);
    }

    /**
     * create new ConfigMap
     */
    private void createConfigMap(String type, String configName, Map<String, String> configMapData, boolean replaceExisting) {
        String fullName = String.format("%s-%s", type, configName);
        log.info("{}-{} ConfigMap will be created", type, configName);
        ConfigMap configMap = new ConfigMap();

        configMap.setApiVersion("v1"); // <apiVersion>
        configMap.setKind("ConfigMap");         // <kind>

        ObjectMeta metadata = new ObjectMeta(); // <metadata>
        metadata.setName(fullName);
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("type", type);
        metadata.setLabels(labels);
        configMap.setMetadata(metadata); // </metadata>
        configMap.setData(configMapData); // <data></data>

        if (replaceExisting) {
            client.configMaps().inNamespace(globalNamespace).createOrReplace(configMap);
        } else if (client.configMaps().inNamespace(globalNamespace).withName(fullName).get() == null) {
            client.configMaps().create(configMap);
        } else {
            throw new IllegalStateException(String.format("%s '%s' already exists and replace is set to '%s'",
                    type, fullName, replaceExisting));
        }
        log.info("{} '{}' successfully '{}'", type, fullName, replaceExisting ? "replaced" : "created");
    }

    //------------------------------------------------------------------------------------------------
    // Remove config maps
    //------------------------------------------------------------------------------------------------

    /**
     * remove address-space-plan ConfigMap according to its name
     */
    public boolean removeAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan) {
        return removeconfigMap("address-space-plan", addressSpacePlan.getConfigName());
    }

    /**
     * remove address-plan ConfigMap according to its name
     */
    public boolean removeAddressPlanConfig(AddressPlan addressPlan) {
        return removeconfigMap("address-plan", addressPlan.getName());
    }

    /**
     * remove resource-definition ConfigMap according to its name
     */
    public boolean removeResourceDefinitionConfig(ResourceDefinition resourceDefinition) {
        return removeconfigMap("resource-definition", resourceDefinition.getName());
    }

    private boolean removeconfigMap(String type, String name) {
        log.info("{}-{} ConfigMap will be removed", type, name);
        String fullName = String.format("%s-%s", type, name);
        return client.configMaps().inNamespace(globalNamespace).withName(fullName).delete();
    }

    //------------------------------------------------------------------------------------------------
    // Get config maps
    //------------------------------------------------------------------------------------------------

    public AddressSpacePlan getAddressSpacePlanConfig(String configName) {
        String fullAddressSpacePlanName = String.format("address-space-plan-%s", configName);

        AtomicReference<AddressSpacePlan> requestedPlan = new AtomicReference<>();
        listConfigMaps("address-space-plan").getItems().forEach(plan -> {
            if (plan.getMetadata().getName().equals(fullAddressSpacePlanName)) {
                log.info(String.format("AddressSpace plan '%s' found", fullAddressSpacePlanName));
                Map<String, String> data = plan.getData();
                JsonObject planDefinition = new JsonObject(data.get("definition"));
                JsonObject metadataDef = planDefinition.getJsonObject("metadata");

                String name = metadataDef.getString("name");
                String resourceDefName = metadataDef.getJsonObject("annotations").getString("enmasse.io/defined-by");
                AddressSpaceType type = AddressSpaceType.valueOf(planDefinition.getString("addressSpaceType").toUpperCase());

                JsonArray resourcesDef = planDefinition.getJsonArray("resources");
                List<AddressSpaceResource> resources = new ArrayList<>();

                for (int i = 0; i < resourcesDef.size(); i++) {
                    JsonObject resourceDef = resourcesDef.getJsonObject(i);
                    AddressSpaceResource resource = new AddressSpaceResource(
                            resourceDef.getString("name"),
                            resourceDef.getDouble("min"),
                            resourceDef.getDouble("max"));
                    resources.add(resource);
                }

                JsonArray addressPlansDef = planDefinition.getJsonArray("addressPlans");
                List<AddressPlan> addressPlans = new ArrayList<>();
                for (int i = 0; i < addressPlansDef.size(); i++) {
                    addressPlans.add(getAddressPlanConfig(addressPlansDef.getString(i)));
                }
                requestedPlan.set(new AddressSpacePlan(name, configName, resourceDefName, type, resources, addressPlans));
            }
        });
        return requestedPlan.get();
    }

    public AddressPlan getAddressPlanConfig(String configName) {
        String fullAddressPlanName = String.format("address-plan-%s", configName);
        AtomicReference<AddressPlan> requestedPlan = new AtomicReference<>();

        listConfigMaps("address-plan").getItems().forEach(addressPlan -> {
            if (addressPlan.getMetadata().getName().equals(fullAddressPlanName)) {
                log.info(String.format("Address plan '%s' found", fullAddressPlanName));
                Map<String, String> data = addressPlan.getData();
                JsonObject planDefinition = new JsonObject(data.get("definition"));
                JsonObject metadataDef = planDefinition.getJsonObject("metadata");

                JsonArray requiredResourcesDef = planDefinition.getJsonArray("requiredResources");
                List<AddressResource> requiredResources = new ArrayList<>();

                for (int i = 0; i < requiredResourcesDef.size(); i++) {
                    JsonObject resourceDef = requiredResourcesDef.getJsonObject(i);
                    AddressResource requiredResource = new AddressResource(
                            resourceDef.getString("name"),
                            resourceDef.getDouble("credit"));
                    requiredResources.add(requiredResource);
                }

                requestedPlan.set(new AddressPlan(
                        metadataDef.getString("name"),
                        AddressType.valueOf(planDefinition.getString("addressType").toUpperCase()),
                        requiredResources
                ));
            }
        });
        return requestedPlan.get();
    }

    public ResourceDefinition getResourceDefinitionConfig(String configName) {
        String fullResourceDefinitionName = String.format("resource-definition-%s", configName);
        AtomicReference<ResourceDefinition> requestedPlan = new AtomicReference<>();

        listConfigMaps("resource-definition").getItems().forEach(resourceDefinition -> {
            if (resourceDefinition.getMetadata().getName().equals(fullResourceDefinitionName)) {
                log.info(String.format("Resource definition '%s' found", fullResourceDefinitionName));
                Map<String, String> data = resourceDefinition.getData();
                JsonObject configDefinition = new JsonObject(data.get("definition"));
                JsonObject metadataDef = configDefinition.getJsonObject("metadata");
                String template = configDefinition.getString("template");

                JsonArray requiredResourcesDef = configDefinition.getJsonArray("parameters");
                List<ResourceParameter> parameters = new ArrayList<>();
                if (requiredResourcesDef != null) {
                    for (int i = 0; i < requiredResourcesDef.size(); i++) {
                        JsonObject resourceDef = requiredResourcesDef.getJsonObject(i);
                        ResourceParameter requiredResource = new ResourceParameter(
                                resourceDef.getString("name"),
                                resourceDef.getString("value"));
                        parameters.add(requiredResource);
                    }
                }

                requestedPlan.set(new ResourceDefinition(metadataDef.getString("name"), template, parameters));
            }
        });
        return requestedPlan.get();
    }

    //------------------------------------------------------------------------------------------------
    // Create config map data
    //------------------------------------------------------------------------------------------------

    /**
     * create body for ConfigMap definition for new address space plan definition
     */
    private Map<String, String> createAddressSpacePlanData(AddressSpacePlan addressSpacePlan) {
        Map<String, String> data = new LinkedHashMap<>();

        //definition
        JsonObject config = new JsonObject();
        config.put("apiVersion", "enmasse.io/v1");
        config.put("kind", "AddressSpacePlan");

        JsonObject definitionMetadata = new JsonObject(); // <metadata>
        definitionMetadata.put("name", addressSpacePlan.getName());

        JsonObject annotations = new JsonObject();  // <annotations>
        annotations.put("enmasse.io/defined-by", addressSpacePlan.getResourceDefName()); //"brokered-space", "standard-space", newly created...
        definitionMetadata.put("annotations", annotations); // </annotations>

        config.put("metadata", definitionMetadata); // </metadata>

        config.put("displayName", addressSpacePlan.getName()); //not parameterized for now
        config.put("displayOrder", "0");
        config.put("shortDescription", "Newly defined address space plan.");
        config.put("longDescription", "Newly defined address space plan.");
        config.put("uuid", "677485a2-0d96-11e8-ba89-0ed5f89f718b");
        config.put("addressSpaceType", addressSpacePlan.getType().toString().toLowerCase()); // "standard", "brokered", newly created...

        JsonArray defResources = new JsonArray(); // <resources>
        JsonObject brokerResource;
        for (AddressSpaceResource res : addressSpacePlan.getResources()) {
            brokerResource = new JsonObject();
            brokerResource.put("name", res.getName());
            brokerResource.put("min", res.getMin());
            brokerResource.put("max", res.getMax());
            defResources.add(brokerResource);
        }
        config.put("resources", defResources); // </resources>

        JsonArray defAddressPlan = new JsonArray(); // <addressPlans>
        for (AddressPlan plan : addressSpacePlan.getAddressPlans()) {
            defAddressPlan.add(plan.getName());
        }
        config.put("addressPlans", defAddressPlan); // </addressPlans>

        data.put("definition", config.toString());

        return data;
    }

    /**
     * create body for ConfigMap definition for new address plan definition
     */
    private Map<String, String> createAddressPlanData(AddressPlan addressPlan) {
        Map<String, String> data = new LinkedHashMap<>(); // <data>

        JsonObject config = new JsonObject();
        config.put("apiVersion", "enmasse.io/v1");
        config.put("kind", "AddressPlan");

        JsonObject definitionMetadata = new JsonObject(); // <metadata>
        definitionMetadata.put("name", addressPlan.getName());
        config.put("metadata", definitionMetadata);// </metadata>

        config.put("displayName", addressPlan.getName()); // not parametrized now
        config.put("displayOrder", "0");
        config.put("shortDescription", "Newly defined address plan.");
        config.put("longDescription", "Newly defined address plan.");
        config.put("uuid", "f64cc30e-0d9e-11e8-ba89-0ed5f89f718b");
        config.put("addressType", addressPlan.getType().toString());

        JsonArray defRequiredResources = new JsonArray(); // <requiredResources>
        JsonObject brokerResource;
        for (AddressResource res : addressPlan.getAddressResources()) {
            brokerResource = new JsonObject();
            brokerResource.put("name", res.getName());
            brokerResource.put("credit", res.getCredit());
            defRequiredResources.add(brokerResource);
        }
        config.put("requiredResources", defRequiredResources); // </requiredResources>

        data.put("definition", config.toString());
        return data;
    }

    /**
     * create body of ConfigMap definition for new resource definition
     */
    private Map<String, String> createResourceDefinitionData(ResourceDefinition resourceDefinition) {
        Map<String, String> data = new LinkedHashMap<>(); // <data>

        JsonObject config = new JsonObject();
        config.put("apiVersion", "enmasse.io/v1");
        config.put("kind", "ResourceDefinition");

        JsonObject definitionMetadata = new JsonObject(); // <metadata>
        definitionMetadata.put("name", resourceDefinition.getName());
        config.put("metadata", definitionMetadata);// </metadata>

        config.put("template", resourceDefinition.getTemplate());

        JsonArray parameters = new JsonArray(); // <parameters>
        JsonObject parameter;
        for (ResourceParameter rp : resourceDefinition.getParameters()) {
            parameter = new JsonObject();
            parameter.put("name", rp.getName());
            parameter.put("value", rp.getValue());
            parameters.add(parameter);
        }
        config.put("parameters", parameters); // </parameters>
        data.put("definition", config.toString());

        return data;
    }

    /**
     * append
     */
    public void appendAddressPlan(AddressPlan addressPlan, AddressSpacePlan addressSpacePlan) {
        String fullAddressSpacePlanName = String.format("address-space-plan-%s", addressSpacePlan.getConfigName());
        String fullAddressPlanName = String.format("address-plan-%s", addressPlan.getName());

        listConfigMaps("address-space-plan").getItems().forEach(plan -> {
            if (plan.getMetadata().getName().equals(fullAddressSpacePlanName)) {
                log.info(String.format("AddressSpace plan '%s' found", fullAddressSpacePlanName));
                Map<String, String> data = plan.getData();
                JsonObject planDefinition = new JsonObject(data.get("definition"));
                JsonArray addressPlansDef = planDefinition.getJsonArray("addressPlans");
                addressPlansDef.add(addressPlan.getName());
                planDefinition.put("addressPlans", addressPlansDef);
                data.replace("definition", planDefinition.toString());
                plan.setData(data);
                client.configMaps().inNamespace(globalNamespace).withName(plan.getMetadata().getName()).replace(plan);
                log.info("AddressPlan '{}' successfully appended into AddressSpace plan '{}'.",
                        fullAddressPlanName, fullAddressSpacePlanName);
            }
        });
    }

    public boolean removeAddressPlan(AddressPlan addressPlan, AddressSpacePlan addressSpacePlan) {
        boolean removed = false;
        String fullAddressSpacePlanName = String.format("address-space-plan-%s", addressSpacePlan.getConfigName());
        String fullAddressPlanName = String.format("address-plan-%s", addressPlan.getName());

        for (ConfigMap plan : listConfigMaps("address-space-plan").getItems()) {
            if (plan.getMetadata().getName().equals(fullAddressSpacePlanName)) {
                log.info(String.format("AddressSpace plan '%s' found", fullAddressSpacePlanName));
                Map<String, String> data = plan.getData();
                JsonObject planDefinition = new JsonObject(data.get("definition"));
                JsonArray addressPlansDef = planDefinition.getJsonArray("addressPlans");
                removed = addressPlansDef.remove(addressPlan.getName());
                planDefinition.put("addressPlans", addressPlansDef);
                data.replace("definition", planDefinition.toString());
                plan.setData(data);
                client.configMaps().inNamespace(globalNamespace).withName(plan.getMetadata().getName()).replace(plan);
                log.info("AddressPlan '{}' successfully removed from AddressSpace plan '{}'.",
                        fullAddressPlanName, fullAddressSpacePlanName);
            }
        }
        return removed;
    }
}
