/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.apiclients.AdminApiClient;
import io.enmasse.systemtest.resources.AddressPlanDefinition;
import io.enmasse.systemtest.resources.AddressSpacePlanDefinition;
import org.slf4j.Logger;

import java.util.ArrayList;

public class PlansProvider {

    private static Logger log = CustomLogger.getLogger();
    private ArrayList<AddressPlanDefinition> addressPlans;
    private ArrayList<AddressSpacePlanDefinition> addressSpacePlans;
    private final AdminApiClient adminApiClient;

    public PlansProvider(Kubernetes kubernetes) {
        this.adminApiClient = new AdminApiClient(kubernetes);
    }

    public void setUp() {
        addressPlans = new ArrayList<>();
        addressSpacePlans = new ArrayList<>();
    }

    public void tearDown() throws Exception {
        for (AddressSpacePlanDefinition addressSpacePlan : addressSpacePlans) {
            adminApiClient.deleteAddressSpacePlan(addressSpacePlan);
        }

        for (AddressPlanDefinition addressPlan : addressPlans) {
            adminApiClient.deleteAddressPlan(addressPlan);
        }

        addressPlans.clear();
        addressSpacePlans.clear();
    }

    //------------------------------------------------------------------------------------------------
    // Address plans
    //------------------------------------------------------------------------------------------------

    public void createAddressPlan(AddressPlanDefinition addressPlan) throws Exception {
        createAddressPlan(addressPlan, false);
    }

    public void createAddressPlan(AddressPlanDefinition addressPlan, boolean replaceExisting) throws Exception {
        if (replaceExisting) {
            adminApiClient.replaceAddressPlan(addressPlan);
        } else {
            adminApiClient.createAddressPlan(addressPlan);
        }
        addressPlans.add(addressPlan);
    }

    public void removeAddressPlan(AddressPlanDefinition addressPlan) throws Exception {
        adminApiClient.deleteAddressPlan(addressPlan);
        addressPlans.removeIf(addressPlanIter -> addressPlanIter.getName().equals(addressPlan.getName()));
    }

    public void replaceAddressPlan(AddressPlanDefinition plan) throws Exception {
        adminApiClient.replaceAddressPlan(plan);
    }

    //------------------------------------------------------------------------------------------------
    // Address space plans
    //------------------------------------------------------------------------------------------------

    public void createAddressSpacePlan(AddressSpacePlanDefinition addressSpacePlan) throws Exception {
        createAddressSpacePlan(addressSpacePlan, false);
    }

    public void createAddressSpacePlan(AddressSpacePlanDefinition addressSpacePlan, boolean replaceExisting) throws Exception {
        if (replaceExisting) {
            adminApiClient.replaceAddressSpacePlan(addressSpacePlan);
        } else {
            adminApiClient.createAddressSpacePlan(addressSpacePlan);
        }
        addressSpacePlans.add(addressSpacePlan);
    }

    public void removeAddressSpacePlan(AddressSpacePlanDefinition addressSpacePlan) {
        removeAddressSpacePlan(addressSpacePlan);
        addressSpacePlans.removeIf(spacePlanIter -> spacePlanIter.getName().equals(addressSpacePlan.getName()));
    }

    public AddressSpacePlanDefinition getAddressSpacePlan(String config) throws Exception {
        return adminApiClient.getAddressSpacePlan(config);
    }
}
