/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.vertx.core.json.JsonObject;

public class PlanData {
    private String name;
    private String description;

    public PlanData(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public PlanData(JsonObject planData) {
        this.name = planData.getString("name");
        this.description = planData.getString("description");
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
