/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AddressTypeData {
    private String name;
    private String description;
    private List<PlanData> plans;

    public AddressTypeData(String name, String description, List<PlanData> plans) {
        this.name = name;
        this.description = description;
        this.plans = plans;
    }

    public AddressTypeData(JsonObject addressTypeData) {
        this.name = addressTypeData.getString("name");
        this.description = addressTypeData.getString("description");
        this.plans = new ArrayList<>();
        JsonArray plansData = addressTypeData.getJsonArray("plans");
        for (int i = 0; i < plansData.size(); i++) {
            this.plans.add(new PlanData(plansData.getJsonObject(i)));
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<PlanData> getPlans() {
        return plans;
    }
}
