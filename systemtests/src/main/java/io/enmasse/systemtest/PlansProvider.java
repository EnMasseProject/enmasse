/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import org.slf4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;

public class PlansProvider {

    private Kubernetes kubernetes;

    protected ArrayList<AddressPlan> addressPlans;
    protected ArrayList<AddressSpacePlan> addressSpacePlans;
    protected HashMap<AddressPlan, AddressSpacePlan> addressXSpaceBinding;

    private static Logger log = CustomLogger.getLogger();

    public PlansProvider(Kubernetes kubernetes) {
        this.kubernetes = kubernetes;
    }

    public void setUp() {
        addressPlans = new ArrayList();
        addressSpacePlans = new ArrayList();
        addressXSpaceBinding = new HashMap<>();
    }

    public void tearDown() {
        addressXSpaceBinding.forEach((addressPlan, spacePlan) -> TestUtils.removeAddressPlan(kubernetes, addressPlan, spacePlan));
        addressSpacePlans.forEach(spacePlan -> TestUtils.removeAddressSpacePlanConfig(kubernetes, spacePlan));
        addressPlans.forEach(addressPlan -> TestUtils.removeAddressPlanConfig(kubernetes, addressPlan));

        addressPlans.clear();
        addressXSpaceBinding.clear();
        addressSpacePlans.clear();
    }

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
}
