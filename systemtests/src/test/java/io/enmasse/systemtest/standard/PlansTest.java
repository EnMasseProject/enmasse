/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.StandardTestBase;
import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressResource;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import io.enmasse.systemtest.resources.AddressSpaceResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PlansTest extends StandardTestBase {

    protected ArrayList<AddressPlan> addressPlans;
    protected ArrayList<AddressSpacePlan> addressSpacePlans;
    protected HashMap<AddressPlan, AddressSpacePlan> addressXSpaceBinding;

    private static Logger log = CustomLogger.getLogger();

    @Before
    public void setUp() {
        addressPlans = new ArrayList();
        addressSpacePlans = new ArrayList();
        addressXSpaceBinding = new HashMap<>();
    }

    @After
    public void tearDown() {
        addressXSpaceBinding.forEach((addressPlan, spacePlan) -> super.removeAddressPlan(addressPlan, spacePlan));
        addressSpacePlans.forEach(spacePlan -> super.removeAddressSpacePlanConfig(spacePlan));
        addressPlans.forEach(addressPlan -> super.removeAddressPlanConfig(addressPlan));

        addressPlans.clear();
        addressXSpaceBinding.clear();
        addressSpacePlans.clear();
    }

    protected void createAddressPlanConfig(AddressPlan addressPlan) {
        createAddressPlanConfig(addressPlan, false);
    }

    protected void createAddressPlanConfig(AddressPlan addressPlan, boolean replaceExisting) {
        super.createAddressPlanConfig(addressPlan, replaceExisting);
        addressPlans.add(addressPlan);
    }

    protected boolean removeAddressPlanConfig(AddressPlan addressPlan) throws NotImplementedException {
        boolean removed = super.removeAddressPlanConfig(addressPlan);
        if (removed) {
            addressPlans.removeIf(addressPlanIter -> addressPlanIter.getName().equals(addressPlan.getName()));
        }
        return removed;
    }

    protected void appendAddressPlan(AddressPlan addressPlan, AddressSpacePlan addressSpacePlan) {
        super.appendAddressPlan(addressPlan, addressSpacePlan);
        addressXSpaceBinding.put(addressPlan, addressSpacePlan);
    }

    protected boolean removeAddressPlan(AddressPlan addressPlan, AddressSpacePlan addressSpacePlan) {
        boolean removed = super.removeAddressPlan(addressPlan, addressSpacePlan);
        if (removed) {
            addressXSpaceBinding.remove(addressPlan, addressSpacePlan);
        }
        return removed;
    }


    protected void createAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan) {
        createAddressSpacePlanConfig(addressSpacePlan, false);
    }

    protected void createAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan, boolean replaceExisting) {
        super.createAddressSpacePlanConfig(addressSpacePlan, replaceExisting);
        addressSpacePlans.add(addressSpacePlan);
    }

    protected boolean removeAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan) {
        boolean removed = super.removeAddressSpacePlanConfig(addressSpacePlan);
        if (removed) {
            addressSpacePlans.removeIf(spacePlanIter -> spacePlanIter.getName().equals(addressSpacePlan.getName()));
        }
        return removed;
    }


    //@Test disabled because dynamically created address-space-plans are not accepted yet
    public void testCreateAddressSpacePlan() throws Exception {
        //define and create address plans
        List<AddressResource> addressResourcesQueue = Arrays.asList(new AddressResource("broker", 1.0));
        List<AddressResource> addressResourcesTopic = Arrays.asList(
                new AddressResource("broker", 1.0),
                new AddressResource("router", 1.0));
        AddressPlan weakQueuePlan = new AddressPlan("standard-queue-weak", AddressType.QUEUE, addressResourcesQueue);
        AddressPlan weakTopicPlan = new AddressPlan("standard-topic-weak", AddressType.TOPIC, addressResourcesTopic);

        createAddressPlanConfig(weakQueuePlan);
        createAddressPlanConfig(weakTopicPlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 0.0, 9.0),
                new AddressSpaceResource("router", 1.0, 5.0));
        List<AddressPlan> addressPlans = Arrays.asList(weakQueuePlan, weakTopicPlan);
        AddressSpacePlan weakSpacePlan = new AddressSpacePlan("weak-plan", "weak",
                "standard-space", AddressSpaceType.STANDARD, resources, addressPlans);
        createAddressSpacePlanConfig(weakSpacePlan);

        //create address space plan with new plan
        AddressSpace weakAddressSpace = new AddressSpace("weak-address-space", AddressSpaceType.STANDARD,
                weakSpacePlan.getName());
        createAddressSpace(weakAddressSpace, AuthService.STANDARD.toString());

        //deploy destinations
        Destination weakQueueDest = Destination.queue("weak-queue", weakQueuePlan.getName());
        Destination weakTopicDest = Destination.queue("weak-topic", weakTopicPlan.getName());
        setAddresses(weakAddressSpace, weakQueueDest, weakTopicDest);

        //get destinations
        Future<List<Address>> getWeakQueue = getAddressesObjects(Optional.of(weakQueuePlan.getName()));
        Future<List<Address>> getWeakTopic = getAddressesObjects(Optional.of(weakTopicPlan.getName()));

        String assertMessage = "Queue plan wasn't set properly";
        assertEquals(assertMessage, getWeakQueue.get(20, TimeUnit.SECONDS).get(0).getPlan(),
                weakQueuePlan.getName());
        assertEquals(assertMessage, getWeakTopic.get(20, TimeUnit.SECONDS).get(0).getPlan(),
                weakTopicPlan.getName());
    }

    @Test
    public void testAppendAddressPlan() throws Exception {
        List<AddressResource> addressResources = Arrays.asList(new AddressResource("broker", 0.1));
        String weakQueuePlanName = "pooled-standard-queue-weak";
        AddressPlan weakQueuePlan = new AddressPlan(weakQueuePlanName, AddressType.QUEUE, addressResources);
        createAddressPlanConfig(weakQueuePlan);

        AddressSpacePlan standardPlan = getAddressSpacePlanConfig("standard");
        appendAddressPlan(weakQueuePlan, standardPlan);

        ArrayList<Destination> dest = new ArrayList<>();
        int destCount = 20;
        for (int i = 0; i < destCount; i++) {
            dest.add(Destination.queue("weak-queue-" + i, weakQueuePlan.getName()));
        }
        setAddresses(dest.toArray(new Destination[0]));

        // TODO once getAddressPlanConfig() method will be implemented
//        double requiredCredit = getRequiredCreditFromResource("broker", getAddressPlanConfig(weakQueuePlanName));
//        int replicasCount = (int) (destCount * requiredCredit);
//        waitForBrokerReplicas(sharedAddressSpace, dest.get(0), replicasCount);
        waitForBrokerReplicas(sharedAddressSpace, dest.get(0), 2);

        Future<List<Address>> standardAddresses = getAddressesObjects(Optional.empty()); //get all addresses
        for (int i = 0; i < destCount; i++) {
            assertThat("Queue plan wasn't set properly",
                    standardAddresses.get(20, TimeUnit.SECONDS).get(i).getPlan(), is(weakQueuePlan.getName()));
        }
    }


}
