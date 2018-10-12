/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.apiclients.AdminApiClient;
import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import org.slf4j.Logger;

import java.util.ArrayList;

public class PlansProvider {

    private static Logger log = CustomLogger.getLogger();
    private ArrayList<AddressPlan> addressPlans;
    private ArrayList<AddressSpacePlan> addressSpacePlans;
    private final AdminApiClient adminApiClient;

    public PlansProvider(Kubernetes kubernetes) {
        this.adminApiClient = new AdminApiClient(kubernetes);
    }

    public void setUp() {
        addressPlans = new ArrayList<>();
        addressSpacePlans = new ArrayList<>();
    }

    public void tearDown() throws Exception {
        for (AddressPlan addressPlan : addressPlans) {
            adminApiClient.deleteAddressPlan(addressPlan);
        }

        for (AddressSpacePlan addressSpacePlan : addressSpacePlans) {
            adminApiClient.deleteAddressSpacePlan(addressSpacePlan);
        }

        addressPlans.clear();
        addressSpacePlans.clear();
    }

    //------------------------------------------------------------------------------------------------
    // Address plans
    //------------------------------------------------------------------------------------------------

    public void createAddressPlan(AddressPlan addressPlan) throws Exception {
        createAddressPlan(addressPlan, false);
    }

    public void createAddressPlan(AddressPlan addressPlan, boolean replaceExisting) throws Exception {
        if (replaceExisting) {
            adminApiClient.replaceAddressPlan(addressPlan);
        } else {
            adminApiClient.createAddressPlan(addressPlan);
        }
        addressPlans.add(addressPlan);
    }

    public void removeAddressPlan(AddressPlan addressPlan) throws Exception {
        adminApiClient.deleteAddressPlan(addressPlan);
        addressPlans.removeIf(addressPlanIter -> addressPlanIter.getName().equals(addressPlan.getName()));
    }

    public void replaceAddressPlan(AddressPlan plan) throws Exception {
        adminApiClient.replaceAddressPlan(plan);
    }

    //------------------------------------------------------------------------------------------------
    // Address space plans
    //------------------------------------------------------------------------------------------------

    public void createAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        createAddressSpacePlan(addressSpacePlan, false);
    }

    public void createAddressSpacePlan(AddressSpacePlan addressSpacePlan, boolean replaceExisting) throws Exception {
        if (replaceExisting) {
            adminApiClient.replaceAddressSpacePlan(addressSpacePlan);
        } else {
            adminApiClient.createAddressSpacePlan(addressSpacePlan);
        }
        addressSpacePlans.add(addressSpacePlan);
    }

    public void removeAddressSpacePlan(AddressSpacePlan addressSpacePlan) {
        removeAddressSpacePlan(addressSpacePlan);
        addressSpacePlans.removeIf(spacePlanIter -> spacePlanIter.getName().equals(addressSpacePlan.getName()));
    }

    public AddressSpacePlan getAddressSpacePlan(String config) throws Exception {
        return adminApiClient.getAddressSpacePlan(config);
    }
}
