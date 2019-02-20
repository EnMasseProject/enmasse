/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class BrokerInfraSpec implements InfraSpecComponent {

    List<InfraResource> resources;
    private final String type = InfraSpecComponent.BROKER_INFRA_RESOURCE;
    private String addressFullPolicy;
    private String storageClassName;

    public BrokerInfraSpec(List<InfraResource> resources) {
        this.resources = resources;
        this.addressFullPolicy = "FAIL";
        this.storageClassName = null;
    }

    public BrokerInfraSpec(List<InfraResource> resources, String addressFullPolicy) {
        this.resources = resources;
        this.addressFullPolicy = addressFullPolicy;
        this.storageClassName = null;
    }

    public BrokerInfraSpec(List<InfraResource> resources, String addressFullPolicy, String storageClassName) {
        this.resources = resources;
        this.addressFullPolicy = addressFullPolicy;
        this.storageClassName = storageClassName;
    }

    @Override
    public List<InfraResource> getResources() {
        return resources;
    }

    @Override
    public void setResources(List<InfraResource> resources) {

    }

    @Override
    public String getType() {
        return type;
    }

    public String getRequiredValueFromResource(String resource) throws IllegalStateException {
        for (InfraResource res : this.getResources()) {
            if (resource.equals(res.getName())) {
                return res.getValue();
            }
        }
        throw new IllegalStateException(String.format("address resource '%s' didn't found", resource));
    }

    @Override
    public JsonObject toJson() {
        JsonObject config = new JsonObject();

        if (addressFullPolicy != null) {
            config.put("addressFullPolicy", addressFullPolicy);
        }

        if (storageClassName != null) {
            config.put("storageClassName", storageClassName);
        }

        JsonObject resources = new JsonObject();
        for (InfraResource res : this.resources) {
            resources.put(res.getName(), res.getValue());
        }
        config.put("resources", resources);
        return config;
    }


    public static BrokerInfraSpec fromJson(JsonObject infraDefinition) {
        String addressFullPolicy = null;
        String storageClass = null;
        if (infraDefinition.containsKey("addressFullPolicy")) {
            addressFullPolicy = infraDefinition.getString("addressFullPolicy");
        }
        if (infraDefinition.containsKey("storageClassName")) {
            storageClass = infraDefinition.getString("storageClassName");
        }
        JsonObject requiredResourcesDef = infraDefinition.getJsonObject("resources");
        List<InfraResource> requiredResources = new ArrayList<>();

        requiredResourcesDef.stream().forEach((entry) -> {
            InfraResource requiredResource = new InfraResource(entry.getKey(), entry.getValue().toString());
            requiredResources.add(requiredResource);
        });

        return new BrokerInfraSpec(requiredResources, addressFullPolicy, storageClass);
    }
}
