/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.vertx.core.json.JsonObject;

import java.util.List;

public interface InfraSpecComponent {
    String BROKER_INFRA_RESOURCE = "broker";
    String ADMIN_INFRA_RESOURCE = "admin";
    String ROUTER_INFRA_RESOURCE = "router";

    List<InfraResource> getResources();

    void setResources(List<InfraResource> resources);

    String getType();

    public JsonObject toJson();


}
