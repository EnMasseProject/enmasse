/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AddressSpaceTypeData {
    private String name;
    private String description;
    private List<AddressTypeData> addressTypes;
    private List<PlanData> plans;

    public AddressSpaceTypeData(String name, String description, List<AddressTypeData> addressTypes, List<PlanData> plans) {
        this.name = name;
        this.description = description;
        this.addressTypes = addressTypes;
        this.plans = plans;
    }

    public AddressSpaceTypeData(JsonObject addressSpaceTypeData) {
        this.name = addressSpaceTypeData.getString("name");
        this.description = addressSpaceTypeData.getString("description");
        this.addressTypes = new ArrayList<>();
        JsonArray addressTypes = addressSpaceTypeData.getJsonArray("addressTypes");
        for (int i = 0; i < addressTypes.size(); i++) {
            this.addressTypes.add(new AddressTypeData(addressTypes.getJsonObject(i)));
        }
        this.plans = new ArrayList<>();
        JsonArray plansData = addressSpaceTypeData.getJsonArray("plans");
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

    public List<AddressTypeData> getAddressTypes() {
        return addressTypes;
    }

    public List<PlanData> getPlans() {
        return plans;
    }

    public AddressTypeData getAddressType(String name) {
        return this.addressTypes.stream().filter(a -> a.getName().equals(name)).collect(Collectors.toList()).get(0);
    }
}
