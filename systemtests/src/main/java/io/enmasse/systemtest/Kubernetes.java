/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressResource;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import io.enmasse.systemtest.resources.AddressSpaceResource;
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
    protected final Environment environment;
    protected final KubernetesClient client;
    protected final String globalNamespace;
    private static Logger log = CustomLogger.getLogger();

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

    private ConfigMapList listConfigMaps(String type) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("type", type);
        return client.configMaps().inNamespace(globalNamespace).withLabels(labels).list();
    }

    /**
     * create new ConfigMap with new address space plan definition
     */
    public void createAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan, boolean replaceExisting) {
        String fullAddressSpacePlanName = String.format("address-space-plan-%s", addressSpacePlan.getConfigName());
        ConfigMap addressSpacePlanDefinition = new ConfigMap();

        addressSpacePlanDefinition.setApiVersion("v1"); // <apiVersion>
        addressSpacePlanDefinition.setKind("ConfigMap");         // <kind>

        ObjectMeta metadata = new ObjectMeta(); // <metadata>
        metadata.setName(fullAddressSpacePlanName);
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("type", "address-space-plan");
        metadata.setLabels(labels);
        addressSpacePlanDefinition.setMetadata(metadata); // </metadata>
        addressSpacePlanDefinition.setData(createAddressSpacePlanData(addressSpacePlan)); // <data></data>

        if (replaceExisting) {
            client.configMaps().inNamespace(globalNamespace).createOrReplace(addressSpacePlanDefinition);
        } else if (client.configMaps().inNamespace(globalNamespace).withName(fullAddressSpacePlanName).get() == null) {
            client.configMaps().create(addressSpacePlanDefinition);
        } else {
            throw new IllegalStateException(String.format("AddressSpacePlan '%s' already exists and replace is set to '%s'",
                    fullAddressSpacePlanName, replaceExisting));
        }
        log.info(String.format("AddressSpacePlan '%s' successfully '%s'", fullAddressSpacePlanName,
                replaceExisting ? "replaced" : "created"));
    }

    /**
     * remove address-space-plan ConfigMap according to its name
     */
    public boolean removeAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan) {
        String fullAddressSpacePlanName = String.format("address-space-plan-%s", addressSpacePlan.getName());
        return client.configMaps().inNamespace(globalNamespace).withName(fullAddressSpacePlanName).delete();
    }

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


    /**
     * create new ConfigMap with new address plan definition
     */
    public void createAddressPlanConfig(AddressPlan addressPlan, boolean replaceExisting) {
        String fullAddressPlanName = String.format("address-plan-%s", addressPlan.getName());
        ConfigMap addressPlanDefinition = new ConfigMap();
        addressPlanDefinition.setApiVersion("v1"); //apiVersion
        addressPlanDefinition.setKind("ConfigMap"); //Kind

        ObjectMeta metadata = new ObjectMeta(); // <metadata>
        metadata.setName(fullAddressPlanName);
        Map<String, String> labels = new LinkedHashMap<>(); // <labels>
        labels.put("type", "address-plan");
        metadata.setLabels(labels); // </labels>
        addressPlanDefinition.setMetadata(metadata); //</metadata>

        addressPlanDefinition.setData(createAddressPlanData(addressPlan)); // <data></data>

        if (replaceExisting) {
            client.configMaps().inNamespace(globalNamespace).createOrReplace(addressPlanDefinition);
        } else if (client.configMaps().inNamespace(globalNamespace).withName(fullAddressPlanName).get() == null) {
            client.configMaps().create(addressPlanDefinition);
        } else {
            throw new IllegalStateException(String.format("AddressPlan '%s' already exists and replace is set to '%s'",
                    fullAddressPlanName, replaceExisting));
        }
        log.info(String.format("AddressPlan '%s' successfully '%s'", fullAddressPlanName,
                replaceExisting ? "replaced" : "created"));
    }

    /**
     * remove address-plan ConfigMap according to its name
     */
    public boolean removeAddressPlanConfig(AddressPlan addressPlan) {
        String fullAddressPlanName = String.format("address-plan-%s", addressPlan.getName());
        return client.configMaps().inNamespace(globalNamespace).withName(fullAddressPlanName).delete();
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
                log.info(String.format("AddressPlan '%s' successfully appended into AddressSpace plan '%s'.",
                        fullAddressPlanName, fullAddressSpacePlanName));
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
                log.info(String.format("AddressPlan '%s' successfully removed from AddressSpace plan '%s'.",
                        fullAddressPlanName, fullAddressSpacePlanName));
            }
        }
        return removed;
    }
}
