/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.systemtest.apiclients.AdminApiClient;
import io.enmasse.systemtest.resources.InfraConfigDefinition;
import org.slf4j.Logger;

import java.util.ArrayList;

public class PlansProvider {

    private static Logger log = CustomLogger.getLogger();
    private ArrayList<AddressPlan> addressPlans;
    private ArrayList<AddressSpacePlan> addressSpacePlans;
    private ArrayList<InfraConfigDefinition> infraConfigs;
    private final AdminApiClient adminApiClient;

    public PlansProvider(Kubernetes kubernetes) {
        this.adminApiClient = new AdminApiClient(kubernetes);
    }

    public void setUp() {
        addressPlans = new ArrayList<>();
        addressSpacePlans = new ArrayList<>();
        infraConfigs = new ArrayList<>();
    }

    public void tearDown() throws Exception {
        for (AddressSpacePlan addressSpacePlan : addressSpacePlans) {
            adminApiClient.deleteAddressSpacePlan(addressSpacePlan);
        }

        for (AddressPlan addressPlan : addressPlans) {
            adminApiClient.deleteAddressPlan(addressPlan);
        }

        for (InfraConfigDefinition infraConfigDefinition : infraConfigs) {
            adminApiClient.deleteInfraConfig(infraConfigDefinition);
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
        addressPlans.removeIf(addressPlanIter -> addressPlanIter.getMetadata().getName().equals(addressPlan.getMetadata().getName()));
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

    public void removeAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        adminApiClient.deleteAddressSpacePlan(addressSpacePlan);
        addressSpacePlans.removeIf(spacePlanIter -> spacePlanIter.getMetadata().getName().equals(addressSpacePlan.getMetadata().getName()));
    }

    public AddressSpacePlan getAddressSpacePlan(String config) throws Exception {
        return adminApiClient.getAddressSpacePlan(config);
    }

    //------------------------------------------------------------------------------------------------
    // Infra configs
    //------------------------------------------------------------------------------------------------

    public void createInfraConfig(InfraConfigDefinition infraConfigDefinition) throws Exception {
        createInfraConfig(infraConfigDefinition, false);
    }

    public void createInfraConfig(InfraConfigDefinition infraConfigDefinition, boolean replaceExisting) throws Exception {
        if (replaceExisting) {
            adminApiClient.replaceInfraConfig(infraConfigDefinition);
        } else {
            adminApiClient.createInfraConfig(infraConfigDefinition);
        }
        infraConfigs.add(infraConfigDefinition);
    }

    public void removeInfraConfig(InfraConfigDefinition infraConfigDefinition) throws Exception {
        adminApiClient.deleteInfraConfig(infraConfigDefinition);
        infraConfigs.removeIf(infraId -> infraId.getName().equals(infraConfigDefinition.getName()));
    }

    public InfraConfigDefinition getBrokeredInfraConfig(String config) throws Exception {
        return adminApiClient.getInfraConfig(AddressSpaceType.BROKERED, config);
    }

    public InfraConfigDefinition getStandardInfraConfig(String config) throws Exception {
        return adminApiClient.getInfraConfig(AddressSpaceType.STANDARD, config);
    }
}
