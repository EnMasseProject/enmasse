/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.apiclients.AdminApiClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AddressSpacePlan {

    private String name;
    private String infraConfigName;
    private AddressSpaceType type;
    private List<AddressSpaceResource> resources;
    private List<AddressPlan> addressPlans = new ArrayList<>();

    public AddressSpacePlan(String name, String infraConfigName, AddressSpaceType type, List<AddressSpaceResource> resources, List<AddressPlan> addressPlans) {
        this.name = name;
        this.infraConfigName = infraConfigName;
        this.type = type;
        this.resources = resources;
        this.addressPlans = addressPlans;
    }

    public AddressSpacePlan(String name, String configName, String infraConfigName, AddressSpaceType type, List<AddressSpaceResource> resources) {
        this.name = name;
        this.infraConfigName = infraConfigName;
        this.type = type;
        this.resources = resources;
    }


    public String getName() {
        return name;
    }
    public String getInfraConfigName() {
        return infraConfigName;
    }

    public AddressSpaceType getType() {
        return type;
    }

    public List<AddressSpaceResource> getResources() {
        return resources;
    }

    public List<AddressPlan> getAddressPlans() {
        return addressPlans;
    }

    public JsonObject toJson() {
        //definition
        JsonObject config = new JsonObject();
        config.put("apiVersion", "admin.enmasse.io/v1alpha1");
        config.put("kind", "AddressSpacePlan");

        JsonObject definitionMetadata = new JsonObject(); // <metadata>
        definitionMetadata.put("name", this.getName());

        JsonObject annotations = new JsonObject();  // <annotations>
        annotations.put("enmasse.io/defined-by", this.getInfraConfigName()); //"brokered-space", "standard-space", newly created...
        definitionMetadata.put("annotations", annotations); // </annotations>

        config.put("metadata", definitionMetadata); // </metadata>

        config.put("displayName", this.getName()); //not parameterized for now
        config.put("displayOrder", 0);
        config.put("shortDescription", "Newly defined address space plan.");
        config.put("longDescription", "Newly defined address space plan.");
        config.put("uuid", "677485a2-0d96-11e8-ba89-0ed5f89f718b");
        config.put("addressSpaceType", this.getType().toString().toLowerCase()); // "standard", "brokered", newly created...

        JsonArray defResources = new JsonArray(); // <resources>
        JsonObject brokerResource;
        for (AddressSpaceResource res : this.getResources()) {
            brokerResource = new JsonObject();
            brokerResource.put("name", res.getName());
            brokerResource.put("min", res.getMin());
            brokerResource.put("max", res.getMax());
            defResources.add(brokerResource);
        }
        config.put("resources", defResources); // </resources>

        JsonArray defAddressPlan = new JsonArray(); // <addressPlans>
        for (AddressPlan plan : this.getAddressPlans()) {
            defAddressPlan.add(plan.getName());
        }
        config.put("addressPlans", defAddressPlan); // </addressPlans>

        return config;
    }

    public static AddressSpacePlan fromJson(JsonObject planDefinition, AdminApiClient adminApiClient) throws Exception {
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
            addressPlans.add(adminApiClient.getAddressPlan(addressPlansDef.getString(i)));
        }
        return new AddressSpacePlan(name, resourceDefName, type, resources, addressPlans);
    }

}
