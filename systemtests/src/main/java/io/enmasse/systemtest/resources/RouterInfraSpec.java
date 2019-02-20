/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class RouterInfraSpec implements InfraSpecComponent {

    List<InfraResource> resources;
    private final String type = InfraSpecComponent.ROUTER_INFRA_RESOURCE;
    private int linkCapacity;
    private int minReplicas;

    public RouterInfraSpec(List<InfraResource> resources) {
        this.resources = resources;
        this.linkCapacity = -1;
        this.minReplicas = -1;
    }

    public RouterInfraSpec(List<InfraResource> resources, int linkCapacity) {
        this.resources = resources;
        this.linkCapacity = linkCapacity;
        this.minReplicas = -1;
    }

    public RouterInfraSpec(List<InfraResource> resources, int linkCapacity, int minReplicas) {
        this.resources = resources;
        this.linkCapacity = linkCapacity;
        this.minReplicas = minReplicas;
    }

    @Override
    public List<InfraResource> getResources() {
        return resources;
    }

    @Override
    public void setResources(List<InfraResource> resources) {
        this.resources = resources;
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

        if (linkCapacity != -1) {
            config.put("linkCapacity", linkCapacity);
        }

        if (minReplicas != -1) {
            config.put("minReplicas", minReplicas);
        }

        JsonObject resources = new JsonObject();
        for (InfraResource res : this.resources) {
            resources.put(res.getName(), res.getValue());
        }
        config.put("resources", resources);
        return config;
    }


    public static RouterInfraSpec fromJson(JsonObject infraDefinition) {
        int linkCapacity = -1;
        int minReplicas = -1;
        if (infraDefinition.containsKey("linkCapacity")) {
            linkCapacity = infraDefinition.getInteger("linkCapacity");
        }
        if (infraDefinition.containsKey("minReplicas")) {
            minReplicas = infraDefinition.getInteger("minReplicas");
        }
        JsonObject requiredResourcesDef = infraDefinition.getJsonObject("resources");
        List<InfraResource> requiredResources = new ArrayList<>();

        requiredResourcesDef.stream().forEach((entry) -> {
            InfraResource requiredResource = new InfraResource(entry.getKey(), entry.getValue().toString());
            requiredResources.add(requiredResource);
        });

        return new RouterInfraSpec(requiredResources, linkCapacity, minReplicas);
    }
}
