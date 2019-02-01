/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.enmasse.systemtest.AddressType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AddressPlanDefinition {

    List<AddressResource> addressResources;
    private String name;
    private AddressType type;

    public AddressPlanDefinition(String name, AddressType type, List<AddressResource> addressResources) {
        this.name = name;
        this.type = type;
        this.addressResources = addressResources;
    }

    public String getName() {
        return name;
    }

    public AddressType getType() {
        return type;
    }

    public List<AddressResource> getAddressResources() {
        return addressResources;
    }

    public double getRequiredCreditFromResource(String addressResource) throws java.lang.IllegalStateException {
        for (AddressResource res : this.getAddressResources()) {
            if (addressResource.equals(res.getName())) {
                return res.getCredit();
            }
        }
        throw new java.lang.IllegalStateException(String.format("address resource '%s' didn't found", addressResource));
    }

    public JsonObject toJson() {
        JsonObject config = new JsonObject();
        config.put("apiVersion", "admin.enmasse.io/v1beta1");
        config.put("kind", "AddressPlan");

        JsonObject definitionMetadata = new JsonObject(); // <metadata>
        definitionMetadata.put("name", this.getName());
        config.put("metadata", definitionMetadata);// </metadata>

        config.put("displayName", this.getName()); // not parametrized now
        config.put("displayOrder", 0);
        config.put("shortDescription", "Newly defined address plan.");
        config.put("longDescription", "Newly defined address plan.");
        config.put("uuid", "f64cc30e-0d9e-11e8-ba89-0ed5f89f718b");
        config.put("addressType", this.getType().toString());

        JsonArray defRequiredResources = new JsonArray(); // <requiredResources>
        JsonObject brokerResource;
        for (AddressResource res : this.getAddressResources()) {
            brokerResource = new JsonObject();
            brokerResource.put("name", res.getName());
            brokerResource.put("credit", res.getCredit());
            defRequiredResources.add(brokerResource);
        }
        config.put("requiredResources", defRequiredResources); // </requiredResources>
        return config;
    }


    public static AddressPlanDefinition fromJson(JsonObject planDefinition) {
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

        return new AddressPlanDefinition(
                metadataDef.getString("name"),
                AddressType.valueOf(planDefinition.getString("addressType").toUpperCase()),
                requiredResources);
    }
}
