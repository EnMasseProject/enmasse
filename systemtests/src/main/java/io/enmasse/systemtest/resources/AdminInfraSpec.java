/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AdminInfraSpec implements InfraSpecComponent {

    List<InfraResource> resources;
    private final String type = InfraSpecComponent.ADMIN_INFRA_RESOURCE;

    public AdminInfraSpec(List<InfraResource> resources) {
        this.resources = resources;
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

        JsonObject resources = new JsonObject();
        for (InfraResource res : this.resources) {
            resources.put(res.getName(), res.getValue());
        }
        config.put("resources", resources);
        return config;
    }


    public static AdminInfraSpec fromJson(JsonObject infraDefinition) {
        JsonObject requiredResourcesDef = infraDefinition.getJsonObject("resources");
        List<InfraResource> requiredResources = new ArrayList<>();

        requiredResourcesDef.stream().forEach((entry) -> {
            InfraResource requiredResource = new InfraResource(entry.getKey(), entry.getValue().toString());
            requiredResources.add(requiredResource);
        });

        return new AdminInfraSpec(requiredResources);
    }
}
