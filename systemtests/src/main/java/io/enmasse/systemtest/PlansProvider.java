/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import io.enmasse.systemtest.resources.ResourceDefinition;
import org.slf4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;

public class PlansProvider {

    private static Logger log = CustomLogger.getLogger();
    private Kubernetes kubernetes;
    private ArrayList<AddressPlan> addressPlans;
    private ArrayList<AddressSpacePlan> addressSpacePlans;
    private ArrayList<ResourceDefinition> resourceDefinitionConfigs;
    private ArrayList<ResourceDefinition> resourceDefinitionForRestore;
    private HashMap<AddressPlan, AddressSpacePlan> addressXSpaceBinding;

    public PlansProvider(Kubernetes kubernetes) {
        this.kubernetes = kubernetes;
    }

    public void setUp() {
        addressPlans = new ArrayList<>();
        addressSpacePlans = new ArrayList<>();
        resourceDefinitionConfigs = new ArrayList<>();
        addressXSpaceBinding = new HashMap<>();
        resourceDefinitionForRestore = new ArrayList<>();
    }

    public void tearDown() {
        addressXSpaceBinding.forEach((addressPlan, spacePlan) -> TestUtils.removeAddressPlan(kubernetes, addressPlan, spacePlan));
        addressSpacePlans.forEach(spacePlan -> TestUtils.removeAddressSpacePlanConfig(kubernetes, spacePlan));
        addressPlans.forEach(addressPlan -> TestUtils.removeAddressPlanConfig(kubernetes, addressPlan));
        resourceDefinitionConfigs.forEach(resourceDefinition -> TestUtils.removeResourceDefinitionConfig(kubernetes, resourceDefinition));
        restoreResourceDefinitionConfigs();

        addressPlans.clear();
        addressXSpaceBinding.clear();
        addressSpacePlans.clear();
        resourceDefinitionConfigs.clear();
        resourceDefinitionForRestore.clear();
    }

    //------------------------------------------------------------------------------------------------
    // Address plans
    //------------------------------------------------------------------------------------------------

    public void createAddressPlanConfig(AddressPlan addressPlan) {
        createAddressPlanConfig(addressPlan, false);
    }

    public void createAddressPlanConfig(AddressPlan addressPlan, boolean replaceExisting) {
        TestUtils.createAddressPlanConfig(kubernetes, addressPlan, replaceExisting);
        addressPlans.add(addressPlan);
    }

    public boolean removeAddressPlanConfig(AddressPlan addressPlan) throws NotImplementedException {
        boolean removed = TestUtils.removeAddressPlanConfig(kubernetes, addressPlan);
        if (removed) {
            addressPlans.removeIf(addressPlanIter -> addressPlanIter.getName().equals(addressPlan.getName()));
        }
        return removed;
    }

    public void appendAddressPlan(AddressPlan addressPlan, AddressSpacePlan addressSpacePlan) {
        TestUtils.appendAddressPlan(kubernetes, addressPlan, addressSpacePlan);
        addressXSpaceBinding.put(addressPlan, addressSpacePlan);
    }

    public boolean removeAddressPlan(AddressPlan addressPlan, AddressSpacePlan addressSpacePlan) {
        boolean removed = TestUtils.removeAddressPlan(kubernetes, addressPlan, addressSpacePlan);
        if (removed) {
            addressXSpaceBinding.remove(addressPlan, addressSpacePlan);
        }
        return removed;
    }

    //------------------------------------------------------------------------------------------------
    // Address space plans
    //------------------------------------------------------------------------------------------------

    public void createAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan) {
        createAddressSpacePlanConfig(addressSpacePlan, false);
    }

    public void createAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan, boolean replaceExisting) {
        TestUtils.createAddressSpacePlanConfig(kubernetes, addressSpacePlan, replaceExisting);
        addressSpacePlans.add(addressSpacePlan);
    }

    public boolean removeAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan) {
        boolean removed = TestUtils.removeAddressSpacePlanConfig(kubernetes, addressSpacePlan);
        if (removed) {
            addressSpacePlans.removeIf(spacePlanIter -> spacePlanIter.getName().equals(addressSpacePlan.getName()));
        }
        return removed;
    }

    //------------------------------------------------------------------------------------------------
    // Resource definitions
    //------------------------------------------------------------------------------------------------

    public void createResourceDefinitionConfig(ResourceDefinition resourceDefinition) {
        createResourceDefinitionConfig(resourceDefinition, false);
    }

    private void createResourceDefinitionConfig(ResourceDefinition resourceDefinition, boolean replaceExisting) {
        TestUtils.createResourceDefinitionConfig(kubernetes, resourceDefinition, replaceExisting);
        resourceDefinitionConfigs.add(resourceDefinition);
    }

    /**
     * Replace custom configMap created by user
     */
    public void replaceCustomResourceDefinitionConfig(ResourceDefinition resourceDefinition) {
        createResourceDefinitionConfig(resourceDefinition, true);
    }

    /**
     * Replace originam config map which will be restored after test
     */
    public void replaceResourceDefinitionConfig(ResourceDefinition resourceDefinition) {
        resourceDefinitionForRestore.add(TestUtils.getResourceDefinitionConfig(kubernetes, resourceDefinition.getName()));
        TestUtils.createResourceDefinitionConfig(kubernetes, resourceDefinition, true);
    }

    public boolean removeResourceDefinitionConfig(ResourceDefinition resourceDefinition) {
        boolean removed = TestUtils.removeResourceDefinitionConfig(kubernetes, resourceDefinition);
        if (removed) {
            resourceDefinitionConfigs.removeIf(resIter -> resIter.getName().equals(resourceDefinition.getName()));
        }
        return removed;
    }

    private void restoreResourceDefinitionConfigs() {
        resourceDefinitionForRestore.forEach(
                rs -> TestUtils.createResourceDefinitionConfig(kubernetes, rs, true));
    }
}
