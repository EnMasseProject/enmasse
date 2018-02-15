/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.enmasse.systemtest.AddressSpaceType;

import java.util.ArrayList;
import java.util.List;

public class AddressSpacePlan {

    private String name;
    private String configName;
    private String resourceDefName;
    private AddressSpaceType type;
    private List<AddressSpaceResource> resources;
    private List<AddressPlan> addressPlans = new ArrayList<>();

    public AddressSpacePlan(String name, String configName, String resourceDefName, AddressSpaceType type, List<AddressSpaceResource> resources, List<AddressPlan> addressPlans) {
        this.name = name;
        this.configName = configName;
        this.resourceDefName = resourceDefName;
        this.type = type;
        this.resources = resources;
        this.addressPlans = addressPlans;
    }

    public AddressSpacePlan(String name, String configName, String resourceDefName, AddressSpaceType type, List<AddressSpaceResource> resources) {
        this.name = name;
        this.configName = configName;
        this.resourceDefName = resourceDefName;
        this.type = type;
        this.resources = resources;
    }


    public String getName() {
        return name;
    }

    public String getConfigName() {
        return configName;
    }

    public String getResourceDefName() {
        return resourceDefName;
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


}
